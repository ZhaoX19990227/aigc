package com.aigc.contentfactory.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hotspot_record")
public class HotspotRecord extends BaseEntity {

    @TableId
    private Long id;
    private String source;
    private String sourceTopicId;
    private String title;
    private String summary;
    private BigDecimal score;
    private LocalDateTime capturedAt;
    private String rawPayload;
}
