package com.aigc.contentfactory.service.publisher;

import com.aigc.contentfactory.config.AppProperties;
import com.aigc.contentfactory.mapper.MediaAssetMapper;
import org.springframework.stereotype.Component;

@Component
public class DouyinManualPublisher extends PlaywrightManualPublisher {

    public DouyinManualPublisher(MediaAssetMapper mediaAssetMapper, AppProperties properties) {
        super(mediaAssetMapper, properties.getPublish().getDouyin());
    }

    @Override
    public String platform() {
        return "DOUYIN";
    }

    @Override
    protected String scriptPlatformName() {
        return "douyin";
    }

    @Override
    protected String loginHint() {
        return "如未登录，请在打开的 Chromium 窗口使用抖音 App 扫码登录。";
    }
}
