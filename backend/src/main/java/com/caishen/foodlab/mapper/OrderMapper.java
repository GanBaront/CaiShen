package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order 数据访问
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /** 统计某时间区间内订单总金额（后台统计用） */
    @Select("SELECT IFNULL(SUM(amount), 0) FROM t_orders WHERE create_time >= #{start} AND create_time < #{end}")
    BigDecimal sumAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
