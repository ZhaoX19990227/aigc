package com.aigc.contentfactory.service.publisher;

import com.aigc.contentfactory.config.AppProperties;
import com.aigc.contentfactory.entity.ContentScript;
import com.aigc.contentfactory.entity.ContentTask;
import com.aigc.contentfactory.entity.MediaAsset;
import com.aigc.contentfactory.entity.PublishRecord;
import com.aigc.contentfactory.enums.AssetType;
import com.aigc.contentfactory.mapper.MediaAssetMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

public abstract class ManualAssistPublisher implements PlatformPublisher {

    private final MediaAssetMapper mediaAssetMapper;
    private final String uploadUrl;
    private final boolean autoOpen;

    protected ManualAssistPublisher(MediaAssetMapper mediaAssetMapper, String uploadUrl, boolean autoOpen) {
        this.mediaAssetMapper = mediaAssetMapper;
        this.uploadUrl = uploadUrl;
        this.autoOpen = autoOpen;
    }

    @Override
    public PublishRecord publish(ContentTask task, ContentScript script) {
        MediaAsset videoAsset = mediaAssetMapper.selectOne(new LambdaQueryWrapper<MediaAsset>()
                .eq(MediaAsset::getTaskId, task.getId())
                .eq(MediaAsset::getAssetType, AssetType.VIDEO.name())
                .last("limit 1"));
        if (autoOpen) {
            openBrowser(uploadUrl);
        }

        PublishRecord record = new PublishRecord();
        record.setTaskId(task.getId());
        record.setPlatform(platform());
        record.setAccountName("manual-scan-login");
        record.setStatus("MANUAL_REQUIRED");
        record.setPublishedAt(LocalDateTime.now());
        record.setResponseMessage("""
                已打开平台上传页，请扫码登录后手动完成发布。
                上传地址：%s
                视频文件：%s
                标题建议：%s
                描述建议：%s
                """.formatted(
                uploadUrl,
                videoAsset == null ? "未找到视频文件" : videoAsset.getFileUrl(),
                script.getTitle(),
                script.getIntroHook() + " " + script.getClosingCta()
        ));
        record.setPlatformContentId("MANUAL-" + task.getId());
        return record;
    }

    private void openBrowser(String targetUrl) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(targetUrl));
            }
        } catch (Exception ignored) {
        }
    }
}
