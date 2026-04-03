package com.aigc.contentfactory.service.provider;

import com.aigc.contentfactory.config.AppProperties;
import com.aigc.contentfactory.service.model.HotspotPayload;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ZhihuHotspotProvider implements HotspotProvider {

    private final AppProperties properties;

    public ZhihuHotspotProvider(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public String source() {
        return "zhihu";
    }

    @Override
    public List<HotspotPayload> fetch() {
        try {
            Process process = new ProcessBuilder(
                    "python3",
                    PathHolder.script("fetch_hotspot_pages.py"),
                    "--platform",
                    "zhihu",
                    "--profile-dir",
                    properties.getHotspot().getZhihuProfileDir()
            ).start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().reduce("", (a, b) -> a + b);
            }
            process.waitFor();
            return PathHolder.parseHotspots(output, "zhihu");
        } catch (Exception exception) {
            return List.of();
        }
    }
}
