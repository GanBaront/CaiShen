package com.caishen.foodlab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 大模型返回的菜谱 JSON（严格 JSON，由后端落库）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiRecipeResultDTO {

    private String name;

    private String cuisine;

    private String difficulty;

    private String taste;

    private Integer cookTime;

    private String description;

    private Integer servings;

    private List<Ingredient> ingredients;

    private List<Step> steps;

    private Nutrition nutrition;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ingredient {
        private String name;
        private String amount;
        private String unit;
        private Boolean isMain;
        private Boolean isRequired;
        private List<Substitute> substitutes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Substitute {
        private String name;
        private String ratioDesc;
        private String note;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Step {
        private Integer stepNo;
        private String title;
        private String description;
        private String tips;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nutrition {
        private BigDecimal calories;
        private BigDecimal protein;
        private BigDecimal fat;
        private BigDecimal carbs;
    }
}
