package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.config.AppProperties;
import com.caishen.foodlab.entity.AiGenerationRecord;
import com.caishen.foodlab.entity.BillingRecord;
import com.caishen.foodlab.entity.Membership;
import com.caishen.foodlab.mapper.AiGenerationRecordMapper;
import com.caishen.foodlab.mapper.BillingRecordMapper;
import com.caishen.foodlab.mapper.MembershipMapper;
import com.caishen.foodlab.vo.FreeQuotaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 使用额度：非会员每个自然周（周一 0 点起）5 次免费，会员不限次。
 * 每周免费次数用完后，已支付的「单次AI菜谱」余额可继续使用（首页生成与 AI 新菜谱共用同一单次余额）。
 * 以库中生成记录 + 内存增量计数为准，定时任务兜底清理内存计数。
 */
@Service
@RequiredArgsConstructor
public class FreeQuotaService {

    /** 首页生成来源标识（消耗每周免费额度） */
    public static final String SOURCE_HOME = "首页生成";
    /** 首页生成来源标识（免费额度用尽后消耗「单次」余额） */
    public static final String SOURCE_HOME_SINGLE = "首页生成-单次";
    /** 会员 AI 新菜谱来源标识（消耗「单次」余额 / 会员不限次） */
    public static final String SOURCE_MEMBER_AI = "会员AI新菜谱";

    private final AiGenerationRecordMapper aiGenerationRecordMapper;
    private final BillingRecordMapper billingRecordMapper;
    private final MembershipMapper membershipMapper;
    private final AppProperties appProperties;

    /** 当周期（本周）增量计数（定时任务兜底清理） */
    private final Map<Long, Integer> todayUsed = new ConcurrentHashMap<>();

    /** 是否有效会员 */
    public boolean isMember(Long userId) {
        if (userId == null) {
            return false;
        }
        LambdaQueryWrapper<Membership> wrapper = new LambdaQueryWrapper<Membership>()
                .eq(Membership::getUserId, userId)
                .eq(Membership::getPlanType, "会员")
                .eq(Membership::getStatus, 1)
                .ge(Membership::getEndTime, LocalDateTime.now());
        return membershipMapper.selectCount(wrapper) > 0;
    }

    /** 当前有效会员记录 */
    public Membership currentMembership(Long userId) {
        if (userId == null) {
            return null;
        }
        LambdaQueryWrapper<Membership> wrapper = new LambdaQueryWrapper<Membership>()
                .eq(Membership::getUserId, userId)
                .eq(Membership::getPlanType, "会员")
                .eq(Membership::getStatus, 1)
                .ge(Membership::getEndTime, LocalDateTime.now())
                .orderByDesc(Membership::getEndTime)
                .last("LIMIT 1");
        return membershipMapper.selectOne(wrapper);
    }

    /** 每周上限（自然周：周一 0 点 ~ 周日 24 点） */
    public int weeklyLimit() {
        return appProperties.getFreeDailyLimit();
    }

    /** 本周期（本周）起始时间：周一 0 点 */
    private LocalDateTime weekStart() {
        return LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
    }

    /** 本周已用免费次数（首页生成） */
    public int usedThisWeek(Long userId) {
        if (userId == null) {
            return 0;
        }
        long dbCount = aiGenerationRecordMapper.countByUserAndSourceSince(userId, SOURCE_HOME, weekStart());
        int cache = todayUsed.getOrDefault(userId, 0);
        return (int) Math.max(dbCount, cache);
    }

    /** 已支付的「单次AI菜谱」次数 */
    public int singlePaid(Long userId) {
        if (userId == null) {
            return 0;
        }
        long count = billingRecordMapper.selectCount(new LambdaQueryWrapper<BillingRecord>()
                .eq(BillingRecord::getUserId, userId)
                .eq(BillingRecord::getType, MemberService.TYPE_SINGLE)
                .eq(BillingRecord::getStatus, "已支付"));
        return (int) count;
    }

    /** 已消耗的单次次数（AI 新菜谱 + 首页单次路径，统一口径防止重复消费） */
    public int singleUsed(Long userId) {
        if (userId == null) {
            return 0;
        }
        Long memberAi = aiGenerationRecordMapper.selectCount(new LambdaQueryWrapper<AiGenerationRecord>()
                .eq(AiGenerationRecord::getUserId, userId)
                .eq(AiGenerationRecord::getSource, SOURCE_MEMBER_AI));
        Long homeSingle = aiGenerationRecordMapper.selectCount(new LambdaQueryWrapper<AiGenerationRecord>()
                .eq(AiGenerationRecord::getUserId, userId)
                .eq(AiGenerationRecord::getSource, SOURCE_HOME_SINGLE));
        return (int) (memberAi + homeSingle);
    }

    /** 未使用的单次余额 */
    public int singleRemaining(Long userId) {
        return Math.max(0, singlePaid(userId) - singleUsed(userId));
    }

    public FreeQuotaVO quota(Long userId) {
        FreeQuotaVO vo = new FreeQuotaVO();
        vo.setMember(isMember(userId));
        vo.setLimit(weeklyLimit());
        int used = usedThisWeek(userId);
        vo.setUsed(used);
        // 非会员剩余 = 每周免费剩余 + 未使用的单次余额（购买单次后立即生效）
        int freeRemaining = Math.max(0, weeklyLimit() - used);
        vo.setRemaining(vo.getMember() ? Integer.MAX_VALUE : freeRemaining + singleRemaining(userId));
        return vo;
    }

    /**
     * 校验并消耗一次额度（会员不限次；非会员优先消耗每周免费，用尽后消耗单次余额）。
     * @return true 表示本次消耗的是「单次」余额（生成记录应以 SOURCE_HOME_SINGLE 落库）
     */
    public boolean consume(Long userId) {
        if (isMember(userId)) {
            return false;
        }
        if (usedThisWeek(userId) < weeklyLimit()) {
            todayUsed.merge(userId, 1, Integer::sum);
            return false;
        }
        if (singleRemaining(userId) <= 0) {
            throw new BusinessException("本周免费次数已用完，开通会员可不限次使用，或购买单次体验");
        }
        // 单次消耗以生成记录落库为准（见调用方按来源落库），此处不做内存计数
        return true;
    }

    /** 兜底清理内存计数（数据库按周统计为准；周一 0 点的每日重置同时覆盖周边界） */
    public void resetDaily() {
        todayUsed.clear();
    }
}
