package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.UserIngredientAddDTO;
import com.caishen.foodlab.entity.AllergenTag;
import com.caishen.foodlab.entity.Ingredient;
import com.caishen.foodlab.entity.UserIngredient;
import com.caishen.foodlab.security.AuthContext;
import com.caishen.foodlab.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 食材：常用食材、联想搜索；我的食材篮增删查（文字 / 图片 / 语音识别结果统一入口）
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    /** 常用食材（洋葱 / 豆腐 / 胡萝卜 / 五花肉） */
    @GetMapping("/ingredients/common")
    public Result<List<Ingredient>> common() {
        return Result.success(ingredientService.common());
    }

    /** 食材联想/搜索 */
    @GetMapping("/ingredients/search")
    public Result<List<Ingredient>> search(@RequestParam(name = "keyword", required = false) String keyword) {
        return Result.success(ingredientService.search(keyword));
    }

    /** 启用中的过敏源标签（管理端过敏源标签库维护，用户端忌口/过敏源选择用） */
    @GetMapping("/ingredients/allergen-tags")
    public Result<List<AllergenTag>> allergenTags() {
        return Result.success(ingredientService.enabledAllergenTags());
    }

    /** 我的食材篮 */
    @GetMapping("/user/ingredients")
    public Result<List<UserIngredient>> myIngredients() {
        return Result.success(ingredientService.myIngredients(AuthContext.requireUserId()));
    }

    /** 新增食材 */
    @PostMapping("/user/ingredients")
    public Result<List<UserIngredient>> addIngredients(@RequestBody UserIngredientAddDTO dto) {
        return Result.success(ingredientService.add(AuthContext.requireUserId(), dto));
    }

    /** 移除食材 */
    @DeleteMapping("/user/ingredients/{id}")
    public Result<Void> removeIngredient(@PathVariable Long id) {
        ingredientService.remove(AuthContext.requireUserId(), id);
        return Result.success();
    }
}
