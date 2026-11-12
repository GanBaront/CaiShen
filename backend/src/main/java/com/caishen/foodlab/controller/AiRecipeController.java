package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.AiRecipeGenerateDTO;
import com.caishen.foodlab.security.AuthContext;
import com.caishen.foodlab.service.AiRecipeService;
import com.caishen.foodlab.vo.AiRecipePreviewVO;
import com.caishen.foodlab.vo.RecipeCardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 新菜谱：生成（会员 / 单次）、预览
 */
@RestController
@RequestMapping("/api/ai/recipes")
@RequiredArgsConstructor
public class AiRecipeController {

    private final AiRecipeService aiRecipeService;

    /** 会员 / 单次「AI 新菜谱」生成（真实调用大模型） */
    @PostMapping("/generate")
    public Result<RecipeCardVO> generate(@RequestBody AiRecipeGenerateDTO dto) {
        return Result.success(aiRecipeService.generate(AuthContext.requireUserId(), dto));
    }

    /** AI 新菜谱预览（示例，取库中 AI 生成的菜谱） */
    @GetMapping("/preview")
    public Result<AiRecipePreviewVO> preview() {
        return Result.success(aiRecipeService.preview(AuthContext.getUserId()));
    }
}
