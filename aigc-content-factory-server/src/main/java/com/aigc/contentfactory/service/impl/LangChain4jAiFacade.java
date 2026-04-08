package com.aigc.contentfactory.service.impl;

import com.aigc.contentfactory.config.AppProperties;
import com.aigc.contentfactory.entity.HotspotRecord;
import com.aigc.contentfactory.service.AiFacade;
import com.aigc.contentfactory.service.model.ScriptDraft;
import com.aigc.contentfactory.service.model.ScriptDraftPayload;
import com.aigc.contentfactory.service.model.TopicSuggestion;
import com.aigc.contentfactory.service.model.TopicSuggestionPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Primary
@Service
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
public class LangChain4jAiFacade implements AiFacade {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*(\\{.*})\\s*```", Pattern.DOTALL);

    private final OpenAiStructuredAssistant assistant;
    private final ObjectMapper objectMapper;

    public LangChain4jAiFacade(AppProperties properties, ObjectMapper objectMapper) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(properties.getAi().getApiKey())
                .baseUrl(properties.getAi().getBaseUrl())
                .modelName(properties.getAi().getModel())
                .build();
        this.assistant = AiServices.create(OpenAiStructuredAssistant.class, chatModel);
        this.objectMapper = objectMapper;
    }

    @Override
    public List<TopicSuggestion> suggestTopics(List<HotspotRecord> hotspots, String accountPositioning, List<String> platforms) {
        try {
            String response = assistant.suggestTopics(
                    normalize(accountPositioning),
                    String.join(", ", platforms),
                    hotspots.stream()
                            .map(item -> item.getTitle() + " | 热度=" + item.getScore() + " | 摘要=" + item.getSummary())
                            .collect(Collectors.joining("\n"))
            );
            List<TopicSuggestion> parsedTopics = parseTopicSuggestions(extractJson(response));
            if (parsedTopics.isEmpty()) {
                throw new IllegalStateException("模型未返回可用选题");
            }
            return parsedTopics;
        } catch (Exception exception) {
            log.error("AI suggestTopics failed: {}", exception.getMessage());
            throw new IllegalStateException("AI 选题失败: " + exception.getMessage(), exception);
        }
    }

    @Override
    public ScriptDraft generateScript(String topic, String accountPositioning, List<String> platforms) {
        try {
            String response = assistant.generateScript(
                    normalize(accountPositioning),
                    String.join(", ", platforms),
                    topic
            );
            ScriptDraftPayload payload = objectMapper.readValue(extractJson(response), ScriptDraftPayload.class);
            return ScriptDraft.builder()
                    .title(payload.getTitle())
                    .introHook(payload.getIntroHook())
                    .segments(payload.getSegments())
                    .closingCta(payload.getClosingCta())
                    .tags(payload.getTags())
                    .estimatedDurationSec(payload.getEstimatedDurationSec())
                    .voiceTone(payload.getVoiceTone())
                    .imagePrompt(payload.getImagePrompt())
                    .build();
        } catch (Exception exception) {
            log.error("AI generateScript failed: {}", exception.getMessage());
            throw new IllegalStateException("AI 脚本生成失败: " + exception.getMessage(), exception);
        }
    }

    private String normalize(String positioning) {
        return positioning == null || positioning.isBlank() ? "AI 自动化内容运营" : positioning;
    }

    private List<TopicSuggestion> parseTopicSuggestions(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode selectedTopicsNode = root.path("selectedTopics");
        if (!selectedTopicsNode.isArray()) {
            return List.of();
        }

        List<TopicSuggestion> topics = new java.util.ArrayList<>();
        for (JsonNode item : selectedTopicsNode) {
            try {
                String title = item.path("title").asText("").trim();
                if (title.isBlank()) {
                    continue;
                }
                String reason = item.path("reason").asText("模型未返回原因");
                String targetAudience = item.path("targetAudience").asText("泛内容消费用户");
                List<String> suggestedPlatforms = toStringList(item.path("suggestedPlatforms"));
                List<String> riskFlags = toStringList(item.path("riskFlags"));
                int priority = item.path("priority").isInt() ? item.path("priority").asInt() : topics.size() + 1;
                topics.add(TopicSuggestion.builder()
                        .title(title)
                        .reason(reason)
                        .targetAudience(targetAudience)
                        .suggestedPlatforms(suggestedPlatforms)
                        .riskFlags(riskFlags)
                        .priority(priority)
                        .build());
            } catch (Exception itemException) {
                log.warn("Skip invalid topic suggestion item: {}", itemException.getMessage());
            }
        }
        return topics;
    }

    private List<String> toStringList(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            List<String> values = new java.util.ArrayList<>();
            for (JsonNode item : node) {
                String text = item.asText("").trim();
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
            return values;
        }
        String text = node.asText("").trim();
        return text.isBlank() ? List.of() : List.of(text);
    }

    private String extractJson(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("模型返回为空");
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        Matcher matcher = CODE_BLOCK_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        int start = trimmed.indexOf('{');
        if (start < 0) {
            throw new IllegalArgumentException("未找到 JSON 起始位置");
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < trimmed.length(); index++) {
            char current = trimmed.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return trimmed.substring(start, index + 1).trim();
                }
            }
        }
        throw new IllegalArgumentException("未找到完整 JSON 对象");
    }
}
