package com.caishen.foodlab.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 会员/单次购买下单入参
 */
@Data
public class MemberOrderDTO {

    /** 会员·1个月 / 单次AI菜谱 */
    @NotBlank(message = "请选择购买类型")
    private String type;
}
