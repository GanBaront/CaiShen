package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.PreferenceUpdateDTO;
import com.caishen.foodlab.dto.ProfileUpdateDTO;
import com.caishen.foodlab.security.AuthContext;
import com.caishen.foodlab.service.UserService;
import com.caishen.foodlab.vo.FreeQuotaVO;
import com.caishen.foodlab.vo.PreferenceVO;
import com.caishen.foodlab.vo.RecipeCardVO;
import com.caishen.foodlab.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户与偏好：基本资料、饮食偏好、收藏、历史、今日免费次数
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Result<UserInfoVO> profile() {
        return Result.success(userService.getProfile(AuthContext.requireUserId()));
    }

    @PutMapping("/profile")
    public Result<UserInfoVO> updateProfile(@RequestBody ProfileUpdateDTO dto) {
        return Result.success(userService.updateProfile(AuthContext.requireUserId(), dto));
    }

    @GetMapping("/preference")
    public Result<PreferenceVO> preference() {
        return Result.success(userService.getPreference(AuthContext.requireUserId()));
    }

    @PutMapping("/preference")
    public Result<PreferenceVO> savePreference(@RequestBody PreferenceUpdateDTO dto) {
        return Result.success(userService.savePreference(AuthContext.requireUserId(), dto));
    }

    @GetMapping("/history")
    public Result<List<RecipeCardVO>> history() {
        return Result.success(userService.history(AuthContext.requireUserId()));
    }

    @GetMapping("/favorites")
    public Result<List<RecipeCardVO>> favorites() {
        return Result.success(userService.favorites(AuthContext.requireUserId()));
    }

    @GetMapping("/free-quota")
    public Result<FreeQuotaVO> freeQuota() {
        return Result.success(userService.freeQuota(AuthContext.requireUserId()));
    }
}
