package com.aigc.contentfactory.service.model;

import java.util.List;
import lombok.Data;

@Data
public class ScriptDraftPayload {

    private String title;
    private String introHook;
    private List<String> segments;
    private String closingCta;
    private List<String> tags;
    private Integer estimatedDurationSec;
    private String voiceTone;
    private String imagePrompt;
}
