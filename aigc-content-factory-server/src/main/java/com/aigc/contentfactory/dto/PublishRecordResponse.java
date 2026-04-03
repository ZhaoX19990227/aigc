package com.aigc.contentfactory.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublishRecordResponse {

    private String platform;
    private String status;
    private String responseMessage;
    private String platformContentId;
    private LocalDateTime publishedAt;
}
