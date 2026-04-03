package com.aigc.contentfactory.service.impl;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface OpenAiStructuredAssistant {

    @SystemMessage("""
            你是企业级 AIGC 内容工厂的选题策略专家。
            你必须只返回合法 JSON，不要输出 markdown，不要输出解释。
            """)
    @UserMessage("""
            请基于以下账号定位和候选热点，选择最适合做短视频的 3 个选题。

            账号定位：
            {{positioning}}

            目标平台：
            {{platforms}}

            候选热点：
            {{hotspots}}

            返回 JSON，格式如下：
            {
              "selectedTopics": [
                {
                  "title": "string",
                  "reason": "string",
                  "targetAudience": "string",
                  "suggestedPlatforms": ["string"],
                  "riskFlags": ["string"],
                  "priority": 1
                }
              ]
            }
            """)
    String suggestTopics(@V("positioning") String positioning,
                         @V("platforms") String platforms,
                         @V("hotspots") String hotspots);

    @SystemMessage("""
            你是短视频内容导演和脚本策划。
            你必须只返回合法 JSON，不要输出 markdown，不要输出解释。
            """)
    @UserMessage("""
            请围绕以下话题生成一个适合 60 秒内短视频的结构化脚本。

            账号定位：
            {{positioning}}

            目标平台：
            {{platforms}}

            话题：
            {{topic}}

            返回 JSON，格式如下：
            {
              "title": "string",
              "introHook": "string",
              "segments": ["string", "string", "string"],
              "closingCta": "string",
              "tags": ["string"],
              "estimatedDurationSec": 55,
              "voiceTone": "string",
              "imagePrompt": "string"
            }
            """)
    String generateScript(@V("positioning") String positioning,
                          @V("platforms") String platforms,
                          @V("topic") String topic);
}
