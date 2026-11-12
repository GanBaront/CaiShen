package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.dto.AdminLoginDTO;
import com.caishen.foodlab.entity.AdminUser;
import com.caishen.foodlab.mapper.AdminUserMapper;
import com.caishen.foodlab.security.JwtUtil;
import com.caishen.foodlab.security.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端认证：登录 / 退出
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(AdminLoginDTO dto) {
        AdminUser admin = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, dto.getUsername().trim()));
        if (admin == null || !PasswordUtil.matches(dto.getPassword(), admin.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException("该管理员账号已停用");
        }
        AdminUser update = new AdminUser();
        update.setId(admin.getId());
        update.setLastLoginTime(LocalDateTime.now());
        adminUserMapper.updateById(update);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", admin.getId());
        info.put("username", admin.getUsername());
        info.put("realName", admin.getRealName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtUtil.generateAdminToken(admin.getId(), admin.getUsername()));
        result.put("admin", info);
        return result;
    }
}
