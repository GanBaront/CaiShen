package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * Feedback 数据访问
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
}