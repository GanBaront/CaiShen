package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户忌口/过敏源
 */
@Data
@TableName("t_user_avoid_item")
public class UserAvoidItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 1忌口食材 2过敏源 */
    private Integer type;

    private String name;

    private LocalDateTime createTime;
}
