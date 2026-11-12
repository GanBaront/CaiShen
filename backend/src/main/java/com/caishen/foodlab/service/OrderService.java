package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.dto.OrderCreateDTO;
import com.caishen.foodlab.dto.OrderReviewDTO;
import com.caishen.foodlab.entity.Merchant;
import com.caishen.foodlab.entity.MerchantProduct;
import com.caishen.foodlab.entity.Order;
import com.caishen.foodlab.entity.OrderItem;
import com.caishen.foodlab.entity.OrderReview;
import com.caishen.foodlab.entity.Recipe;
import com.caishen.foodlab.entity.RecipeIngredient;
import com.caishen.foodlab.entity.ShoppingListItem;
import com.caishen.foodlab.entity.UserIngredient;
import com.caishen.foodlab.mapper.MerchantMapper;
import com.caishen.foodlab.mapper.MerchantProductMapper;
import com.caishen.foodlab.mapper.OrderItemMapper;
import com.caishen.foodlab.mapper.OrderMapper;
import com.caishen.foodlab.mapper.OrderReviewMapper;
import com.caishen.foodlab.mapper.RecipeIngredientMapper;
import com.caishen.foodlab.mapper.RecipeMapper;
import com.caishen.foodlab.mapper.UserIngredientMapper;
import com.caishen.foodlab.vo.OrderListVO;
import com.caishen.foodlab.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单：下单、我的订单（列表 + 状态计数）、详情、评价
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final List<String> ALL_STATUS = List.of("待支付", "已支付", "配送中", "已完成");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderReviewMapper orderReviewMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantProductMapper merchantProductMapper;
    private final ShoppingListService shoppingListService;
    private final RecipeMapper recipeMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final UserIngredientMapper userIngredientMapper;

    /** 下单（全部加入购物车 → 下单） */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(Long userId, OrderCreateDTO dto) {
        if (dto.getMerchantId() == null) {
            throw new BusinessException("请选择商家");
        }
        Merchant merchant = merchantMapper.selectById(dto.getMerchantId());
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }

        List<OrderItem> items = new ArrayList<>();
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (OrderCreateDTO.Item item : dto.getItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setIngredientId(item.getIngredientId());
                orderItem.setName(item.getName());
                orderItem.setAmount(item.getAmount());
                orderItem.setUnit(item.getUnit());
                orderItem.setPrice(item.getPrice() == null ? BigDecimal.ZERO : item.getPrice());
                items.add(orderItem);
            }
        } else {
            items = fromShoppingList(userId, merchant.getId());
        }
        if (items.isEmpty()) {
            throw new BusinessException("购物清单为空，或该商家暂不提供清单中的食材，请换一家商家");
        }

        BigDecimal amount = items.stream().map(OrderItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setMerchantId(merchant.getId());
        order.setMerchantName(merchant.getName());
        order.setAmount(amount);
        order.setStatus("待支付");
        orderMapper.insert(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }
        return toVO(order, items, false);
    }

    /**
     * 菜谱推荐「一键下单」：
     * 取菜谱中用户还缺的食材，选择覆盖最全、总价最低的商家直接生成待支付订单
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createFromRecipe(Long userId, Long recipeId) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            throw new BusinessException("菜谱不存在或已下架");
        }
        // 与详情页口径一致：AI 现场创作的「待审核」草稿也允许下单，
        // 否则用户在卡片/详情页点「一键下单」会被判成"菜谱不存在"。
        boolean aiDraft = "AI生成".equals(recipe.getSource()) && "待审核".equals(recipe.getStatus());
        if (!"已上线".equals(recipe.getStatus()) && !aiDraft) {
            throw new BusinessException("菜谱不存在或已下架");
        }
        // 还缺的食材 = 菜谱食材 - 用户食材篮已有
        List<RecipeIngredient> ingredients = recipeIngredientMapper.selectList(
                new LambdaQueryWrapper<RecipeIngredient>()
                        .eq(RecipeIngredient::getRecipeId, recipeId)
                        .orderByAsc(RecipeIngredient::getSort));
        Set<String> owned = userIngredientMapper.selectList(new LambdaQueryWrapper<UserIngredient>()
                        .eq(UserIngredient::getUserId, userId))
                .stream().map(UserIngredient::getName).collect(Collectors.toSet());
        List<RecipeIngredient> missing = ingredients.stream()
                .filter(ri -> !owned.contains(ri.getName()))
                .collect(Collectors.toList());
        if (missing.isEmpty()) {
            throw new BusinessException("所需食材都已齐备，无需下单");
        }

        // 商家选择：覆盖缺失食材最多，其次总价最低
        List<Merchant> merchants = merchantMapper.selectList(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getAuditStatus, "已通过")
                .eq(Merchant::getStatus, 1));
        if (merchants.isEmpty()) {
            throw new BusinessException("暂无可下单的商家");
        }
        Merchant bestMerchant = null;
        List<OrderItem> bestItems = null;
        BigDecimal bestTotal = null;
        int bestCovered = 0;
        for (Merchant merchant : merchants) {
            List<MerchantProduct> products = merchantProductMapper.selectList(
                    new LambdaQueryWrapper<MerchantProduct>()
                            .eq(MerchantProduct::getMerchantId, merchant.getId())
                            .eq(MerchantProduct::getAuditStatus, "已通过")
                            .eq(MerchantProduct::getStatus, 1));
            Map<Long, MerchantProduct> byIngredientId = new HashMap<>();
            Map<String, MerchantProduct> byName = new HashMap<>();
            for (MerchantProduct product : products) {
                if (product.getIngredientId() != null) {
                    byIngredientId.putIfAbsent(product.getIngredientId(), product);
                }
                byName.putIfAbsent(product.getName(), product);
            }

            List<OrderItem> items = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            int covered = 0;
            for (RecipeIngredient ri : missing) {
                MerchantProduct product = ri.getIngredientId() != null
                        ? byIngredientId.get(ri.getIngredientId()) : null;
                if (product == null) {
                    product = byName.get(ri.getName());
                }
                // 商家没有报价的食材不放进订单：¥0 明细会让用户以为"免费送"，
                // 实际是这家店不卖。这些食材仍留在"还缺食材"里，可换店或加购物清单。
                if (product == null) {
                    continue;
                }
                OrderItem item = new OrderItem();
                item.setIngredientId(ri.getIngredientId());
                item.setName(ri.getName());
                item.setAmount(ri.getAmount());
                // 下单以商家售卖规格为单位，不再用汤匙/茶匙等烹饪用量
                item.setUnit(product.getSellUnit() == null ? ri.getUnit() : product.getSellUnit());
                item.setPrice(product.getPrice());
                items.add(item);
                covered++;
                total = total.add(product.getPrice());
            }
            if (covered > bestCovered
                    || (covered == bestCovered && bestTotal != null && total.compareTo(bestTotal) < 0)) {
                bestMerchant = merchant;
                bestItems = items;
                bestTotal = total;
                bestCovered = covered;
            }
        }
        if (bestMerchant == null || bestCovered == 0) {
            throw new BusinessException("暂无商家可提供所缺食材");
        }

        Order order = new Order();
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setMerchantId(bestMerchant.getId());
        order.setMerchantName(bestMerchant.getName());
        order.setAmount(bestTotal);
        order.setStatus("待支付");
        orderMapper.insert(order);
        for (OrderItem item : bestItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }
        return toVO(order, bestItems, false);
    }

    /** 用购物清单中未勾选的食材，按商家报价匹配成订单明细 */
    private List<OrderItem> fromShoppingList(Long userId, Long merchantId) {
        List<MerchantProduct> products = merchantProductMapper.selectList(
                new LambdaQueryWrapper<MerchantProduct>()
                        .eq(MerchantProduct::getMerchantId, merchantId)
                        .eq(MerchantProduct::getAuditStatus, "已通过")
                        .eq(MerchantProduct::getStatus, 1));
        Map<String, MerchantProduct> byName = products.stream()
                .collect(Collectors.toMap(MerchantProduct::getName, p -> p, (a, b) -> a));
        Map<Long, MerchantProduct> byId = new HashMap<>();
        for (MerchantProduct product : products) {
            if (product.getIngredientId() != null) {
                byId.putIfAbsent(product.getIngredientId(), product);
            }
        }

        List<OrderItem> items = new ArrayList<>();
        for (ShoppingListItem listItem : shoppingListService.detail(userId).getItems()) {
            if (listItem.getChecked() != null && listItem.getChecked() == 1) {
                continue;
            }
            MerchantProduct product = listItem.getIngredientId() != null ? byId.get(listItem.getIngredientId()) : null;
            if (product == null) {
                product = byName.get(listItem.getName());
            }
            // 该商家没卖这个食材 → 不进订单，避免出现 ¥0 明细（用户会以为免费）
            if (product == null) {
                continue;
            }
            OrderItem item = new OrderItem();
            item.setIngredientId(listItem.getIngredientId());
            item.setName(listItem.getName());
            item.setAmount(listItem.getAmount());
            // 下单以商家售卖规格为单位，不再用清单里的烹饪用量
            item.setUnit(product.getSellUnit() == null ? listItem.getUnit() : product.getSellUnit());
            item.setPrice(product.getPrice());
            items.add(item);
        }
        return items;
    }

    /** 我的订单（列表 + 状态计数） */
    public OrderListVO list(Long userId) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
                .orderByDesc(Order::getId));

        Map<String, Integer> statusCount = new HashMap<>();
        for (String status : ALL_STATUS) {
            statusCount.put(status, 0);
        }
        for (Order order : orders) {
            statusCount.merge(order.getStatus(), 1, Integer::sum);
        }

        List<OrderVO> list = new ArrayList<>();
        for (Order order : orders) {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId()));
            list.add(toVO(order, items, reviewed(order.getId(), userId)));
        }
        OrderListVO vo = new OrderListVO();
        vo.setList(list);
        vo.setStatusCount(statusCount);
        vo.setTotal(orders.size());
        return vo;
    }

    /** 订单详情（查看进度） */
    public OrderVO detail(Long userId, Long orderId) {
        Order order = requireOwn(userId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        return toVO(order, items, reviewed(orderId, userId));
    }

    /** 去评价 */
    @Transactional(rollbackFor = Exception.class)
    public void review(Long userId, Long orderId, OrderReviewDTO dto) {
        Order order = requireOwn(userId, orderId);
        if (!"已完成".equals(order.getStatus())) {
            throw new BusinessException("订单完成后才能评价");
        }
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new BusinessException("请给出 1-5 星评分");
        }
        if (reviewed(orderId, userId)) {
            throw new BusinessException("该订单已评价");
        }
        OrderReview review = new OrderReview();
        review.setOrderId(orderId);
        review.setUserId(userId);
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        orderReviewMapper.insert(review);
    }

    /** 确认收货：已支付 / 配送中 → 已完成（完成后才可评价） */
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Long userId, Long orderId) {
        Order order = requireOwn(userId, orderId);
        String status = order.getStatus();
        if (!"已支付".equals(status) && !"配送中".equals(status)) {
            throw new BusinessException("当前订单状态不支持确认收货");
        }
        Order update = new Order();
        update.setId(orderId);
        update.setStatus("已完成");
        update.setDeliveryDesc("订单已完成，感谢使用");
        orderMapper.updateById(update);
    }

    /** 模拟支付时更新订单状态 */
    public void markPaid(Order order) {
        Order update = new Order();
        update.setId(order.getId());
        update.setStatus("已支付");
        Merchant merchant = order.getMerchantId() == null ? null : merchantMapper.selectById(order.getMerchantId());
        if (merchant != null && merchant.getDeliveryMinutes() != null) {
            update.setDeliveryDesc("预计 " + merchant.getDeliveryMinutes() + " 分钟内送达");
        }
        orderMapper.updateById(update);
    }

    private Order requireOwn(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private boolean reviewed(Long orderId, Long userId) {
        return orderReviewMapper.selectCount(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getOrderId, orderId)
                .eq(OrderReview::getUserId, userId)) > 0;
    }

    private OrderVO toVO(Order order, List<OrderItem> items, boolean reviewed) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setMerchantId(order.getMerchantId());
        vo.setMerchantName(order.getMerchantName());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setDeliveryDesc(order.getDeliveryDesc());
        vo.setCreateTime(order.getCreateTime());
        vo.setReviewed(reviewed);
        vo.setItems(items);
        return vo;
    }

    private String nextOrderNo() {
        return "CS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
