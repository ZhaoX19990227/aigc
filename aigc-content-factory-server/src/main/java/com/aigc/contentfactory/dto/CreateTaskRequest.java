package com.aigc.contentfactory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class CreateTaskRequest {

    @NotBlank
    private String taskName;

    private String accountPositioning;

    private String preferredTopic;

    @NotEmpty
    private List<String> targetPlatforms;

    private List<Long> selectedHotspotIds;
}
