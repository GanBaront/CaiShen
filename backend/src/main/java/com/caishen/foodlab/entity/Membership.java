package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员状态
 */
@Data
@TableName("t_membership")
public class Membership {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 免费/会员 */
    private String planType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;
}
