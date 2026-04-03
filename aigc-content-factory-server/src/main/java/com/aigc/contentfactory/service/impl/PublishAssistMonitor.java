package com.aigc.contentfactory.service.impl;

import com.aigc.contentfactory.config.AppProperties;
import com.aigc.contentfactory.entity.ContentTask;
import com.aigc.contentfactory.entity.PublishRecord;
import com.aigc.contentfactory.enums.PublishStatus;
import com.aigc.contentfactory.enums.TaskStatus;
import com.aigc.contentfactory.mapper.ContentTaskMapper;
import com.aigc.contentfactory.mapper.PublishRecordMapper;
import com.aigc.contentfactory.service.model.PublishAssistState;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PublishAssistMonitor {

    private final ObjectMapper objectMapper;
    private final PublishRecordMapper publishRecordMapper;
    private final ContentTaskMapper contentTaskMapper;
    private final Set<String> processedSuccessKeys = new HashSet<>();
    private final List<Path> statusDirs;

    public PublishAssistMonitor(ObjectMapper objectMapper,
                                PublishRecordMapper publishRecordMapper,
                                ContentTaskMapper contentTaskMapper,
                                AppProperties properties) {
        this.objectMapper = objectMapper;
        this.publishRecordMapper = publishRecordMapper;
        this.contentTaskMapper = contentTaskMapper;
        this.statusDirs = List.of(
                Path.of(properties.getPublish().getBilibili().getHelperLogDir()).toAbsolutePath().normalize(),
                Path.of(properties.getPublish().getDouyin().getHelperLogDir()).toAbsolutePath().normalize(),
                Path.of(properties.getPublish().getXiaohongshu().getHelperLogDir()).toAbsolutePath().normalize()
        );
    }

    @Scheduled(fixedDelay = 5000)
    public void syncAssistStates() {
        for (Path dir : statusDirs) {
            if (!Files.exists(dir)) {
                continue;
            }
            try (var stream = Files.list(dir)) {
                stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .forEach(this::syncStateFile);
            } catch (IOException ignored) {
            }
        }
    }

    private void syncStateFile(Path stateFile) {
        try {
            PublishAssistState state = objectMapper.readValue(stateFile.toFile(), PublishAssistState.class);
            if (state.getTaskId() == null || state.getPlatform() == null || state.getStatus() == null) {
                return;
            }
            String stateKey = state.getTaskId() + "-" + state.getPlatform() + "-" + state.getStatus();
            if ("SUCCESS".equals(state.getStatus()) && processedSuccessKeys.contains(stateKey)) {
                return;
            }

            PublishRecord record = publishRecordMapper.selectOne(new LambdaQueryWrapper<PublishRecord>()
                    .eq(PublishRecord::getTaskId, state.getTaskId())
                    .eq(PublishRecord::getPlatform, state.getPlatform())
                    .orderByDesc(PublishRecord::getCreatedAt)
                    .last("limit 1"));
            if (record == null) {
                return;
            }

            record.setStatus(state.getStatus());
            record.setResponseMessage(state.getMessage());
            record.setPublishedAt(LocalDateTime.now());
            if (state.getPlatformContentId() != null && !state.getPlatformContentId().isBlank()) {
                record.setPlatformContentId(state.getPlatformContentId());
            }
            publishRecordMapper.updateById(record);

            ContentTask task = contentTaskMapper.selectById(state.getTaskId());
            if (task != null) {
                List<PublishRecord> records = publishRecordMapper.selectList(new LambdaQueryWrapper<PublishRecord>()
                        .eq(PublishRecord::getTaskId, task.getId()));
                boolean allSuccess = !records.isEmpty() && records.stream().allMatch(item -> "SUCCESS".equals(item.getStatus()));
                boolean hasFailure = records.stream().anyMatch(item -> "FAILED".equals(item.getStatus()));

                if (allSuccess) {
                    task.setPublishStatus(PublishStatus.SUCCESS.name());
                    task.setStatus(TaskStatus.PUBLISHED.name());
                    task.setCurrentStep("PUBLISHED");
                    task.setErrorMessage("平台发布已确认完成");
                    contentTaskMapper.updateById(task);
                } else if (hasFailure) {
                    task.setPublishStatus(PublishStatus.FAILED.name());
                    task.setStatus(TaskStatus.REVIEW_APPROVED.name());
                    task.setCurrentStep("REVIEW_APPROVED");
                    task.setErrorMessage("存在平台发布失败，请查看发布记录");
                    contentTaskMapper.updateById(task);
                }
            }

            if ("SUCCESS".equals(state.getStatus())) {
                processedSuccessKeys.add(stateKey);
            }
        } catch (Exception ignored) {
        }
    }
}
