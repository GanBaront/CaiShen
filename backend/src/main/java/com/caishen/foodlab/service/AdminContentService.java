package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.common.PageResult;
import com.caishen.foodlab.dto.AllergenTagSaveDTO;
import com.caishen.foodlab.dto.IngredientSaveDTO;
import com.caishen.foodlab.dto.NutritionSaveDTO;
import com.caishen.foodlab.dto.RecipeSaveDTO;
import com.caishen.foodlab.entity.AllergenTag;
import com.caishen.foodlab.entity.Ingredient;
import com.caishen.foodlab.entity.IngredientAllergen;
import com.caishen.foodlab.entity.IngredientNutrition;
import com.caishen.foodlab.entity.IngredientSubstitute;
import com.caishen.foodlab.entity.Recipe;
import com.caishen.foodlab.entity.RecipeIngredient;
import com.caishen.foodlab.entity.RecipeStep;
import com.caishen.foodlab.mapper.AllergenTagMapper;
import com.caishen.foodlab.mapper.IngredientAllergenMapper;
import com.caishen.foodlab.mapper.IngredientMapper;
import com.caishen.foodlab.mapper.IngredientNutritionMapper;
import com.caishen.foodlab.mapper.IngredientSubstituteMapper;
import com.caishen.foodlab.mapper.RecipeIngredientMapper;
import com.caishen.foodlab.mapper.RecipeMapper;
import com.caishen.foodlab.mapper.RecipeStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台内容管理：菜谱库、食材库、营养数据、过敏源标签
 */
@Service
@RequiredArgsConstructor
public class AdminContentService {

    private static final List<String> RECIPE_STATUS = List.of("已上线", "待审核", "已下架");

    private final RecipeMapper recipeMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final RecipeStepMapper recipeStepMapper;
    private final IngredientSubstituteMapper substituteMapper;
    private final IngredientMapper ingredientMapper;
    private final IngredientNutritionMapper nutritionMapper;
    private final IngredientAllergenMapper ingredientAllergenMapper;
    private final AllergenTagMapper allergenTagMapper;

    // ==================== 菜谱库 ====================

    public PageResult<Recipe> recipePage(long page, long size, String keyword, String status) {
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<Recipe>()
                .like(keyword != null && !keyword.isBlank(), Recipe::getName, keyword)
                .eq(status != null && !status.isBlank(), Recipe::getStatus, status)
                .orderByDesc(Recipe::getId);
        return PageResult.of(recipeMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public Recipe recipeCreate(RecipeSaveDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("请输入菜名");
        }
        Recipe recipe = new Recipe();
        applyRecipe(recipe, dto);
        if (recipe.getCuisine() == null) {
            recipe.setCuisine("家常菜");
        }
        if (recipe.getDifficulty() == null) {
            recipe.setDifficulty("简单");
        }
        if (recipe.getTaste() == null) {
            recipe.setTaste("清淡");
        }
        if (recipe.getCookTime() == null) {
            recipe.setCookTime(15);
        }
        if (recipe.getServings() == null) {
            recipe.setServings(2);
        }
        if (recipe.getSource() == null) {
            recipe.setSource("人工");
        }
        if (recipe.getStatus() == null) {
            recipe.setStatus("已上线");
        }
        recipeMapper.insert(recipe);
        return recipe;
    }

    public Recipe recipeUpdate(Long id, RecipeSaveDTO dto) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new BusinessException("菜谱不存在");
        }
        Recipe update = new Recipe();
        update.setId(id);
        applyRecipe(update, dto);
        if (dto.getStatus() != null && !RECIPE_STATUS.contains(dto.getStatus())) {
            throw new BusinessException("非法的菜谱状态");
        }
        recipeMapper.updateById(update);
        return recipeMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recipeDelete(Long id) {
        List<RecipeIngredient> ingredients = recipeIngredientMapper.selectList(
                new LambdaQueryWrapper<RecipeIngredient>().eq(RecipeIngredient::getRecipeId, id));
        if (!ingredients.isEmpty()) {
            List<Long> riIds = ingredients.stream().map(RecipeIngredient::getId).collect(Collectors.toList());
            substituteMapper.delete(new LambdaQueryWrapper<IngredientSubstitute>()
                    .in(IngredientSubstitute::getRecipeIngredientId, riIds));
            recipeIngredientMapper.delete(new LambdaQueryWrapper<RecipeIngredient>()
                    .eq(RecipeIngredient::getRecipeId, id));
        }
        recipeStepMapper.delete(new LambdaQueryWrapper<RecipeStep>().eq(RecipeStep::getRecipeId, id));
        recipeMapper.deleteById(id);
    }

    /** 上架 / 下架 / 审核通过 */
    public Recipe recipeStatus(Long id, String status) {
        if (status == null || !RECIPE_STATUS.contains(status)) {
            throw new BusinessException("非法的菜谱状态");
        }
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new BusinessException("菜谱不存在");
        }
        Recipe update = new Recipe();
        update.setId(id);
        update.setStatus(status);
        recipeMapper.updateById(update);
        return recipeMapper.selectById(id);
    }

    // ==================== 食材库 ====================

    public PageResult<Ingredient> ingredientPage(long page, long size, String keyword, String category) {
        LambdaQueryWrapper<Ingredient> wrapper = new LambdaQueryWrapper<Ingredient>()
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(Ingredient::getName, keyword).or().like(Ingredient::getAlias, keyword))
                .eq(category != null && !category.isBlank(), Ingredient::getCategory, category)
                .orderByDesc(Ingredient::getId);
        return PageResult.of(ingredientMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public Ingredient ingredientCreate(IngredientSaveDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("请输入食材名");
        }
        Ingredient ingredient = new Ingredient();
        ingredient.setName(dto.getName().trim());
        ingredient.setAlias(dto.getAlias());
        ingredient.setCategory(dto.getCategory() == null || dto.getCategory().isBlank() ? "其他" : dto.getCategory());
        ingredient.setDefaultUnit(dto.getDefaultUnit() == null || dto.getDefaultUnit().isBlank() ? "克" : dto.getDefaultUnit());
        ingredient.setImage(dto.getImage());
        ingredient.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        ingredientMapper.insert(ingredient);
        return ingredient;
    }

    public Ingredient ingredientUpdate(Long id, IngredientSaveDTO dto) {
        if (ingredientMapper.selectById(id) == null) {
            throw new BusinessException("食材不存在");
        }
        Ingredient update = new Ingredient();
        update.setId(id);
        update.setName(dto.getName());
        update.setAlias(dto.getAlias());
        update.setCategory(dto.getCategory());
        update.setDefaultUnit(dto.getDefaultUnit());
        update.setImage(dto.getImage());
        update.setStatus(dto.getStatus());
        ingredientMapper.updateById(update);
        return ingredientMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void ingredientDelete(Long id) {
        nutritionMapper.delete(new LambdaQueryWrapper<IngredientNutrition>()
                .eq(IngredientNutrition::getIngredientId, id));
        ingredientAllergenMapper.delete(new LambdaQueryWrapper<IngredientAllergen>()
                .eq(IngredientAllergen::getIngredientId, id));
        ingredientMapper.deleteById(id);
    }

    // ==================== 营养数据 ====================

    /** 营养数据列表（每 100g，按食材维度展示） */
    public PageResult<Map<String, Object>> nutritionPage(long page, long size, String keyword) {
        Page<Ingredient> ingredientPage = ingredientMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Ingredient>()
                        .like(keyword != null && !keyword.isBlank(), Ingredient::getName, keyword)
                        .orderByAsc(Ingredient::getId));
        List<Map<String, Object>> list = new ArrayList<>();
        if (!ingredientPage.getRecords().isEmpty()) {
            List<Long> ids = ingredientPage.getRecords().stream().map(Ingredient::getId).collect(Collectors.toList());
            Map<Long, IngredientNutrition> nutritionMap = new HashMap<>();
            for (IngredientNutrition nutrition : nutritionMapper.selectList(
                    new LambdaQueryWrapper<IngredientNutrition>().in(IngredientNutrition::getIngredientId, ids))) {
                nutritionMap.put(nutrition.getIngredientId(), nutrition);
            }
            for (Ingredient ingredient : ingredientPage.getRecords()) {
                IngredientNutrition nutrition = nutritionMap.get(ingredient.getId());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ingredientId", ingredient.getId());
                row.put("ingredientName", ingredient.getName());
                row.put("category", ingredient.getCategory());
                row.put("calories", nutrition == null ? BigDecimal.ZERO : nutrition.getCalories());
                row.put("protein", nutrition == null ? BigDecimal.ZERO : nutrition.getProtein());
                row.put("fat", nutrition == null ? BigDecimal.ZERO : nutrition.getFat());
                row.put("carbs", nutrition == null ? BigDecimal.ZERO : nutrition.getCarbs());
                row.put("unitDesc", nutrition == null ? "每100g" : nutrition.getUnitDesc());
                list.add(row);
            }
        }
        return PageResult.of(ingredientPage.getTotal(), ingredientPage.getCurrent(), ingredientPage.getSize(), list);
    }

    public IngredientNutrition nutritionSave(NutritionSaveDTO dto) {
        if (dto.getIngredientId() == null) {
            throw new BusinessException("请选择食材");
        }
        IngredientNutrition existed = nutritionMapper.selectOne(new LambdaQueryWrapper<IngredientNutrition>()
                .eq(IngredientNutrition::getIngredientId, dto.getIngredientId())
                .last("LIMIT 1"));
        IngredientNutrition nutrition = new IngredientNutrition();
        nutrition.setIngredientId(dto.getIngredientId());
        nutrition.setCalories(dto.getCalories() == null ? BigDecimal.ZERO : dto.getCalories());
        nutrition.setProtein(dto.getProtein() == null ? BigDecimal.ZERO : dto.getProtein());
        nutrition.setFat(dto.getFat() == null ? BigDecimal.ZERO : dto.getFat());
        nutrition.setCarbs(dto.getCarbs() == null ? BigDecimal.ZERO : dto.getCarbs());
        nutrition.setUnitDesc(dto.getUnitDesc() == null || dto.getUnitDesc().isBlank() ? "每100g" : dto.getUnitDesc());
        if (existed == null) {
            nutritionMapper.insert(nutrition);
        } else {
            nutrition.setId(existed.getId());
            nutritionMapper.updateById(nutrition);
        }
        return nutritionMapper.selectById(nutrition.getId());
    }

    // ==================== 过敏源标签 ====================

    public List<AllergenTag> allergenTags() {
        return allergenTagMapper.selectList(new LambdaQueryWrapper<AllergenTag>().orderByAsc(AllergenTag::getId));
    }

    public AllergenTag allergenTagCreate(AllergenTagSaveDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("请输入过敏源名称");
        }
        AllergenTag tag = new AllergenTag();
        tag.setName(dto.getName().trim());
        tag.setRemark(dto.getRemark());
        tag.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        allergenTagMapper.insert(tag);
        return tag;
    }

    public AllergenTag allergenTagUpdate(Long id, AllergenTagSaveDTO dto) {
        if (allergenTagMapper.selectById(id) == null) {
            throw new BusinessException("过敏源标签不存在");
        }
        AllergenTag update = new AllergenTag();
        update.setId(id);
        update.setName(dto.getName());
        update.setRemark(dto.getRemark());
        update.setStatus(dto.getStatus());
        allergenTagMapper.updateById(update);
        return allergenTagMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void allergenTagDelete(Long id) {
        ingredientAllergenMapper.delete(new LambdaQueryWrapper<IngredientAllergen>()
                .eq(IngredientAllergen::getAllergenTagId, id));
        allergenTagMapper.deleteById(id);
    }

    private void applyRecipe(Recipe recipe, RecipeSaveDTO dto) {
        recipe.setName(dto.getName() == null ? null : dto.getName().trim());
        recipe.setCoverImage(dto.getCoverImage());
        recipe.setCuisine(dto.getCuisine());
        recipe.setDifficulty(dto.getDifficulty());
        recipe.setTaste(dto.getTaste());
        recipe.setCookTime(dto.getCookTime());
        recipe.setDescription(dto.getDescription());
        recipe.setServings(dto.getServings());
        recipe.setSource(dto.getSource());
        recipe.setStatus(dto.getStatus());
        recipe.setCalories(dto.getCalories());
        recipe.setProtein(dto.getProtein());
        recipe.setFat(dto.getFat());
        recipe.setCarbs(dto.getCarbs());
    }
}
