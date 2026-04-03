package com.aigc.contentfactory.service.provider;

import com.aigc.contentfactory.service.model.HotspotPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BilibiliHotspotProvider implements HotspotProvider {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public BilibiliHotspotProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String source() {
        return "bilibili";
    }

    @Override
    public List<HotspotPayload> fetch() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.bilibili.com/x/web-interface/popular?pn=1&ps=10"))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Referer", "https://www.bilibili.com/")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());
            if (root.path("code").asInt() != 0) {
                return List.of();
            }
            List<HotspotPayload> payloads = new ArrayList<>();
            for (JsonNode item : root.path("data").path("list")) {
                payloads.add(HotspotPayload.builder()
                        .source("bilibili")
                        .sourceTopicId(item.path("aid").asText())
                        .title(item.path("title").asText())
                        .summary(item.path("desc").asText(""))
                        .score(BigDecimal.valueOf(item.path("stat").path("view").asLong()))
                        .rawPayload(item.toString())
                        .build());
            }
            return payloads;
        } catch (Exception exception) {
            return List.of();
        }
    }
}
