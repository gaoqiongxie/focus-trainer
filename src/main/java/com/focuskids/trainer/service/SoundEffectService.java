package com.focuskids.trainer.service;

import com.focuskids.trainer.entity.SoundEffectConfig;

import java.util.List;

/**
 * 音效配置服务
 */
public interface SoundEffectService {

    /**
     * 获取所有启用的音效配置
     */
    List<SoundEffectConfig> listAll();

    /**
     * 按类别获取音效配置
     */
    List<SoundEffectConfig> listByCategory(String category);

    /**
     * 获取单个音效配置
     */
    SoundEffectConfig getByKey(String soundKey);
}
