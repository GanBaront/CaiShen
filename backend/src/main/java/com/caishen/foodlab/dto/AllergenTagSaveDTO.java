package com.caishen.foodlab.dto;

import lombok.Data;

/**
 * 后台过敏源标签新增/编辑入参
 */
@Data
public class AllergenTagSaveDTO {

    private String name;

    private String remark;

    private Integer status;
}
