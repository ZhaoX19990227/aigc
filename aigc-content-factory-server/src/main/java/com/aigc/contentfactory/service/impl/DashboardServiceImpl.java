package com.aigc.contentfactory.service.impl;

import com.aigc.contentfactory.dto.DashboardSummaryResponse;
import com.aigc.contentfactory.entity.ContentTask;
import com.aigc.contentfactory.entity.HotspotRecord;
import com.aigc.contentfactory.enums.ReviewStatus;
import com.aigc.contentfactory.enums.TaskStatus;
import com.aigc.contentfactory.mapper.ContentTaskMapper;
import com.aigc.contentfactory.mapper.HotspotRecordMapper;
import com.aigc.contentfactory.service.DashboardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final HotspotRecordMapper hotspotRecordMapper;
    private final ContentTaskMapper contentTaskMapper;

    public DashboardServiceImpl(HotspotRecordMapper hotspotRecordMapper, ContentTaskMapper contentTaskMapper) {
        this.hotspotRecordMapper = hotspotRecordMapper;
        this.contentTaskMapper = contentTaskMapper;
    }

    @Override
    public DashboardSummaryResponse summary() {
        long hotspotCount = hotspotRecordMapper.selectCount(new LambdaQueryWrapper<>(HotspotRecord.class));
        long taskCount = contentTaskMapper.selectCount(new LambdaQueryWrapper<>(ContentTask.class));
        long reviewPendingCount = contentTaskMapper.selectCount(new LambdaQueryWrapper<ContentTask>()
                .eq(ContentTask::getReviewStatus, ReviewStatus.PENDING.name()));
        long publishedCount = contentTaskMapper.selectCount(new LambdaQueryWrapper<ContentTask>()
                .eq(ContentTask::getStatus, TaskStatus.PUBLISHED.name()));
        return DashboardSummaryResponse.builder()
                .hotspotCount(hotspotCount)
                .taskCount(taskCount)
                .reviewPendingCount(reviewPendingCount)
                .publishedCount(publishedCount)
                .supportedPlatforms(List.of("XIAOHONGSHU", "DOUYIN", "BILIBILI"))
                .build();
    }
}
