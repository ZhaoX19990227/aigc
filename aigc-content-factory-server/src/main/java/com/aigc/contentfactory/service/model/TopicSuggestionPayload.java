package com.aigc.contentfactory.service.model;

import java.util.List;
import lombok.Data;

@Data
public class TopicSuggestionPayload {

    private List<TopicSuggestion> selectedTopics;
}
