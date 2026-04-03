package com.aigc.contentfactory.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_script")
public class ContentScript extends BaseEntity {

    @TableId
    private Long id;
    private Long taskId;
    private String title;
    private String introHook;
    private String segmentsJson;
    private String closingCta;
    private String tagsJson;
    private Integer estimatedDurationSec;
    private String voiceTone;
    private String imagePrompt;
}
