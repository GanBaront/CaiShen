package com.caishen.foodlab.dto;

import lombok.Data;

/**
 * 审核入参（商家入驻审核 / 商品上架审核）
 */
@Data
public class AuditDTO {

    /** 待审核/已通过/已驳回 */
    private String auditStatus;
}
