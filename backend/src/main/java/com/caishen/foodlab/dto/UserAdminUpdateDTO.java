package com.caishen.foodlab.dto;

import lombok.Data;

/**
 * 后台用户修改入参
 */
@Data
public class UserAdminUpdateDTO {

    private String nickname;

    private String phone;

    private Integer status;
}
