package com.aigc.contentfactory.service.publisher;

import com.aigc.contentfactory.config.AppProperties;
import com.aigc.contentfactory.mapper.MediaAssetMapper;
import org.springframework.stereotype.Component;

@Component
public class BilibiliManualPublisher extends PlaywrightManualPublisher {

    public BilibiliManualPublisher(MediaAssetMapper mediaAssetMapper, AppProperties properties) {
        super(mediaAssetMapper, properties.getPublish().getBilibili());
    }

    @Override
    public String platform() {
        return "BILIBILI";
    }

    @Override
    protected String scriptPlatformName() {
        return "bilibili";
    }

    @Override
    protected String loginHint() {
        return "如未登录，请在打开的 Chromium 窗口扫码登录 B站。";
    }
}
