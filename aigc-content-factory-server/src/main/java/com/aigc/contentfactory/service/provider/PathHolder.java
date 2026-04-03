package com.aigc.contentfactory.service.provider;

import com.aigc.contentfactory.service.model.HotspotPayload;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class PathHolder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PathHolder() {
    }

    static String script(String name) {
        return Path.of(System.getProperty("user.dir"), "scripts", name).toAbsolutePath().normalize().toString();
    }

    static List<HotspotPayload> parseHotspots(String json, String source) throws IOException {
        List<Map<String, Object>> list = OBJECT_MAPPER.readValue(json, new TypeReference<>() {
        });
        List<HotspotPayload> payloads = new ArrayList<>();
        for (Map<String, Object> item : list) {
            payloads.add(HotspotPayload.builder()
                    .source(source)
                    .sourceTopicId(String.valueOf(item.getOrDefault("id", "")))
                    .title(String.valueOf(item.getOrDefault("title", "")))
                    .summary(String.valueOf(item.getOrDefault("summary", "")))
                    .score(new BigDecimal(String.valueOf(item.getOrDefault("score", "0"))))
                    .rawPayload(OBJECT_MAPPER.writeValueAsString(item))
                    .build());
        }
        return payloads;
    }
}
