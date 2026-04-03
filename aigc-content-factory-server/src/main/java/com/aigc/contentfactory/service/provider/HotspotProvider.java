package com.aigc.contentfactory.service.provider;

import com.aigc.contentfactory.service.model.HotspotPayload;
import java.util.List;

public interface HotspotProvider {

    String source();

    List<HotspotPayload> fetch();
}
