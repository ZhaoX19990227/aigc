package com.aigc.contentfactory.service.impl;

import com.aigc.contentfactory.service.HotspotService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DemoWorkflowScheduler {

    private final HotspotService hotspotService;

    public DemoWorkflowScheduler(HotspotService hotspotService) {
        this.hotspotService = hotspotService;
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void collectHotspots() {
        hotspotService.collectHotspots();
    }
}
