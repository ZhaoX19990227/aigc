package com.aigc.contentfactory.service.impl;

import com.aigc.contentfactory.config.AppProperties;
import com.aigc.contentfactory.dto.CreateTaskRequest;
import com.aigc.contentfactory.dto.MediaAssetResponse;
import com.aigc.contentfactory.dto.PublishRecordResponse;
import com.aigc.contentfactory.dto.ScriptResponse;
import com.aigc.contentfactory.dto.TaskDetailResponse;
import com.aigc.contentfactory.dto.TaskListItemResponse;
import com.aigc.contentfactory.entity.ContentScript;
import com.aigc.contentfactory.entity.ContentTask;
import com.aigc.contentfactory.entity.HotspotRecord;
import com.aigc.contentfactory.entity.MediaAsset;
import com.aigc.contentfactory.entity.PublishRecord;
import com.aigc.contentfactory.entity.TopicCandidate;
import com.aigc.contentfactory.enums.PublishStatus;
import com.aigc.contentfactory.enums.ReviewStatus;
import com.aigc.contentfactory.enums.TaskStatus;
import com.aigc.contentfactory.mapper.ContentScriptMapper;
import com.aigc.contentfactory.mapper.ContentTaskMapper;
import com.aigc.contentfactory.mapper.MediaAssetMapper;
import com.aigc.contentfactory.mapper.PublishRecordMapper;
import com.aigc.contentfactory.mapper.TopicCandidateMapper;
import com.aigc.contentfactory.service.AiFacade;
import com.aigc.contentfactory.service.AssetPipelineService;
import com.aigc.contentfactory.service.HotspotService;
import com.aigc.contentfactory.service.TaskFacade;
import com.aigc.contentfactory.service.model.GeneratedAsset;
import com.aigc.contentfactory.service.model.GeneratedAssetBundle;
import com.aigc.contentfactory.service.model.ScriptDraft;
import com.aigc.contentfactory.service.model.TopicSuggestion;
import com.aigc.contentfactory.service.publisher.PlatformPublisher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskFacadeImpl implements TaskFacade {

    private final HotspotService hotspotService;
    private final AiFacade aiFacade;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final ContentTaskMapper contentTaskMapper;
    private final TopicCandidateMapper topicCandidateMapper;
    private final ContentScriptMapper contentScriptMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final PublishRecordMapper publishRecordMapper;
    private final Map<String, PlatformPublisher> platformPublishers;
    private final AssetPipelineService assetPipelineService;

    public TaskFacadeImpl(HotspotService hotspotService,
                          AiFacade aiFacade,
                          AppProperties appProperties,
                          ObjectMapper objectMapper,
                          ContentTaskMapper contentTaskMapper,
                          TopicCandidateMapper topicCandidateMapper,
                          ContentScriptMapper contentScriptMapper,
                          MediaAssetMapper mediaAssetMapper,
                          PublishRecordMapper publishRecordMapper,
                          AssetPipelineService assetPipelineService,
                          List<PlatformPublisher> platformPublishers) {
        this.hotspotService = hotspotService;
        this.aiFacade = aiFacade;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.contentTaskMapper = contentTaskMapper;
        this.topicCandidateMapper = topicCandidateMapper;
        this.contentScriptMapper = contentScriptMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.publishRecordMapper = publishRecordMapper;
        this.assetPipelineService = assetPipelineService;
        this.platformPublishers = platformPublishers.stream()
                .collect(Collectors.toMap(PlatformPublisher::platform, Function.identity()));
    }

    @Override
    @Transactional
    public TaskDetailResponse createTask(CreateTaskRequest request) {
        List<HotspotRecord> hotspots;
        if (request.getSelectedHotspotIds() != null && !request.getSelectedHotspotIds().isEmpty()) {
            hotspots = hotspotService.findByIds(request.getSelectedHotspotIds());
            if (hotspots.isEmpty()) {
                throw new IllegalArgumentException("所选热点不存在或已失效");
            }
        } else {
            hotspots = appProperties.getWorkflow().isAutoCollectHotspotsOnCreate()
                    ? hotspotService.collectHotspots()
                    : hotspotService.latestHotspots();
        }

        ContentTask task = new ContentTask();
        task.setBatchNo("BATCH-" + UUID.randomUUID().toString().substring(0, 8));
        task.setName(request.getTaskName());
        task.setStatus(TaskStatus.HOTSPOT_FETCHED.name());
        task.setCurrentStep("HOTSPOT_FETCHED");
        task.setSourceType(request.getSelectedHotspotIds() != null && !request.getSelectedHotspotIds().isEmpty() ? "SELECTED_HOTSPOT" : "HOTSPOT");
        task.setPlatformsJson(writeJson(request.getTargetPlatforms()));
        task.setReviewStatus(ReviewStatus.PENDING.name());
        task.setPublishStatus(PublishStatus.NOT_STARTED.name());
        task.setRetryCount(0);
        task.setManualReviewRequired(appProperties.getWorkflow().isManualReviewRequired());
        contentTaskMapper.insert(task);

        List<TopicSuggestion> suggestions = aiFacade.suggestTopics(hotspots, request.getAccountPositioning(), request.getTargetPlatforms());
        TopicSuggestion selected;
        if (request.getSelectedHotspotIds() != null && request.getSelectedHotspotIds().size() == 1) {
            HotspotRecord onlyHotspot = hotspots.getFirst();
            selected = TopicSuggestion.builder()
                    .title(onlyHotspot.getTitle())
                    .reason("用户显式选择了该热点，按所选热点直接创建任务")
                    .targetAudience("基于账号定位自动生成")
                    .suggestedPlatforms(request.getTargetPlatforms())
                    .riskFlags(List.of())
                    .priority(1)
                    .build();
            suggestions = List.of(selected);
        } else if (request.getPreferredTopic() != null && !request.getPreferredTopic().isBlank()) {
            selected = suggestions.stream()
                    .filter(item -> item.getTitle().contains(request.getPreferredTopic()))
                    .findFirst()
                    .orElse(suggestions.getFirst());
        } else {
            selected = suggestions.getFirst();
        }

        task.setSelectedTopic(selected.getTitle());
        task.setStatus(TaskStatus.TOPIC_SELECTED.name());
        task.setCurrentStep("TOPIC_SELECTED");
        contentTaskMapper.updateById(task);

        persistTopics(task.getId(), suggestions);

        ScriptDraft draft = aiFacade.generateScript(selected.getTitle(), request.getAccountPositioning(), request.getTargetPlatforms());
        ContentScript script = persistScript(task.getId(), draft);

        task.setStatus(TaskStatus.VIDEO_READY.name());
        task.setCurrentStep("VIDEO_READY");
        task.setPublishStatus(PublishStatus.READY.name());
        task.setReviewStatus(ReviewStatus.PENDING.name());
        contentTaskMapper.updateById(task);

        GeneratedAssetBundle assetBundle = assetPipelineService.generate(task.getId(), draft);
        persistAssets(task.getId(), assetBundle);

        task.setStatus(TaskStatus.REVIEW_PENDING.name());
        task.setCurrentStep("REVIEW_PENDING");
        contentTaskMapper.updateById(task);

        return buildTaskDetail(task, script);
    }

    @Override
    public List<TaskListItemResponse> listTasks() {
        return contentTaskMapper.selectList(new LambdaQueryWrapper<ContentTask>()
                        .orderByDesc(ContentTask::getCreatedAt))
                .stream()
                .map(task -> TaskListItemResponse.builder()
                        .id(String.valueOf(task.getId()))
                        .name(task.getName())
                        .status(task.getStatus())
                        .reviewStatus(task.getReviewStatus())
                        .publishStatus(task.getPublishStatus())
                        .selectedTopic(task.getSelectedTopic())
                        .targetPlatforms(readStringList(task.getPlatformsJson()))
                        .createdAt(task.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public TaskDetailResponse getTask(Long taskId) {
        return buildTaskDetail(fetchTask(taskId), null);
    }

    @Override
    @Transactional
    public TaskDetailResponse approveTask(Long taskId, String comment) {
        ContentTask task = fetchTask(taskId);
        task.setReviewStatus(ReviewStatus.APPROVED.name());
        task.setStatus(TaskStatus.REVIEW_APPROVED.name());
        task.setCurrentStep("REVIEW_APPROVED");
        task.setErrorMessage(comment);
        contentTaskMapper.updateById(task);
        return buildTaskDetail(task, null);
    }

    @Override
    @Transactional
    public TaskDetailResponse rejectTask(Long taskId, String comment) {
        ContentTask task = fetchTask(taskId);
        task.setReviewStatus(ReviewStatus.REJECTED.name());
        task.setStatus(TaskStatus.REVIEW_REJECTED.name());
        task.setCurrentStep("REVIEW_REJECTED");
        task.setErrorMessage(comment);
        contentTaskMapper.updateById(task);
        return buildTaskDetail(task, null);
    }

    @Override
    @Transactional
    public TaskDetailResponse publishTask(Long taskId, List<String> platforms) {
        ContentTask task = fetchTask(taskId);
        if (!ReviewStatus.APPROVED.name().equals(task.getReviewStatus())) {
            throw new IllegalStateException("任务未通过审核，不能发布");
        }
        ContentScript script = fetchScript(taskId);
        task.setStatus(TaskStatus.PUBLISHING.name());
        task.setCurrentStep("PUBLISHING");
        contentTaskMapper.updateById(task);

        List<String> finalPlatforms = platforms == null || platforms.isEmpty()
                ? readStringList(task.getPlatformsJson())
                : platforms;
        List<PublishRecord> records = new ArrayList<>();
        for (String platform : finalPlatforms) {
            PlatformPublisher publisher = platformPublishers.get(platform);
            if (publisher == null) {
                throw new IllegalArgumentException("不支持的平台: " + platform);
            }
            PublishRecord record = publisher.publish(task, script);
            publishRecordMapper.insert(record);
            records.add(record);
        }
        boolean allSuccess = records.stream().allMatch(item -> "SUCCESS".equals(item.getStatus()));
        boolean hasFailure = records.stream().anyMatch(item -> "FAILED".equals(item.getStatus()));
        if (allSuccess) {
            task.setPublishStatus(PublishStatus.SUCCESS.name());
            task.setStatus(TaskStatus.PUBLISHED.name());
            task.setCurrentStep("PUBLISHED");
            task.setErrorMessage("发布完成，共 " + records.size() + " 个平台");
        } else if (hasFailure) {
            task.setPublishStatus(PublishStatus.FAILED.name());
            task.setStatus(TaskStatus.REVIEW_APPROVED.name());
            task.setCurrentStep("REVIEW_APPROVED");
            task.setErrorMessage("部分平台发布失败，请查看发布记录");
        } else {
            task.setPublishStatus(PublishStatus.PARTIAL_SUCCESS.name());
            task.setStatus(TaskStatus.PUBLISHING.name());
            task.setCurrentStep("PUBLISHING");
            task.setErrorMessage("已生成手动发布操作，请查看发布记录并扫码登录完成");
        }
        contentTaskMapper.updateById(task);
        return buildTaskDetail(task, script);
    }

    private void persistTopics(Long taskId, List<TopicSuggestion> suggestions) {
        for (TopicSuggestion suggestion : suggestions) {
            TopicCandidate candidate = new TopicCandidate();
            candidate.setTaskId(taskId);
            candidate.setTitle(suggestion.getTitle());
            candidate.setReason(suggestion.getReason());
            candidate.setTargetAudience(suggestion.getTargetAudience());
            candidate.setSuggestedPlatforms(writeJson(suggestion.getSuggestedPlatforms()));
            candidate.setRiskFlags(writeJson(suggestion.getRiskFlags()));
            candidate.setPriority(suggestion.getPriority());
            topicCandidateMapper.insert(candidate);
        }
    }

    private ContentScript persistScript(Long taskId, ScriptDraft draft) {
        ContentScript script = new ContentScript();
        script.setTaskId(taskId);
        script.setTitle(draft.getTitle());
        script.setIntroHook(draft.getIntroHook());
        script.setSegmentsJson(writeJson(draft.getSegments()));
        script.setClosingCta(draft.getClosingCta());
        script.setTagsJson(writeJson(draft.getTags()));
        script.setEstimatedDurationSec(draft.getEstimatedDurationSec());
        script.setVoiceTone(draft.getVoiceTone());
        script.setImagePrompt(draft.getImagePrompt());
        contentScriptMapper.insert(script);
        return script;
    }

    private void persistAssets(Long taskId, GeneratedAssetBundle assetBundle) {
        for (GeneratedAsset asset : assetBundle.getAssets()) {
            createAsset(taskId, asset);
        }
    }

    private void createAsset(Long taskId, GeneratedAsset generatedAsset) {
        MediaAsset asset = new MediaAsset();
        asset.setTaskId(taskId);
        asset.setAssetType(generatedAsset.getAssetType().name());
        asset.setFileName(generatedAsset.getFileName());
        asset.setFileUrl(generatedAsset.getFileUrl());
        asset.setMimeType(generatedAsset.getMimeType());
        asset.setDurationSec(generatedAsset.getDurationSec());
        asset.setFileSize(generatedAsset.getFileSize());
        asset.setGenerationParams(generatedAsset.getGenerationParams());
        asset.setStatus("READY");
        mediaAssetMapper.insert(asset);
    }

    private TaskDetailResponse buildTaskDetail(ContentTask task, ContentScript existingScript) {
        ContentScript script = existingScript != null ? existingScript : fetchScript(task.getId());
        List<MediaAssetResponse> assets = mediaAssetMapper.selectList(new LambdaQueryWrapper<MediaAsset>()
                        .eq(MediaAsset::getTaskId, task.getId())
                        .orderByAsc(MediaAsset::getCreatedAt))
                .stream()
                .map(item -> MediaAssetResponse.builder()
                        .assetType(item.getAssetType())
                        .fileUrl(item.getFileUrl())
                        .fileName(item.getFileName())
                        .durationSec(item.getDurationSec())
                        .status(item.getStatus())
                        .build())
                .toList();
        List<PublishRecordResponse> publishRecords = publishRecordMapper.selectList(new LambdaQueryWrapper<PublishRecord>()
                        .eq(PublishRecord::getTaskId, task.getId())
                        .orderByDesc(PublishRecord::getPublishedAt))
                .stream()
                .map(item -> PublishRecordResponse.builder()
                        .platform(item.getPlatform())
                        .status(item.getStatus())
                        .responseMessage(item.getResponseMessage())
                        .platformContentId(item.getPlatformContentId())
                        .publishedAt(item.getPublishedAt())
                        .build())
                .toList();
        return TaskDetailResponse.builder()
                .id(String.valueOf(task.getId()))
                .batchNo(task.getBatchNo())
                .name(task.getName())
                .status(task.getStatus())
                .currentStep(task.getCurrentStep())
                .selectedTopic(task.getSelectedTopic())
                .reviewStatus(task.getReviewStatus())
                .publishStatus(task.getPublishStatus())
                .targetPlatforms(readStringList(task.getPlatformsJson()))
                .script(script == null ? null : ScriptResponse.builder()
                        .title(script.getTitle())
                        .introHook(script.getIntroHook())
                        .segments(readStringList(script.getSegmentsJson()))
                        .closingCta(script.getClosingCta())
                        .tags(readStringList(script.getTagsJson()))
                        .estimatedDurationSec(script.getEstimatedDurationSec())
                        .voiceTone(script.getVoiceTone())
                        .imagePrompt(script.getImagePrompt())
                        .build())
                .assets(assets)
                .publishRecords(publishRecords)
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private ContentTask fetchTask(Long taskId) {
        ContentTask task = contentTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return task;
    }

    private ContentScript fetchScript(Long taskId) {
        return contentScriptMapper.selectOne(new LambdaQueryWrapper<ContentScript>().eq(ContentScript::getTaskId, taskId));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return List.of(json);
        }
    }
}
