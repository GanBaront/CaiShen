package com.caishen.foodlab.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜谱所需食材返回（含是否已有、替代方案）
 */
@Data
public class RecipeIngredientVO {

    private Long id;

    private Long ingredientId;

    private String name;

    private String amount;

    private String unit;

    private Boolean isMain;

    private Boolean isRequired;

    /** 当前用户是否已有 */
    private Boolean owned;

    private List<Substitute> substitutes = new ArrayList<>();

    @Data
    public static class Substitute {
        private Long id;
        private String name;
        private String ratioDesc;
        private String note;
    }
}
