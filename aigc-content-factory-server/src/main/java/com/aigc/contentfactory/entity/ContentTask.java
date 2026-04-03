package com.aigc.contentfactory.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_task")
public class ContentTask extends BaseEntity {

    @TableId
    private Long id;
    private String batchNo;
    private String name;
    private String status;
    private String currentStep;
    private String sourceType;
    private String selectedTopic;
    private String platformsJson;
    private String reviewStatus;
    private String publishStatus;
    private String errorMessage;
    private Integer retryCount;
    private Boolean manualReviewRequired;
}
