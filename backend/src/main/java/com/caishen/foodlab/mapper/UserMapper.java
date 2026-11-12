package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * User 数据访问
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}