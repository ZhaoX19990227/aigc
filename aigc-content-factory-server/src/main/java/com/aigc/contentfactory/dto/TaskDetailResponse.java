package com.aigc.contentfactory.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskDetailResponse {

    private String id;
    private String batchNo;
    private String name;
    private String status;
    private String currentStep;
    private String selectedTopic;
    private String reviewStatus;
    private String publishStatus;
    private List<String> targetPlatforms;
    private ScriptResponse script;
    private List<MediaAssetResponse> assets;
    private List<PublishRecordResponse> publishRecords;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
