package com.aigc.contentfactory.service.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeneratedAssetBundle {

    private List<GeneratedAsset> assets;
}
