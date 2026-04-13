package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.entity.SoundEffectConfig;
import com.focuskids.trainer.service.SoundEffectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 音效配置控制器
 */
@RestController
@RequestMapping("/sound")
@RequiredArgsConstructor
public class SoundEffectController {

    private final SoundEffectService soundEffectService;

    /**
     * 获取所有启用的音效配置
     */
    @GetMapping("/list")
    public R<List<SoundEffectConfig>> listAll() {
        return R.success(soundEffectService.listAll());
    }

    /**
     * 按类别获取音效配置
     */
    @GetMapping("/category/{category}")
    public R<List<SoundEffectConfig>> listByCategory(@PathVariable String category) {
        return R.success(soundEffectService.listByCategory(category));
    }

    /**
     * 获取单个音效配置
     */
    @GetMapping("/key/{soundKey}")
    public R<SoundEffectConfig> getByKey(@PathVariable String soundKey) {
        return R.success(soundEffectService.getByKey(soundKey));
    }
}
