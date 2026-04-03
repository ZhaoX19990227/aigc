package com.aigc.contentfactory.service;

import com.aigc.contentfactory.service.model.GeneratedAssetBundle;
import com.aigc.contentfactory.service.model.ScriptDraft;

public interface AssetPipelineService {

    GeneratedAssetBundle generate(Long taskId, ScriptDraft draft);
}
