package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户饮食偏好
 */
@Data
@TableName("t_user_preference")
public class UserPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 口味偏好(多选,逗号分隔:酸,甜,辣,咸,清淡) */
    private String tastes;

    /** 饮食方式:不限/素食/生酮/低卡 */
    private String dietType;

    /** 默认用餐人数 */
    private Integer defaultServings;

    private LocalDateTime updateTime;
}
