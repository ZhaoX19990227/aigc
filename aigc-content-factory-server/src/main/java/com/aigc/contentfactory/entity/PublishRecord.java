package com.aigc.contentfactory.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("publish_record")
public class PublishRecord extends BaseEntity {

    @TableId
    private Long id;
    private Long taskId;
    private String platform;
    private String accountName;
    private String status;
    private String platformContentId;
    private String responseMessage;
    private LocalDateTime publishedAt;
}
