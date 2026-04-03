package com.aigc.contentfactory.service.publisher;

import com.aigc.contentfactory.config.AppProperties;
import com.aigc.contentfactory.entity.ContentScript;
import com.aigc.contentfactory.entity.ContentTask;
import com.aigc.contentfactory.entity.MediaAsset;
import com.aigc.contentfactory.entity.PublishRecord;
import com.aigc.contentfactory.enums.AssetType;
import com.aigc.contentfactory.mapper.MediaAssetMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public abstract class PlaywrightManualPublisher implements PlatformPublisher {

    private final MediaAssetMapper mediaAssetMapper;
    private final String uploadUrl;
    private final String browserProfileDir;
    private final String helperLogDir;
    private final Integer qrLoginTimeoutSec;

    protected PlaywrightManualPublisher(MediaAssetMapper mediaAssetMapper, AppProperties.Platform config) {
        this.mediaAssetMapper = mediaAssetMapper;
        this.uploadUrl = config.getUploadUrl();
        this.browserProfileDir = config.getBrowserProfileDir();
        this.helperLogDir = config.getHelperLogDir();
        this.qrLoginTimeoutSec = config.getQrLoginTimeoutSec();
    }

    protected abstract String scriptPlatformName();

    protected abstract String loginHint();

    @Override
    public PublishRecord publish(ContentTask task, ContentScript script) {
        MediaAsset videoAsset = mediaAssetMapper.selectOne(new LambdaQueryWrapper<MediaAsset>()
                .eq(MediaAsset::getTaskId, task.getId())
                .eq(MediaAsset::getAssetType, AssetType.VIDEO.name())
                .last("limit 1"));

        if (videoAsset == null) {
            PublishRecord failed = new PublishRecord();
            failed.setTaskId(task.getId());
            failed.setPlatform(platform());
            failed.setAccountName("manual-assist");
            failed.setStatus("FAILED");
            failed.setPublishedAt(LocalDateTime.now());
            failed.setResponseMessage("未找到可上传的视频文件");
            failed.setPlatformContentId("FAILED-" + task.getId());
            return failed;
        }

        try {
            Path generatedRoot = Path.of("./runtime/generated").toAbsolutePath().normalize();
            String relative = videoAsset.getFileUrl().replaceFirst("^/generated", "");
            Path videoPath = generatedRoot.resolve(relative.replaceFirst("^/", "")).normalize();

            Path profileDir = Path.of(browserProfileDir).toAbsolutePath().normalize();
            Path logDir = Path.of(helperLogDir).toAbsolutePath().normalize();
            Files.createDirectories(profileDir);
            Files.createDirectories(logDir);

            Path scriptPath = Path.of(System.getProperty("user.dir"), "scripts/platform_publish_helper.py").toAbsolutePath().normalize();
            Path logFile = logDir.resolve(platform().toLowerCase() + "-task-" + task.getId() + ".log");
            Path statusFile = logDir.resolve(platform().toLowerCase() + "-task-" + task.getId() + ".json");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python3",
                    scriptPath.toString(),
                    "--platform", scriptPlatformName(),
                    "--upload-url", uploadUrl,
                    "--video-path", videoPath.toString(),
                    "--title", script.getTitle(),
                    "--description", script.getIntroHook() + "\n" + script.getClosingCta(),
                    "--profile-dir", profileDir.toString(),
                    "--timeout-sec", String.valueOf(qrLoginTimeoutSec),
                    "--status-file", statusFile.toString()
            );
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(logFile.toFile());
            processBuilder.start();

            PublishRecord record = new PublishRecord();
            record.setTaskId(task.getId());
            record.setPlatform(platform());
            record.setAccountName("manual-assist");
            record.setStatus("MANUAL_REQUIRED");
            record.setPublishedAt(LocalDateTime.now());
            record.setPlatformContentId("MANUAL-" + task.getId());
            record.setResponseMessage("""
                    已启动 %s 辅助发布器。
                    %s
                    登录完成后系统会自动填入视频、标题和简介，最后一步“点击发布”由你手动完成。
                    视频文件：%s
                    标题建议：%s
                    简介建议：%s
                    浏览器配置目录：%s
                    状态文件：%s
                    调试日志：%s
                    """.formatted(
                    platform(),
                    loginHint(),
                    videoPath,
                    script.getTitle(),
                    script.getIntroHook() + " " + script.getClosingCta(),
                    profileDir,
                    statusFile,
                    logFile
            ));
            return record;
        } catch (IOException exception) {
            PublishRecord failed = new PublishRecord();
            failed.setTaskId(task.getId());
            failed.setPlatform(platform());
            failed.setAccountName("manual-assist");
            failed.setStatus("FAILED");
            failed.setPublishedAt(LocalDateTime.now());
            failed.setPlatformContentId("FAILED-" + task.getId());
            failed.setResponseMessage("启动辅助发布器失败: " + exception.getMessage());
            return failed;
        }
    }
}
