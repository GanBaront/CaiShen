package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.config.AppProperties;
import com.caishen.foodlab.dto.MemberOrderDTO;
import com.caishen.foodlab.entity.BillingRecord;
import com.caishen.foodlab.entity.Membership;
import com.caishen.foodlab.mapper.BillingRecordMapper;
import com.caishen.foodlab.mapper.MembershipMapper;
import com.caishen.foodlab.vo.FreeQuotaVO;
import com.caishen.foodlab.vo.MemberStatusVO;
import com.caishen.foodlab.vo.PlanVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 会员中心：当前身份、套餐、下单（购买记录）、订阅与购买记录
 * 支付统一为模拟支付
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    public static final String TYPE_MEMBER = "会员·1个月";
    public static final String TYPE_SINGLE = "单次AI菜谱";

    private final MembershipMapper membershipMapper;
    private final BillingRecordMapper billingRecordMapper;
    private final FreeQuotaService freeQuotaService;
    private final AppProperties appProperties;

    /** 当前身份 + 今日剩余次数 */
    public MemberStatusVO status(Long userId) {
        Membership membership = freeQuotaService.currentMembership(userId);
        MemberStatusVO vo = new MemberStatusVO();
        vo.setMember(membership != null);
        vo.setPlanType(membership == null ? "免费" : "会员");
        if (membership != null) {
            vo.setStartTime(membership.getStartTime());
            vo.setEndTime(membership.getEndTime());
        }
        FreeQuotaVO quota = freeQuotaService.quota(userId);
        vo.setLimit(quota.getLimit());
        vo.setUsed(quota.getUsed());
        vo.setRemaining(quota.getRemaining());
        return vo;
    }

    /** 三个套餐（免费 / 单次体验 / 会员） */
    public List<PlanVO> plans(Long userId) {
        boolean member = freeQuotaService.isMember(userId);
        List<PlanVO> plans = new ArrayList<>();

        PlanVO free = new PlanVO();
        free.setCode("免费");
        free.setName("免费");
        free.setPrice(BigDecimal.ZERO);
        free.setUnit("");
        free.setFeatures(List.of("文字 / 语音 / 图片上传识别食材", "个性化筛选与食材替代",
                "分步烹饪教学", "购物清单", "每周 " + appProperties.getFreeDailyLimit() + " 次免费使用"));
        free.setRecommended(false);
        free.setCurrent(!member);
        // 免费方案无需开通，按钮引导用户去使用基础功能
        free.setButtonText("前往使用");
        plans.add(free);

        PlanVO single = new PlanVO();
        single.setCode("单次");
        single.setName("单次体验");
        single.setPrice(appProperties.getAiSinglePrice());
        single.setUnit("/ 次");
        single.setDescription("生成 1 道 AI 新菜谱，无需订阅，用完即止");
        single.setFeatures(List.of("生成 1 道 AI 新菜谱", "无需订阅，用完即止"));
        single.setRecommended(false);
        single.setCurrent(false);
        single.setButtonText("购买 1 次");
        plans.add(single);

        PlanVO memberPlan = new PlanVO();
        memberPlan.setCode("会员");
        memberPlan.setName("会员");
        memberPlan.setPrice(appProperties.getMemberMonthPrice());
        memberPlan.setUnit("/ 月");
        memberPlan.setDescription("基础功能每周免费 " + appProperties.getFreeDailyLimit() + " 次。会员解锁 AI 生成的新颖菜谱。");
        memberPlan.setFeatures(List.of("包含免费方案全部功能", "AI 生成的新颖菜谱",
                "更多高级功能", "到期提醒，支持续费"));
        memberPlan.setRecommended(true);
        memberPlan.setCurrent(member);
        memberPlan.setButtonText(member ? "续费" : "立即开通");
        plans.add(memberPlan);
        return plans;
    }

    /** 创建会员 / 单次订单（生成待支付的购买记录） */
    public BillingRecord createOrder(Long userId, MemberOrderDTO dto) {
        String type = dto.getType();
        BigDecimal amount;
        if (TYPE_MEMBER.equals(type)) {
            amount = appProperties.getMemberMonthPrice();
        } else if (TYPE_SINGLE.equals(type)) {
            amount = appProperties.getAiSinglePrice();
        } else {
            throw new BusinessException("不支持的购买类型");
        }
        BillingRecord record = new BillingRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setAmount(amount);
        record.setPayMethod("模拟支付");
        record.setStatus("待支付");
        billingRecordMapper.insert(record);
        return record;
    }

    /** 按类型直接生成已支付的购买记录（仅在支付成功时调用，避免产生待支付垃圾记录） */
    public BillingRecord createPaidRecord(Long userId, String type) {
        BigDecimal amount;
        if (TYPE_MEMBER.equals(type)) {
            amount = appProperties.getMemberMonthPrice();
        } else if (TYPE_SINGLE.equals(type)) {
            amount = appProperties.getAiSinglePrice();
        } else {
            throw new BusinessException("不支持的购买类型");
        }
        BillingRecord record = new BillingRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setAmount(amount);
        record.setPayMethod("模拟支付");
        record.setStatus("已支付");
        billingRecordMapper.insert(record);
        return record;
    }

    /** 订阅与购买记录 */
    public List<BillingRecord> billingRecords(Long userId) {
        return billingRecordMapper.selectList(new LambdaQueryWrapper<BillingRecord>()
                .eq(BillingRecord::getUserId, userId)
                .orderByDesc(BillingRecord::getCreateTime)
                .orderByDesc(BillingRecord::getId));
    }
}
