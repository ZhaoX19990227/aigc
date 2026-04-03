package com.aigc.contentfactory.service;

import com.aigc.contentfactory.entity.HotspotRecord;
import java.util.List;

public interface HotspotService {

    List<HotspotRecord> collectHotspots();

    List<HotspotRecord> latestHotspots();

    List<HotspotRecord> findByIds(List<Long> ids);
}
