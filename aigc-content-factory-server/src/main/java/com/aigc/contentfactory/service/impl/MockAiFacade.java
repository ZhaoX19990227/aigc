package com.aigc.contentfactory.service.impl;

import com.aigc.contentfactory.entity.HotspotRecord;
import com.aigc.contentfactory.service.AiFacade;
import com.aigc.contentfactory.service.model.ScriptDraft;
import com.aigc.contentfactory.service.model.TopicSuggestion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MockAiFacade implements AiFacade {

    @Override
    public List<TopicSuggestion> suggestTopics(List<HotspotRecord> hotspots, String accountPositioning, List<String> platforms) {
        List<TopicSuggestion> results = new ArrayList<>();
        hotspots.stream()
                .sorted(Comparator.comparing(HotspotRecord::getScore).reversed())
                .limit(3)
                .forEach(hotspot -> results.add(TopicSuggestion.builder()
                        .title(hotspot.getTitle())
                        .reason("热点分值高，且适合拆成“问题-方法-结果”短视频结构，匹配账号定位：" + normalize(accountPositioning))
                        .targetAudience("关注 AI 自动化、内容运营与增长的从业者")
                        .suggestedPlatforms(platforms)
                        .riskFlags(List.of("需人工确认平台合规表述"))
                        .priority(results.size() + 1)
                        .build()));
        return results;
    }

    @Override
    public ScriptDraft generateScript(String topic, String accountPositioning, List<String> platforms) {
        return ScriptDraft.builder()
                .title("1 套工作流，把“" + topic + "”做成可复制内容")
                .introHook("很多团队不是不会做内容，而是不会把热点变成可以重复生产的流程。")
                .segments(List.of(
                        "第一步，先抓热点，不要抓所有热点，只抓跟账号定位真正相关的题材。",
                        "第二步，用固定的选题规则和大模型判断，快速过滤掉不适合转化的热点。",
                        "第三步，把脚本、配图、配音、字幕和发布都拆成独立步骤，每一步都能重试。",
                        "最后，把审核流和发布记录加进来，内容工厂才不是一次性脚本。"))
                .closingCta("如果你也想把内容生产做成系统化流程，评论区告诉我你的场景。")
                .tags(List.of("AIGC内容工厂", "LangChain4j", "自动化运营", "短视频系统"))
                .estimatedDurationSec(55)
                .voiceTone("专业、直接、节奏快")
                .imagePrompt("未来感运营工作台、数据看板、短视频编辑时间线、企业内容工厂")
                .build();
    }

    private String normalize(String positioning) {
        return positioning == null || positioning.isBlank() ? "AI 自动化内容运营" : positioning;
    }
}
