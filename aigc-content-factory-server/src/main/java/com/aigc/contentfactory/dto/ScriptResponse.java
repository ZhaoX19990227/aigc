package com.aigc.contentfactory.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScriptResponse {

    private String title;
    private String introHook;
    private List<String> segments;
    private String closingCta;
    private List<String> tags;
    private Integer estimatedDurationSec;
    private String voiceTone;
    private String imagePrompt;
}
