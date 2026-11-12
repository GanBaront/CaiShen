package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.common.PageResult;
import com.caishen.foodlab.dto.UserAdminUpdateDTO;
import com.caishen.foodlab.entity.Membership;
import com.caishen.foodlab.entity.Merchant;
import com.caishen.foodlab.entity.MerchantProduct;
import com.caishen.foodlab.entity.Order;
import com.caishen.foodlab.entity.OrderItem;
import com.caishen.foodlab.entity.User;
import com.caishen.foodlab.mapper.MembershipMapper;
import com.caishen.foodlab.mapper.MerchantMapper;
import com.caishen.foodlab.mapper.MerchantProductMapper;
import com.caishen.foodlab.mapper.OrderItemMapper;
import com.caishen.foodlab.mapper.OrderMapper;
import com.caishen.foodlab.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台用户与交易：用户管理、会员与订单、商家审核、商品审核
 */
@Service
@RequiredArgsConstructor
public class AdminTradeService {

    private static final List<String> AUDIT_STATUS = List.of("待审核", "已通过", "已驳回");

    private final UserMapper userMapper;
    private final MembershipMapper membershipMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantProductMapper merchantProductMapper;

    // ==================== 用户管理 ====================

    public PageResult<Map<String, Object>> userPage(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(User::getAccount, keyword).or().like(User::getNickname, keyword))
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getId);
        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = userPage.getRecords().stream().map(user -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", user.getId());
            row.put("account", user.getAccount());
            row.put("nickname", user.getNickname());
            row.put("phone", user.getPhone());
            row.put("avatar", user.getAvatar());
            row.put("status", user.getStatus());
            row.put("createTime", user.getCreateTime());
            return row;
        }).collect(Collectors.toList());
        return PageResult.of(userPage.getTotal(), userPage.getCurrent(), userPage.getSize(), list);
    }

    public User userUpdate(Long id, UserAdminUpdateDTO dto) {
        if (userMapper.selectById(id) == null) {
            throw new BusinessException("用户不存在");
        }
        User update = new User();
        update.setId(id);
        update.setNickname(dto.getNickname());
        update.setPhone(dto.getPhone());
        update.setStatus(dto.getStatus());
        userMapper.updateById(update);
        return userMapper.selectById(id);
    }

    // ==================== 会员与订单 ====================

    public PageResult<Map<String, Object>> orderPage(long page, long size, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(status != null && !status.isBlank(), Order::getStatus, status)
                .orderByDesc(Order::getCreateTime)
                .orderByDesc(Order::getId);
        Page<Order> orderPage = orderMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, User> userMap = new HashMap<>();
        if (!orderPage.getRecords().isEmpty()) {
            List<Long> userIds = orderPage.getRecords().stream().map(Order::getUserId)
                    .distinct().collect(Collectors.toList());
            for (User user : userMapper.selectBatchIds(userIds)) {
                userMap.put(user.getId(), user);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Order order : orderPage.getRecords()) {
            User user = userMap.get(order.getUserId());
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId()));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", order.getId());
            row.put("orderNo", order.getOrderNo());
            row.put("userId", order.getUserId());
            row.put("account", user == null ? null : user.getAccount());
            row.put("nickname", user == null ? null : user.getNickname());
            row.put("merchantName", order.getMerchantName());
            row.put("amount", order.getAmount());
            row.put("status", order.getStatus());
            row.put("deliveryDesc", order.getDeliveryDesc());
            row.put("createTime", order.getCreateTime());
            row.put("items", items);
            list.add(row);
        }
        return PageResult.of(orderPage.getTotal(), orderPage.getCurrent(), orderPage.getSize(), list);
    }

    public PageResult<Map<String, Object>> memberPage(long page, long size) {
        LambdaQueryWrapper<Membership> wrapper = new LambdaQueryWrapper<Membership>()
                .eq(Membership::getPlanType, "会员")
                .orderByDesc(Membership::getId);
        Page<Membership> membershipPage = membershipMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, User> userMap = new HashMap<>();
        if (!membershipPage.getRecords().isEmpty()) {
            List<Long> userIds = membershipPage.getRecords().stream().map(Membership::getUserId)
                    .distinct().collect(Collectors.toList());
            for (User user : userMapper.selectBatchIds(userIds)) {
                userMap.put(user.getId(), user);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Membership membership : membershipPage.getRecords()) {
            User user = userMap.get(membership.getUserId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", membership.getId());
            row.put("userId", membership.getUserId());
            row.put("account", user == null ? null : user.getAccount());
            row.put("nickname", user == null ? null : user.getNickname());
            row.put("planType", membership.getPlanType());
            row.put("startTime", membership.getStartTime());
            row.put("endTime", membership.getEndTime());
            row.put("status", membership.getStatus());
            list.add(row);
        }
        return PageResult.of(membershipPage.getTotal(), membershipPage.getCurrent(), membershipPage.getSize(), list);
    }

    // ==================== 商家审核 ====================

    public PageResult<Merchant> merchantPage(long page, long size, String auditStatus, String keyword) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .eq(auditStatus != null && !auditStatus.isBlank(), Merchant::getAuditStatus, auditStatus)
                .like(keyword != null && !keyword.isBlank(), Merchant::getName, keyword)
                .orderByDesc(Merchant::getId);
        return PageResult.of(merchantMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public Merchant merchantAudit(Long id, String auditStatus) {
        if (!AUDIT_STATUS.contains(auditStatus)) {
            throw new BusinessException("非法的审核状态");
        }
        if (merchantMapper.selectById(id) == null) {
            throw new BusinessException("商家不存在");
        }
        Merchant update = new Merchant();
        update.setId(id);
        update.setAuditStatus(auditStatus);
        merchantMapper.updateById(update);
        return merchantMapper.selectById(id);
    }

    // ==================== 商品审核 ====================

    public PageResult<Map<String, Object>> productPage(long page, long size, String auditStatus) {
        LambdaQueryWrapper<MerchantProduct> wrapper = new LambdaQueryWrapper<MerchantProduct>()
                .eq(auditStatus != null && !auditStatus.isBlank(), MerchantProduct::getAuditStatus, auditStatus)
                .orderByDesc(MerchantProduct::getId);
        Page<MerchantProduct> productPage = merchantProductMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, Merchant> merchantMap = new HashMap<>();
        if (!productPage.getRecords().isEmpty()) {
            List<Long> merchantIds = productPage.getRecords().stream().map(MerchantProduct::getMerchantId)
                    .distinct().collect(Collectors.toList());
            for (Merchant merchant : merchantMapper.selectBatchIds(merchantIds)) {
                merchantMap.put(merchant.getId(), merchant);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (MerchantProduct product : productPage.getRecords()) {
            Merchant merchant = merchantMap.get(product.getMerchantId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", product.getId());
            row.put("merchantId", product.getMerchantId());
            row.put("merchantName", merchant == null ? null : merchant.getName());
            row.put("name", product.getName());
            row.put("price", product.getPrice());
            row.put("stock", product.getStock());
            row.put("auditStatus", product.getAuditStatus());
            list.add(row);
        }
        return PageResult.of(productPage.getTotal(), productPage.getCurrent(), productPage.getSize(), list);
    }

    public MerchantProduct productAudit(Long id, String auditStatus) {
        if (!AUDIT_STATUS.contains(auditStatus)) {
            throw new BusinessException("非法的审核状态");
        }
        if (merchantProductMapper.selectById(id) == null) {
            throw new BusinessException("商品不存在");
        }
        MerchantProduct update = new MerchantProduct();
        update.setId(id);
        update.setAuditStatus(auditStatus);
        merchantProductMapper.updateById(update);
        return merchantProductMapper.selectById(id);
    }
}
