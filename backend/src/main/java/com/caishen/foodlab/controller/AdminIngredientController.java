package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.PageResult;
import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.AllergenTagSaveDTO;
import com.caishen.foodlab.dto.IngredientSaveDTO;
import com.caishen.foodlab.dto.NutritionSaveDTO;
import com.caishen.foodlab.entity.AllergenTag;
import com.caishen.foodlab.entity.Ingredient;
import com.caishen.foodlab.entity.IngredientNutrition;
import com.caishen.foodlab.service.AdminContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 后台内容管理：食材库、营养数据、过敏源标签
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminIngredientController {

    private final AdminContentService adminContentService;

    // ==================== 食材库 ====================

    @GetMapping("/ingredients")
    public Result<PageResult<Ingredient>> ingredientPage(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String category) {
        return Result.success(adminContentService.ingredientPage(page, size, keyword, category));
    }

    @PostMapping("/ingredients")
    public Result<Ingredient> ingredientCreate(@RequestBody IngredientSaveDTO dto) {
        return Result.success(adminContentService.ingredientCreate(dto));
    }

    @PutMapping("/ingredients/{id}")
    public Result<Ingredient> ingredientUpdate(@PathVariable Long id, @RequestBody IngredientSaveDTO dto) {
        return Result.success(adminContentService.ingredientUpdate(id, dto));
    }

    @DeleteMapping("/ingredients/{id}")
    public Result<Void> ingredientDelete(@PathVariable Long id) {
        adminContentService.ingredientDelete(id);
        return Result.success();
    }

    // ==================== 营养数据 ====================

    @GetMapping("/nutrition")
    public Result<PageResult<Map<String, Object>>> nutritionPage(@RequestParam(defaultValue = "1") long page,
                                                                  @RequestParam(defaultValue = "10") long size,
                                                                  @RequestParam(required = false) String keyword) {
        return Result.success(adminContentService.nutritionPage(page, size, keyword));
    }

    @PutMapping("/nutrition")
    public Result<IngredientNutrition> nutritionSave(@RequestBody NutritionSaveDTO dto) {
        return Result.success(adminContentService.nutritionSave(dto));
    }

    // ==================== 过敏源标签 ====================

    @GetMapping("/allergen-tags")
    public Result<List<AllergenTag>> allergenTags() {
        return Result.success(adminContentService.allergenTags());
    }

    @PostMapping("/allergen-tags")
    public Result<AllergenTag> allergenTagCreate(@RequestBody AllergenTagSaveDTO dto) {
        return Result.success(adminContentService.allergenTagCreate(dto));
    }

    @PutMapping("/allergen-tags/{id}")
    public Result<AllergenTag> allergenTagUpdate(@PathVariable Long id, @RequestBody AllergenTagSaveDTO dto) {
        return Result.success(adminContentService.allergenTagUpdate(id, dto));
    }

    @DeleteMapping("/allergen-tags/{id}")
    public Result<Void> allergenTagDelete(@PathVariable Long id) {
        adminContentService.allergenTagDelete(id);
        return Result.success();
    }
}
