package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.entity.AiGenerationRecord;
import com.caishen.foodlab.entity.Feedback;
import com.caishen.foodlab.entity.Membership;
import com.caishen.foodlab.entity.Merchant;
import com.caishen.foodlab.entity.MerchantProduct;
import com.caishen.foodlab.entity.RecipeReview;
import com.caishen.foodlab.entity.User;
import com.caishen.foodlab.mapper.AiGenerationRecordMapper;
import com.caishen.foodlab.mapper.FeedbackMapper;
import com.caishen.foodlab.mapper.MembershipMapper;
import com.caishen.foodlab.mapper.MerchantMapper;
import com.caishen.foodlab.mapper.MerchantProductMapper;
import com.caishen.foodlab.mapper.OrderMapper;
import com.caishen.foodlab.mapper.RecipeReviewMapper;
import com.caishen.foodlab.mapper.UserMapper;
import com.caishen.foodlab.vo.StatCardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台数据概览：4 张统计卡、AI 生成趋势、待处理审核计数
 * 全部为从库中聚合的真实数值
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    private final UserMapper userMapper;
    private final MembershipMapper membershipMapper;
    private final AiGenerationRecordMapper aiGenerationRecordMapper;
    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantProductMapper merchantProductMapper;
    private final RecipeReviewMapper recipeReviewMapper;
    private final FeedbackMapper feedbackMapper;

    /** 4 张统计卡（数值 + 较上期变化，期 = 近 range 天） */
    public Map<String, Object> overview(int days) {
        LocalDate today = LocalDate.now();
        LocalDateTime thisPeriodStart = today.minusDays(days - 1L).atStartOfDay();
        LocalDateTime lastPeriodStart = today.minusDays(2L * days - 1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        long activeUsers = count(userMapper, new LambdaQueryWrapper<User>().eq(User::getStatus, 1));
        long userThisWeek = count(userMapper, new LambdaQueryWrapper<User>().ge(User::getCreateTime, thisPeriodStart));
        long userLastWeek = count(userMapper, new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, lastPeriodStart).lt(User::getCreateTime, thisPeriodStart));

        long aiTotal = count(aiGenerationRecordMapper, new LambdaQueryWrapper<>());
        long aiThisWeek = count(aiGenerationRecordMapper,
                new LambdaQueryWrapper<AiGenerationRecord>().ge(AiGenerationRecord::getCreateTime, thisPeriodStart));
        long aiLastWeek = count(aiGenerationRecordMapper, new LambdaQueryWrapper<AiGenerationRecord>()
                .ge(AiGenerationRecord::getCreateTime, lastPeriodStart)
                .lt(AiGenerationRecord::getCreateTime, thisPeriodStart));

        long members = count(membershipMapper, new LambdaQueryWrapper<Membership>()
                .eq(Membership::getPlanType, "会员").eq(Membership::getStatus, 1));
        long memberThisWeek = count(membershipMapper, new LambdaQueryWrapper<Membership>()
                .eq(Membership::getPlanType, "会员").ge(Membership::getStartTime, thisPeriodStart));
        long memberLastWeek = count(membershipMapper, new LambdaQueryWrapper<Membership>()
                .eq(Membership::getPlanType, "会员")
                .ge(Membership::getStartTime, lastPeriodStart).lt(Membership::getStartTime, thisPeriodStart));

        BigDecimal amountTotal = orderMapper.sumAmountBetween(LocalDateTime.of(2000, 1, 1, 0, 0), now.plusYears(1));
        BigDecimal amountThisWeek = orderMapper.sumAmountBetween(thisPeriodStart, now);
        BigDecimal amountLastWeek = orderMapper.sumAmountBetween(lastPeriodStart, thisPeriodStart);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeUsers", StatCardVO.of(activeUsers, delta(userThisWeek, userLastWeek)));
        result.put("aiGenerations", StatCardVO.of(aiTotal, delta(aiThisWeek, aiLastWeek)));
        result.put("memberSubscriptions", StatCardVO.of(members, delta(memberThisWeek, memberLastWeek)));
        result.put("orderAmount", new StatCardVO(amountTotal, delta(amountThisWeek, amountLastWeek)));
        return result;
    }

    /** AI 菜谱生成趋势（近 days 天，按天聚合） */
    public Map<String, Object> aiTrend(int days) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(days - 1L).atStartOfDay();
        Map<String, Long> dailyCount = new LinkedHashMap<>();
        for (Map<String, Object> row : aiGenerationRecordMapper.countDailySince(start)) {
            dailyCount.put(String.valueOf(row.get("d")), ((Number) row.get("c")).longValue());
        }
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            labels.add(date.format(DAY_FORMAT));
            values.add(dailyCount.getOrDefault(date.toString(), 0L));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("values", values);
        result.put("unit", "次");
        return result;
    }

    /** 待处理审核计数 */
    public Map<String, Object> pending() {
        long merchantCount = count(merchantMapper, new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getAuditStatus, "待审核"));
        long productCount = count(merchantProductMapper, new LambdaQueryWrapper<MerchantProduct>()
                .eq(MerchantProduct::getAuditStatus, "待审核"));
        long commentCount = count(recipeReviewMapper, new LambdaQueryWrapper<RecipeReview>()
                .eq(RecipeReview::getStatus, "待审核"));
        long feedbackCount = count(feedbackMapper, new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getStatus, "待处理"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("merchantAudit", merchantCount);
        result.put("productAudit", productCount);
        result.put("commentAudit", commentCount);
        result.put("feedback", feedbackCount);
        return result;
    }

    private <T> long count(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
                           LambdaQueryWrapper<T> wrapper) {
        return mapper.selectCount(wrapper);
    }

    private BigDecimal delta(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(current - previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previous), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal delta(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return current != null && current.signum() > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return (current == null ? BigDecimal.ZERO : current).subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }
}
