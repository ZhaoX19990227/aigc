package com.aigc.contentfactory.service.publisher;

import com.aigc.contentfactory.entity.ContentScript;
import com.aigc.contentfactory.entity.ContentTask;
import com.aigc.contentfactory.entity.PublishRecord;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class LocalSimulationPublisher implements PlatformPublisher {

    @Override
    public String platform() {
        return "LOCAL_SIMULATION";
    }

    @Override
    public PublishRecord publish(ContentTask task, ContentScript script) {
        PublishRecord record = new PublishRecord();
        record.setTaskId(task.getId());
        record.setPlatform("LOCAL_SIMULATION");
        record.setAccountName("demo-account");
        record.setStatus("SUCCESS");
        record.setPlatformContentId("LOCAL-" + task.getId());
        record.setPublishedAt(LocalDateTime.now());
        record.setResponseMessage("本地模拟发布成功，可替换为真实平台适配器。");
        return record;
    }
}
