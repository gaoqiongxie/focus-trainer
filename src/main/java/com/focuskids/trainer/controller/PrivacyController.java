package com.focuskids.trainer.controller;

import com.focuskids.trainer.common.api.R;
import com.focuskids.trainer.service.PrivacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 隐私合规控制器
 */
@RestController
@RequestMapping("/privacy")
@RequiredArgsConstructor
public class PrivacyController {

    private final PrivacyService privacyService;

    /**
     * 导出用户数据（GDPR合规）
     */
    @PostMapping("/export")
    public R<Map<String, Object>> exportData(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.success(privacyService.exportUserData(userId));
    }

    /**
     * 删除用户数据（GDPR合规，不可逆）
     * 仅家长可删除孩子数据
     */
    @DeleteMapping("/data")
    public R<Void> deleteData(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long parentId = (Long) request.getAttribute("userId");
        Long childId = params.get("childId") != null ? Long.valueOf(params.get("childId").toString()) : null;
        if (childId == null) {
            return R.error("childId不能为空");
        }
        privacyService.deleteUserData(childId, parentId);
        return R.success();
    }
}
