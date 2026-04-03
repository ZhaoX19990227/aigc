package com.aigc.contentfactory.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MediaAssetResponse {

    private String assetType;
    private String fileUrl;
    private String fileName;
    private Integer durationSec;
    private String status;
}
