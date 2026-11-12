package com.caishen.foodlab.dto;

import lombok.Data;

/**
 * 基本资料修改入参（昵称、手机号、头像）
 */
@Data
public class ProfileUpdateDTO {

    private String nickname;

    private String phone;

    /** 头像图片 URL（由 /api/upload/image 上传后返回） */
    private String avatar;
}
