package com.caishen.foodlab.dto;

import lombok.Data;

/**
 * 后台食材新增/编辑入参
 */
@Data
public class IngredientSaveDTO {

    private String name;

    private String alias;

    private String category;

    private String defaultUnit;

    private String image;

    private Integer status;
}
