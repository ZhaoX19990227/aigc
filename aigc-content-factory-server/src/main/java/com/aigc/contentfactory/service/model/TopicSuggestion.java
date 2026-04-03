package com.aigc.contentfactory.service.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicSuggestion {

    private String title;
    private String reason;
    private String targetAudience;
    private List<String> suggestedPlatforms;
    private List<String> riskFlags;
    private Integer priority;
}
