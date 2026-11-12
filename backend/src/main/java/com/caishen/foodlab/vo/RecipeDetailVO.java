package com.caishen.foodlab.vo;

import com.caishen.foodlab.entity.RecipeStep;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜谱详情返回（所需食材、替代、步骤、营养、相似、匹配度、是否收藏）
 */
@Data
public class RecipeDetailVO {

    private Long id;

    private String name;

    private String coverImage;

    private String cuisine;

    private String difficulty;

    private String taste;

    private Integer cookTime;

    private String description;

    private Integer servings;

    private String source;

    private String status;

    /** 匹配度(%) */
    private Integer matchRate;

    private Boolean favorite = false;

    /** 每份营养 */
    private Nutrition nutrition = new Nutrition();

    private List<RecipeIngredientVO> ingredients = new ArrayList<>();

    private List<RecipeStep> steps = new ArrayList<>();

    /** 还缺的食材（含可替代标记） */
    private List<RecipeIngredientVO> missingIngredients = new ArrayList<>();

    private List<RecipeCardVO> similar = new ArrayList<>();

    @Data
    public static class Nutrition {
        private BigDecimal calories;
        private BigDecimal protein;
        private BigDecimal fat;
        private BigDecimal carbs;
    }
}
