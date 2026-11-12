package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.dto.MockPayDTO;
import com.caishen.foodlab.entity.BillingRecord;
import com.caishen.foodlab.entity.Membership;
import com.caishen.foodlab.entity.Order;
import com.caishen.foodlab.mapper.BillingRecordMapper;
import com.caishen.foodlab.mapper.MembershipMapper;
import com.caishen.foodlab.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模拟支付：不接真实商户，仅做下单 / 回调的状态流转
 */
@Service
@RequiredArgsConstructor
public class PayService {

    private final BillingRecordMapper billingRecordMapper;
    private final MembershipMapper membershipMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final MemberService memberService;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> mockPay(Long userId, MockPayDTO dto) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("payMethod", "模拟支付");

        // 直接按类型支付：支付成功时才生成购买记录（与开通在同一事务内）
        if (dto.getType() != null && !dto.getType().isBlank()) {
            BillingRecord record = memberService.createPaidRecord(userId, dto.getType().trim());
            if (MemberService.TYPE_MEMBER.equals(record.getType())) {
                grantMembership(userId);
            }
            result.put("type", "billing");
            result.put("id", record.getId());
            result.put("billingType", record.getType());
            result.put("amount", record.getAmount());
            result.put("status", "已支付");
            result.put("message", "模拟支付成功，未产生真实扣款");
            return result;
        }

        if (dto.getBillingRecordId() != null) {
            BillingRecord record = billingRecordMapper.selectById(dto.getBillingRecordId());
            if (record == null || !record.getUserId().equals(userId)) {
                throw new BusinessException("购买记录不存在");
            }
            if ("已支付".equals(record.getStatus())) {
                throw new BusinessException("该订单已支付");
            }
            BillingRecord update = new BillingRecord();
            update.setId(record.getId());
            update.setStatus("已支付");
            billingRecordMapper.updateById(update);
            if (MemberService.TYPE_MEMBER.equals(record.getType())) {
                grantMembership(userId);
            }
            result.put("type", "billing");
            result.put("id", record.getId());
            result.put("billingType", record.getType());
            result.put("amount", record.getAmount());
            result.put("status", "已支付");
            result.put("message", "模拟支付成功，未产生真实扣款");
            return result;
        }

        if (dto.getOrderId() != null) {
            Order order = orderMapper.selectById(dto.getOrderId());
            if (order == null || !order.getUserId().equals(userId)) {
                throw new BusinessException("订单不存在");
            }
            if (!"待支付".equals(order.getStatus())) {
                throw new BusinessException("该订单无需支付");
            }
            orderService.markPaid(order);
            result.put("type", "order");
            result.put("id", order.getId());
            result.put("amount", order.getAmount());
            result.put("status", "已支付");
            result.put("message", "模拟支付成功，未产生真实扣款");
            return result;
        }
        throw new BusinessException("请指定要支付的购买记录或订单");
    }

    /** 开通 / 续费会员：已有有效会员则顺延一个月 */
    private void grantMembership(Long userId) {
        Membership current = membershipMapper.selectOne(new LambdaQueryWrapper<Membership>()
                .eq(Membership::getUserId, userId)
                .eq(Membership::getPlanType, "会员")
                .eq(Membership::getStatus, 1)
                .orderByDesc(Membership::getEndTime)
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (current != null && current.getEndTime() != null && current.getEndTime().isAfter(now)) {
            Membership update = new Membership();
            update.setId(current.getId());
            update.setEndTime(current.getEndTime().plusMonths(1));
            membershipMapper.updateById(update);
            return;
        }
        Membership membership = new Membership();
        membership.setUserId(userId);
        membership.setPlanType("会员");
        membership.setStartTime(now);
        membership.setEndTime(now.plusMonths(1));
        membership.setStatus(1);
        membershipMapper.insert(membership);
    }
}
