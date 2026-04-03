package com.aigc.contentfactory.service.publisher;

import com.aigc.contentfactory.config.AppProperties;
import com.aigc.contentfactory.mapper.MediaAssetMapper;
import org.springframework.stereotype.Component;

@Component
public class XiaohongshuManualPublisher extends PlaywrightManualPublisher {

    public XiaohongshuManualPublisher(MediaAssetMapper mediaAssetMapper, AppProperties properties) {
        super(mediaAssetMapper, properties.getPublish().getXiaohongshu());
    }

    @Override
    public String platform() {
        return "XIAOHONGSHU";
    }

    @Override
    protected String scriptPlatformName() {
        return "xiaohongshu";
    }

    @Override
    protected String loginHint() {
        return "如未登录，请在打开的 Chromium 窗口完成小红书登录；当前创作平台更常见的是短信或密码登录。";
    }
}
