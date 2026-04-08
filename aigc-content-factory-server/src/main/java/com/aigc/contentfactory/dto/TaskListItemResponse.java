package com.aigc.contentfactory.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskListItemResponse {

    private String id;
    private String name;
    private String status;
    private String reviewStatus;
    private String publishStatus;
    private String selectedTopic;
    private List<String> targetPlatforms;
    private LocalDateTime createdAt;
}
