package com.aigc.contentfactory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HotspotResponse {

    private Long id;
    private String source;
    private String title;
    private String summary;
    private BigDecimal score;
    private LocalDateTime capturedAt;
}
