package com.caishen.foodlab.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员套餐返回
 */
@Data
public class PlanVO {

    /** 免费 / 单次 / 会员 */
    private String code;

    private String name;

    private BigDecimal price;

    /** 计价单位说明 */
    private String unit;

    private String description;

    private List<String> features;

    /** 是否推荐 */
    private Boolean recommended;

    /** 是否是当前方案 */
    private Boolean current;

    /** 按钮文案 */
    private String buttonText;
}
