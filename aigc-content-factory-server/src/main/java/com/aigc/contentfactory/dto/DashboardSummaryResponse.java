package com.aigc.contentfactory.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardSummaryResponse {

    private long hotspotCount;
    private long taskCount;
    private long reviewPendingCount;
    private long publishedCount;
    private List<String> supportedPlatforms;
}
