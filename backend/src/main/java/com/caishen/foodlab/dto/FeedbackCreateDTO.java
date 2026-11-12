package com.caishen.foodlab.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 意见反馈入参
 */
@Data
public class FeedbackCreateDTO {

    @NotBlank(message = "请输入反馈内容")
    private String content;

    private String contact;
}
