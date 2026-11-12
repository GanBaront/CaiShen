package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.dto.RegisterDTO;
import com.caishen.foodlab.dto.LoginDTO;
import com.caishen.foodlab.entity.Membership;
import com.caishen.foodlab.entity.User;
import com.caishen.foodlab.mapper.UserMapper;
import com.caishen.foodlab.security.JwtUtil;
import com.caishen.foodlab.security.PasswordUtil;
import com.caishen.foodlab.vo.AuthVO;
import com.caishen.foodlab.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 认证：账号 + 密码注册 / 登录 / 退出 / 当前用户
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final FreeQuotaService freeQuotaService;

    public AuthVO register(RegisterDTO dto) {
        String account = dto.getAccount().trim();
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getAccount, account)) > 0) {
            throw new BusinessException("该账号已被注册");
        }
        User user = new User();
        user.setAccount(account);
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
                ? account : dto.getNickname().trim());
        user.setStatus(1);
        userMapper.insert(user);
        return buildAuth(user);
    }

    public AuthVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getAccount, dto.getAccount().trim()));
        if (user == null || !PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被停用");
        }
        return buildAuth(user);
    }

    public UserInfoVO me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserInfo(user);
    }

    public UserInfoVO toUserInfo(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setAccount(user.getAccount());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        Membership membership = freeQuotaService.currentMembership(user.getId());
        vo.setMember(membership != null);
        vo.setMemberEndTime(membership == null ? null : membership.getEndTime());
        return vo;
    }

    private AuthVO buildAuth(User user) {
        AuthVO vo = new AuthVO();
        vo.setToken(jwtUtil.generateUserToken(user.getId(), user.getAccount()));
        vo.setUser(toUserInfo(user));
        return vo;
    }
}
