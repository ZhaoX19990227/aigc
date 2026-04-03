package com.aigc.contentfactory.service.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PublishAssistState {

    private Long taskId;
    private String platform;
    private String status;
    private String message;
    private String platformContentId;
    private LocalDateTime updatedAt;
}
