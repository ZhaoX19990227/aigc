package com.aigc.contentfactory.controller;

import com.aigc.contentfactory.common.ApiResponse;
import com.aigc.contentfactory.dto.HotspotResponse;
import com.aigc.contentfactory.entity.HotspotRecord;
import com.aigc.contentfactory.service.HotspotService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotspots")
public class HotspotController {

    private final HotspotService hotspotService;

    public HotspotController(HotspotService hotspotService) {
        this.hotspotService = hotspotService;
    }

    @PostMapping("/collect")
    public ApiResponse<List<HotspotResponse>> collect() {
        return ApiResponse.success(map(hotspotService.collectHotspots()));
    }

    @GetMapping
    public ApiResponse<List<HotspotResponse>> list() {
        return ApiResponse.success(map(hotspotService.latestHotspots()));
    }

    private List<HotspotResponse> map(List<HotspotRecord> records) {
        return records.stream()
                .map(item -> HotspotResponse.builder()
                        .id(String.valueOf(item.getId()))
                        .source(item.getSource())
                        .title(item.getTitle())
                        .summary(item.getSummary())
                        .score(item.getScore())
                        .capturedAt(item.getCapturedAt())
                        .build())
                .toList();
    }
}
