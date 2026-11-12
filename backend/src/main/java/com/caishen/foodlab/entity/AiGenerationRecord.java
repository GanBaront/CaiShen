package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 菜谱生成记录
 */
@Data
@TableName("t_ai_generation_record")
public class AiGenerationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 首页生成/会员AI新菜谱 */
    private String source;

    /** 输入食材 */
    private String inputIngredients;

    /** 生成的菜谱 */
    private Long recipeId;

    private LocalDateTime createTime;
}
