package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.Membership;
import org.apache.ibatis.annotations.Mapper;

/**
 * Membership 数据访问
 */
@Mapper
public interface MembershipMapper extends BaseMapper<Membership> {
}