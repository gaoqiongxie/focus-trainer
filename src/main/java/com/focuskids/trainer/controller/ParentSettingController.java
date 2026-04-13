package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.entity.ParentSetting;
import com.focuskids.trainer.mapper.ParentSettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 家长设置控制器
 */
@RestController
@RequestMapping("/parent/setting")
@RequiredArgsConstructor
public class ParentSettingController {

    private final ParentSettingMapper parentSettingMapper;

    /**
     * 获取家长设置
     */
    @GetMapping("/{childId}")
    public R<ParentSetting> getSetting(HttpServletRequest request, @PathVariable Long childId) {
        Long parentId = (Long) request.getAttribute("userId");
        ParentSetting setting = parentSettingMapper.selectOne(
                new LambdaQueryWrapper<ParentSetting>()
                        .eq(ParentSetting::getUserId, parentId)
                        .eq(ParentSetting::getChildId, childId)
        );
        return R.success(setting);
    }

    /**
     * 更新家长设置
     */
    @PutMapping("/{childId}")
    public R<ParentSetting> updateSetting(HttpServletRequest request,
                                           @PathVariable Long childId,
                                           @RequestBody ParentSetting params) {
        Long parentId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<ParentSetting> wrapper = new LambdaQueryWrapper<ParentSetting>()
                .eq(ParentSetting::getUserId, parentId)
                .eq(ParentSetting::getChildId, childId);

        ParentSetting setting = parentSettingMapper.selectOne(wrapper);
        if (setting == null) {
            setting = new ParentSetting();
            setting.setUserId(parentId);
            setting.setChildId(childId);
        }

        if (params.getTrainingLock() != null) {
            setting.setTrainingLock(params.getTrainingLock());
        }
        if (params.getDailyLimitMin() != null) {
            setting.setDailyLimitMin(params.getDailyLimitMin());
        }
        if (params.getRemindTime() != null) {
            setting.setRemindTime(params.getRemindTime());
        }

        if (setting.getSettingId() == null) {
            parentSettingMapper.insert(setting);
        } else {
            parentSettingMapper.updateById(setting);
        }

        return R.success(setting);
    }
}
