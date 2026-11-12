package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * OrderItem 数据访问
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}