package com.aigc.contentfactory.service.impl;

import com.aigc.contentfactory.config.AppProperties;
import com.aigc.contentfactory.enums.AssetType;
import com.aigc.contentfactory.service.AssetPipelineService;
import com.aigc.contentfactory.service.model.GeneratedAsset;
import com.aigc.contentfactory.service.model.GeneratedAssetBundle;
import com.aigc.contentfactory.service.model.ScriptDraft;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class LocalAssetPipelineService implements AssetPipelineService {

    private final AppProperties properties;

    public LocalAssetPipelineService(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public GeneratedAssetBundle generate(Long taskId, ScriptDraft draft) {
        try {
            Path root = Path.of(properties.getStorage().getRootDir()).toAbsolutePath().normalize();
            Files.createDirectories(root);
            Path imagesDir = Files.createDirectories(root.resolve("images"));
            Path audioDir = Files.createDirectories(root.resolve("audio"));
            Path subtitlesDir = Files.createDirectories(root.resolve("subtitles"));
            Path videosDir = Files.createDirectories(root.resolve("videos"));

            Path imageFile = imagesDir.resolve(taskId + ".png");
            renderCoverImage(imageFile, draft);

            Path subtitleFile = subtitlesDir.resolve(taskId + ".srt");
            writeSubtitle(subtitleFile, draft);

            Path tempAiff = audioDir.resolve(taskId + ".aiff");
            Path audioFile = audioDir.resolve(taskId + ".mp3");
            synthesizeSpeech(tempAiff, audioFile, draft);

            Path videoFile = videosDir.resolve(taskId + ".mp4");
            renderVideo(imageFile, audioFile, subtitleFile, videoFile);

            List<GeneratedAsset> assets = new ArrayList<>();
            assets.add(asset(AssetType.IMAGE, imageFile, "image/png", null, "prompt=" + draft.getImagePrompt(), "images"));
            assets.add(asset(AssetType.AUDIO, audioFile, "audio/mpeg", draft.getEstimatedDurationSec(), "voice=" + draft.getVoiceTone(), "audio"));
            assets.add(asset(AssetType.SUBTITLE, subtitleFile, "application/x-subrip", draft.getEstimatedDurationSec(), "segments=" + draft.getSegments().size(), "subtitles"));
            assets.add(asset(AssetType.VIDEO, videoFile, "video/mp4", draft.getEstimatedDurationSec(), "resolution=1080x1920", "videos"));
            return GeneratedAssetBundle.builder().assets(assets).build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("本地资产生成失败: " + exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new IllegalStateException("本地资产生成失败: " + exception.getMessage(), exception);
        }
    }

    private GeneratedAsset asset(AssetType type, Path file, String mimeType, Integer durationSec, String params, String subDir)
            throws IOException {
        return GeneratedAsset.builder()
                .assetType(type)
                .fileName(file.getFileName().toString())
                .fileUrl(properties.getStorage().getBaseUrl() + "/" + subDir + "/" + file.getFileName())
                .mimeType(mimeType)
                .durationSec(durationSec)
                .fileSize(Files.size(file))
                .generationParams(params)
                .build();
    }

    private void renderCoverImage(Path imageFile, ScriptDraft draft) throws IOException {
        BufferedImage image = new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setPaint(new GradientPaint(0, 0, new Color(19, 27, 54), 1080, 1920, new Color(184, 91, 37)));
        graphics.fillRect(0, 0, 1080, 1920);

        graphics.setColor(new Color(255, 246, 237, 230));
        graphics.fillRoundRect(80, 120, 920, 1520, 48, 48);

        graphics.setColor(new Color(32, 23, 16));
        graphics.setFont(new Font("PingFang SC", Font.BOLD, 62));
        drawWrappedText(graphics, draft.getTitle(), 140, 260, 800, 84);

        graphics.setFont(new Font("PingFang SC", Font.PLAIN, 34));
        graphics.setColor(new Color(104, 79, 63));
        drawWrappedText(graphics, draft.getIntroHook(), 140, 560, 800, 48);

        graphics.setFont(new Font("PingFang SC", Font.BOLD, 30));
        graphics.setColor(new Color(177, 77, 33));
        int y = 760;
        for (String segment : draft.getSegments()) {
            drawWrappedText(graphics, "• " + segment, 140, y, 800, 42);
            y += 180;
        }

        graphics.setColor(new Color(244, 220, 204));
        graphics.fillRoundRect(140, 1500, 380, 92, 32, 32);
        graphics.setColor(new Color(125, 49, 16));
        graphics.setFont(new Font("PingFang SC", Font.BOLD, 28));
        graphics.drawString("AIGC CONTENT FACTORY", 176, 1558);
        graphics.dispose();
        ImageIO.write(image, "png", imageFile.toFile());
    }

    private void drawWrappedText(Graphics2D graphics, String text, int x, int startY, int maxWidth, int lineHeight) {
        List<String> lines = wrapText(text, 18);
        int y = startY;
        for (String line : lines) {
            graphics.drawString(line, x, y);
            y += lineHeight;
        }
    }

    private List<String> wrapText(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        String normalized = text == null ? "" : text.replace("\n", " ");
        for (int index = 0; index < normalized.length(); index += maxChars) {
            lines.add(normalized.substring(index, Math.min(normalized.length(), index + maxChars)));
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private void writeSubtitle(Path subtitleFile, ScriptDraft draft) throws IOException {
        int segmentCount = Math.max(draft.getSegments().size(), 1);
        int totalDuration = Math.max(draft.getEstimatedDurationSec(), segmentCount * 6);
        int slot = Math.max(totalDuration / segmentCount, 4);

        List<String> lines = new ArrayList<>();
        int current = 0;
        for (int index = 0; index < draft.getSegments().size(); index++) {
            lines.add(String.valueOf(index + 1));
            lines.add(formatTime(current) + " --> " + formatTime(current + slot));
            lines.add(draft.getSegments().get(index));
            lines.add("");
            current += slot;
        }
        Files.writeString(subtitleFile, String.join("\n", lines), StandardCharsets.UTF_8);
    }

    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int remaining = seconds % 60;
        return String.format("%02d:%02d:%02d,000", hours, minutes, remaining);
    }

    private void synthesizeSpeech(Path tempAiff, Path audioFile, ScriptDraft draft) throws IOException, InterruptedException {
        runCommand(List.of(
                "/usr/bin/say",
                "-v", "Tingting",
                "-r", "195",
                "-o", tempAiff.toString(),
                draft.getIntroHook() + " " + String.join(" ", draft.getSegments()) + " " + draft.getClosingCta()
        ), "语音合成失败");
        runCommand(List.of(
                "ffmpeg", "-y",
                "-i", tempAiff.toString(),
                "-codec:a", "libmp3lame",
                "-q:a", "2",
                audioFile.toString()
        ), "音频转码失败");
        Files.deleteIfExists(tempAiff);
    }

    private void renderVideo(Path imageFile, Path audioFile, Path subtitleFile, Path videoFile) throws IOException, InterruptedException {
        String subtitleFilter = "subtitles=filename='" + escapeForSubtitleFilter(subtitleFile.toString()) + "'";
        List<String> withSubtitles = List.of(
                "ffmpeg", "-y",
                "-loop", "1",
                "-i", imageFile.toString(),
                "-i", audioFile.toString(),
                "-vf", subtitleFilter,
                "-c:v", "libx264",
                "-tune", "stillimage",
                "-c:a", "aac",
                "-b:a", "192k",
                "-pix_fmt", "yuv420p",
                "-shortest",
                videoFile.toString()
        );
        try {
            runCommand(withSubtitles, "视频合成失败");
        } catch (IllegalStateException exception) {
            runCommand(List.of(
                    "ffmpeg", "-y",
                    "-loop", "1",
                    "-i", imageFile.toString(),
                    "-i", audioFile.toString(),
                    "-c:v", "libx264",
                    "-tune", "stillimage",
                    "-c:a", "aac",
                    "-b:a", "192k",
                    "-pix_fmt", "yuv420p",
                    "-shortest",
                    videoFile.toString()
            ), "视频合成失败");
        }
    }

    private String escapeForSubtitleFilter(String path) {
        return path.replace("\\", "\\\\")
                .replace(":", "\\:")
                .replace(",", "\\,")
                .replace("'", "\\'");
    }

    private void runCommand(List<String> command, String message) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException(message + ": " + output);
        }
    }
}
