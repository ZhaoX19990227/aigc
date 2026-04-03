package com.aigc.contentfactory.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("topic_candidate")
public class TopicCandidate extends BaseEntity {

    @TableId
    private Long id;
    private Long taskId;
    private String title;
    private String reason;
    private String targetAudience;
    private String suggestedPlatforms;
    private String riskFlags;
    private Integer priority;
}
