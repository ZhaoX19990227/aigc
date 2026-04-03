package com.aigc.contentfactory.service.model;

import com.aigc.contentfactory.enums.AssetType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeneratedAsset {

    private AssetType assetType;
    private String fileName;
    private String fileUrl;
    private String mimeType;
    private Integer durationSec;
    private Long fileSize;
    private String generationParams;
}
