package com.aigc.contentfactory.service.impl;

import com.aigc.contentfactory.config.AppProperties;
import com.aigc.contentfactory.entity.HotspotRecord;
import com.aigc.contentfactory.service.AiFacade;
import com.aigc.contentfactory.service.model.ScriptDraft;
import com.aigc.contentfactory.service.model.ScriptDraftPayload;
import com.aigc.contentfactory.service.model.TopicSuggestion;
import com.aigc.contentfactory.service.model.TopicSuggestionPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import java.util.List;
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

    private final MockAiFacade fallback = new MockAiFacade();
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
            TopicSuggestionPayload payload = objectMapper.readValue(response, TopicSuggestionPayload.class);
            if (payload.getSelectedTopics() == null || payload.getSelectedTopics().isEmpty()) {
                return fallback.suggestTopics(hotspots, accountPositioning, platforms);
            }
            return payload.getSelectedTopics();
        } catch (Exception exception) {
            log.warn("OpenAI suggestTopics failed, fallback to mock: {}", exception.getMessage());
            return fallback.suggestTopics(hotspots, accountPositioning, platforms);
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
            ScriptDraftPayload payload = objectMapper.readValue(response, ScriptDraftPayload.class);
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
            log.warn("OpenAI generateScript failed, fallback to mock: {}", exception.getMessage());
            return fallback.generateScript(topic, accountPositioning, platforms);
        }
    }

    private String normalize(String positioning) {
        return positioning == null || positioning.isBlank() ? "AI 自动化内容运营" : positioning;
    }
}
