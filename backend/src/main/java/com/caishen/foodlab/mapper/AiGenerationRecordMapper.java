package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.AiGenerationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AiGenerationRecord 数据访问
 */
@Mapper
public interface AiGenerationRecordMapper extends BaseMapper<AiGenerationRecord> {

    /** 统计某用户某来源在某时间之后的生成次数 */
    @Select("SELECT COUNT(*) FROM t_ai_generation_record WHERE user_id = #{userId} AND source = #{source} AND create_time >= #{start}")
    long countByUserAndSourceSince(@Param("userId") Long userId,
                                   @Param("source") String source,
                                   @Param("start") LocalDateTime start);

    /** 近 N 天按天聚合生成次数（后台趋势图用） */
    @Select("SELECT DATE(create_time) AS d, COUNT(*) AS c FROM t_ai_generation_record WHERE create_time >= #{start} GROUP BY DATE(create_time)")
    List<Map<String, Object>> countDailySince(@Param("start") LocalDateTime start);
}
