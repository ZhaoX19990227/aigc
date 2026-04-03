package com.aigc.contentfactory.service.impl;

import com.aigc.contentfactory.entity.HotspotRecord;
import com.aigc.contentfactory.mapper.HotspotRecordMapper;
import com.aigc.contentfactory.service.HotspotService;
import com.aigc.contentfactory.service.model.HotspotPayload;
import com.aigc.contentfactory.service.provider.HotspotProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HotspotServiceImpl implements HotspotService {

    private final List<HotspotProvider> providers;
    private final HotspotRecordMapper hotspotRecordMapper;

    public HotspotServiceImpl(List<HotspotProvider> providers, HotspotRecordMapper hotspotRecordMapper) {
        this.providers = providers;
        this.hotspotRecordMapper = hotspotRecordMapper;
    }

    @Override
    public List<HotspotRecord> collectHotspots() {
        List<HotspotRecord> records = new ArrayList<>();
        for (HotspotProvider provider : providers) {
            for (HotspotPayload payload : provider.fetch()) {
                HotspotRecord record = new HotspotRecord();
                record.setSource(payload.getSource());
                record.setSourceTopicId(payload.getSourceTopicId());
                record.setTitle(payload.getTitle());
                record.setSummary(payload.getSummary());
                record.setScore(payload.getScore());
                record.setCapturedAt(LocalDateTime.now());
                record.setRawPayload(payload.getRawPayload());
                hotspotRecordMapper.insert(record);
                records.add(record);
            }
        }
        return records;
    }

    @Override
    public List<HotspotRecord> latestHotspots() {
        return hotspotRecordMapper.selectList(new LambdaQueryWrapper<HotspotRecord>()
                .orderByDesc(HotspotRecord::getCapturedAt)
                .last("limit 20"));
    }
}
