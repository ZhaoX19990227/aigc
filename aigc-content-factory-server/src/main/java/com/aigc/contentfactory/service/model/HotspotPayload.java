package com.aigc.contentfactory.service.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HotspotPayload {

    private String source;
    private String sourceTopicId;
    private String title;
    private String summary;
    private BigDecimal score;
    private String rawPayload;
}
