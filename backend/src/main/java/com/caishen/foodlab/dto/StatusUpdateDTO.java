package com.caishen.foodlab.dto;

import lombok.Data;

/**
 * 状态变更入参（菜谱上架/下架/通过、评论审核）
 */
@Data
public class StatusUpdateDTO {

    /** 菜谱:已上线/待审核/已下架；评论:待审核/已通过/已驳回 */
    private String status;
}
