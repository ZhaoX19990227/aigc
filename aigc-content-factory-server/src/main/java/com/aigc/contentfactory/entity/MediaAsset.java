package com.aigc.contentfactory.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("media_asset")
public class MediaAsset extends BaseEntity {

    @TableId
    private Long id;
    private Long taskId;
    private String assetType;
    private String fileUrl;
    private String fileName;
    private String mimeType;
    private Integer durationSec;
    private Long fileSize;
    private String generationParams;
    private String status;
}
