package com.aigc.contentfactory.service;

import com.aigc.contentfactory.entity.HotspotRecord;
import com.aigc.contentfactory.service.model.ScriptDraft;
import com.aigc.contentfactory.service.model.TopicSuggestion;
import java.util.List;

public interface AiFacade {

    List<TopicSuggestion> suggestTopics(List<HotspotRecord> hotspots, String accountPositioning, List<String> platforms);

    ScriptDraft generateScript(String topic, String accountPositioning, List<String> platforms);
}
