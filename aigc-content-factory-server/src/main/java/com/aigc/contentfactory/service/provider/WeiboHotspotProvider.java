package com.aigc.contentfactory.service.provider;

import com.aigc.contentfactory.service.model.HotspotPayload;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WeiboHotspotProvider implements HotspotProvider {

    @Override
    public String source() {
        return "weibo";
    }

    @Override
    public List<HotspotPayload> fetch() {
        try {
            Process process = new ProcessBuilder(
                    "python3",
                    PathHolder.script("fetch_hotspot_pages.py"),
                    "--platform",
                    "weibo"
            ).start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().reduce("", (a, b) -> a + b);
            }
            process.waitFor();
            return PathHolder.parseHotspots(output, "weibo");
        } catch (Exception exception) {
            return List.of();
        }
    }
}
