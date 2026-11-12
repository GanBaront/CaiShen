package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台管理员
 */
@Data
@TableName("t_admin_user")
public class AdminUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 密码(BCrypt) */
    private String password;

    private String realName;

    private Integer status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;
}
