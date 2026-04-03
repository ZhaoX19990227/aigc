package com.aigc.contentfactory.service.publisher;

import com.aigc.contentfactory.entity.ContentTask;
import com.aigc.contentfactory.entity.ContentScript;
import com.aigc.contentfactory.entity.PublishRecord;

public interface PlatformPublisher {

    String platform();

    PublishRecord publish(ContentTask task, ContentScript script);
}
