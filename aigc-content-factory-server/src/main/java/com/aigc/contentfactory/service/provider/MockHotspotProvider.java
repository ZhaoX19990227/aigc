package com.aigc.contentfactory.service.provider;

import com.aigc.contentfactory.service.model.HotspotPayload;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockHotspotProvider implements HotspotProvider {

    @Override
    public String source() {
        return "mock-aggregator";
    }

    @Override
    public List<HotspotPayload> fetch() {
        return List.of(
                HotspotPayload.builder()
                        .source("zhihu")
                        .sourceTopicId("zh-demo-1")
                        .title("AI 内容工厂如何把热点变成可复制的脚本资产")
                        .summary("内容团队关注从热点抓取到视频发布的全链路自动化。")
                        .score(new BigDecimal("92.30"))
                        .rawPayload("{\"provider\":\"mock\"}")
                        .build(),
                HotspotPayload.builder()
                        .source("weibo")
                        .sourceTopicId("wb-demo-2")
                        .title("短视频矩阵账号开始采用统一脚本模板与审核流")
                        .summary("运营侧更重视多人协同和平台差异化包装。")
                        .score(new BigDecimal("87.40"))
                        .rawPayload("{\"provider\":\"mock\"}")
                        .build(),
                HotspotPayload.builder()
                        .source("bilibili")
                        .sourceTopicId("bl-demo-3")
                        .title("企业知识账号需要可追踪的自动发布工作台")
                        .summary("关注任务状态、资产沉淀和复盘数据的一体化。")
                        .score(new BigDecimal("85.10"))
                        .rawPayload("{\"provider\":\"mock\"}")
                        .build()
        );
    }
}
