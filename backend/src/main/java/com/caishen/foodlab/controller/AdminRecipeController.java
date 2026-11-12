package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.PageResult;
import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.RecipeSaveDTO;
import com.caishen.foodlab.dto.StatusUpdateDTO;
import com.caishen.foodlab.entity.Recipe;
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

/**
 * 后台菜谱库：CRUD + 上架/下架/审核通过
 */
@RestController
@RequestMapping("/api/admin/recipes")
@RequiredArgsConstructor
public class AdminRecipeController {

    private final AdminContentService adminContentService;

    @GetMapping
    public Result<PageResult<Recipe>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String status) {
        return Result.success(adminContentService.recipePage(page, size, keyword, status));
    }

    @PostMapping
    public Result<Recipe> create(@RequestBody RecipeSaveDTO dto) {
        return Result.success(adminContentService.recipeCreate(dto));
    }

    @PutMapping("/{id}")
    public Result<Recipe> update(@PathVariable Long id, @RequestBody RecipeSaveDTO dto) {
        return Result.success(adminContentService.recipeUpdate(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminContentService.recipeDelete(id);
        return Result.success();
    }

    /** 上架 / 下架 / 审核通过 */
    @PutMapping("/{id}/status")
    public Result<Recipe> status(@PathVariable Long id, @RequestBody StatusUpdateDTO dto) {
        return Result.success(adminContentService.recipeStatus(id, dto.getStatus()));
    }
}
