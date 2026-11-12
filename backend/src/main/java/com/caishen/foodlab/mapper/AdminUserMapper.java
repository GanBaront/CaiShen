package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * AdminUser 数据访问
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}