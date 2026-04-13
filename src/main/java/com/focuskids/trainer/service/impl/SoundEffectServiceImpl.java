package com.focuskids.trainer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focuskids.trainer.entity.SoundEffectConfig;
import com.focuskids.trainer.mapper.SoundEffectConfigMapper;
import com.focuskids.trainer.service.SoundEffectService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SoundEffectServiceImpl implements SoundEffectService {

    private final SoundEffectConfigMapper soundEffectConfigMapper;

    @Override
    public List<SoundEffectConfig> listAll() {
        return soundEffectConfigMapper.selectList(
                new LambdaQueryWrapper<SoundEffectConfig>()
                        .eq(SoundEffectConfig::getIsActive, 1)
                        .orderByAsc(SoundEffectConfig::getCategory)
                        .orderByAsc(SoundEffectConfig::getDisplayOrder)
        );
    }

    @Override
    public List<SoundEffectConfig> listByCategory(String category) {
        return soundEffectConfigMapper.selectList(
                new LambdaQueryWrapper<SoundEffectConfig>()
                        .eq(SoundEffectConfig::getIsActive, 1)
                        .eq(SoundEffectConfig::getCategory, category)
                        .orderByAsc(SoundEffectConfig::getDisplayOrder)
        );
    }

    @Override
    public SoundEffectConfig getByKey(String soundKey) {
        return soundEffectConfigMapper.selectOne(
                new LambdaQueryWrapper<SoundEffectConfig>()
                        .eq(SoundEffectConfig::getSoundKey, soundKey)
                        .eq(SoundEffectConfig::getIsActive, 1)
        );
    }
}
