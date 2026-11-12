package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.entity.Merchant;
import com.caishen.foodlab.entity.MerchantProduct;
import com.caishen.foodlab.entity.ShoppingListItem;
import com.caishen.foodlab.mapper.MerchantMapper;
import com.caishen.foodlab.mapper.MerchantProductMapper;
import com.caishen.foodlab.vo.MerchantCatalogVO;
import com.caishen.foodlab.vo.MerchantCompareVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商家比价：平台内商家比价（综合 / 价格最低 / 好评率 / 距离最近）+ 查看商家明细
 */
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantMapper merchantMapper;
    private final MerchantProductMapper merchantProductMapper;
    private final ShoppingListService shoppingListService;

    /** 平台内商家比价 */
    public List<MerchantCompareVO> compare(Long userId, String sort, List<String> names) {
        List<Item> context = resolveContext(userId, names);
        List<Merchant> merchants = merchantMapper.selectList(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getAuditStatus, "已通过")
                .eq(Merchant::getStatus, 1)
                .orderByAsc(Merchant::getId));

        List<MerchantCompareVO> result = new ArrayList<>();
        for (Merchant merchant : merchants) {
            result.add(buildCompare(merchant, context));
        }
        sortResult(result, sort);
        return result;
    }

    /** 查看商家明细（报价单） */
    public MerchantCompareVO quote(Long merchantId, Long userId, List<String> names) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        return buildCompare(merchant, resolveContext(userId, names));
    }

    /**
     * 商家可购商品总清单：
     * 汇总所有已通过审核、上架中的商品，按商品名聚合，
     * 用户可以看到平台商家一共能买到哪些东西、什么规格、价格区间。
     */
    public List<MerchantCatalogVO> catalog() {
        Map<Long, Merchant> merchants = merchantMapper.selectList(new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getAuditStatus, "已通过")
                        .eq(Merchant::getStatus, 1))
                .stream().collect(Collectors.toMap(Merchant::getId, m -> m, (a, b) -> a));
        if (merchants.isEmpty()) {
            return new ArrayList<>();
        }
        List<MerchantProduct> products = merchantProductMapper.selectList(
                new LambdaQueryWrapper<MerchantProduct>()
                        .eq(MerchantProduct::getAuditStatus, "已通过")
                        .eq(MerchantProduct::getStatus, 1)
                        .in(MerchantProduct::getMerchantId, merchants.keySet()));

        Map<String, MerchantCatalogVO> byName = new LinkedHashMap<>();
        for (MerchantProduct product : products) {
            Merchant merchant = merchants.get(product.getMerchantId());
            if (merchant == null) {
                continue;
            }
            MerchantCatalogVO vo = byName.computeIfAbsent(product.getName(), k -> {
                MerchantCatalogVO created = new MerchantCatalogVO();
                created.setName(k);
                created.setIngredientId(product.getIngredientId());
                created.setSellUnit(product.getSellUnit());
                return created;
            });
            if (vo.getIngredientId() == null) {
                vo.setIngredientId(product.getIngredientId());
            }
            if (vo.getSellUnit() == null && product.getSellUnit() != null) {
                vo.setSellUnit(product.getSellUnit());
            }
            BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
            vo.setMinPrice(vo.getMinPrice() == null ? price : vo.getMinPrice().min(price));
            vo.setMaxPrice(vo.getMaxPrice() == null ? price : vo.getMaxPrice().max(price));
            vo.setMerchantCount(vo.getMerchantCount() + 1);

            MerchantCatalogVO.Offer offer = new MerchantCatalogVO.Offer();
            offer.setMerchantId(merchant.getId());
            offer.setMerchantName(merchant.getName());
            offer.setPrice(price);
            offer.setStock(product.getStock());
            offer.setRating(merchant.getRating());
            vo.getOffers().add(offer);
        }
        List<MerchantCatalogVO> list = new ArrayList<>(byName.values());
        // 在售商家多的排前面，其次按最低价
        list.sort(Comparator.comparing(MerchantCatalogVO::getMerchantCount).reversed()
                .thenComparing(MerchantCatalogVO::getMinPrice, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    private MerchantCompareVO buildCompare(Merchant merchant, List<Item> context) {
        List<MerchantProduct> products = merchantProductMapper.selectList(
                new LambdaQueryWrapper<MerchantProduct>()
                        .eq(MerchantProduct::getMerchantId, merchant.getId())
                        .eq(MerchantProduct::getAuditStatus, "已通过")
                        .eq(MerchantProduct::getStatus, 1));

        Map<String, MerchantProduct> byName = new HashMap<>();
        Map<Long, MerchantProduct> byIngredientId = new HashMap<>();
        for (MerchantProduct product : products) {
            byName.putIfAbsent(product.getName(), product);
            if (product.getIngredientId() != null) {
                byIngredientId.putIfAbsent(product.getIngredientId(), product);
            }
        }

        MerchantCompareVO vo = new MerchantCompareVO();
        vo.setId(merchant.getId());
        vo.setName(merchant.getName());
        vo.setLogo(merchant.getLogo());
        vo.setRating(merchant.getRating());
        vo.setDistanceKm(merchant.getDistanceKm());
        vo.setDeliveryMinutes(merchant.getDeliveryMinutes());
        vo.setTags(merchant.getTags());
        vo.setStockStatus(merchant.getStockStatus());
        vo.setTotalCount(context.size());

        BigDecimal total = BigDecimal.ZERO;
        int covered = 0;
        for (Item item : context) {
            MerchantProduct product = null;
            if (item.ingredientId != null) {
                product = byIngredientId.get(item.ingredientId);
            }
            if (product == null && item.name != null) {
                product = byName.get(item.name);
            }
            if (product == null) {
                vo.getMissingNames().add(item.name);
                continue;
            }
            covered++;
            total = total.add(product.getPrice() == null ? BigDecimal.ZERO : product.getPrice());
            MerchantCompareVO.Item voItem = new MerchantCompareVO.Item();
            voItem.setProductId(product.getId());
            voItem.setIngredientId(product.getIngredientId());
            voItem.setName(product.getName());
            voItem.setAmount(item.amount);
            voItem.setUnit(item.unit);
            voItem.setSellUnit(product.getSellUnit());
            voItem.setPrice(product.getPrice());
            voItem.setStock(product.getStock());
            vo.getItems().add(voItem);
        }
        vo.setTotalPrice(total);
        vo.setCoveredCount(covered);
        vo.setFullCoverage(!context.isEmpty() && covered == context.size());
        return vo;
    }

    /** 比价上下文：优先入参食材名，其次当前用户购物清单中未勾选的食材 */
    private List<Item> resolveContext(Long userId, List<String> names) {
        List<Item> context = new ArrayList<>();
        if (names != null && !names.isEmpty()) {
            Set<String> distinct = new LinkedHashSet<>();
            for (String name : names) {
                if (name != null && !name.isBlank()) {
                    distinct.add(name.trim());
                }
            }
            for (String name : distinct) {
                Item item = new Item();
                item.name = name;
                context.add(item);
            }
            return context;
        }
        if (userId == null) {
            return context;
        }
        List<ShoppingListItem> items = shoppingListService.detail(userId).getItems();
        for (ShoppingListItem item : items) {
            if (item.getChecked() != null && item.getChecked() == 1) {
                continue;
            }
            Item vo = new Item();
            vo.name = item.getName();
            vo.amount = item.getAmount();
            vo.unit = item.getUnit();
            vo.ingredientId = item.getIngredientId();
            context.add(vo);
        }
        return context;
    }

    private void sortResult(List<MerchantCompareVO> result, String sort) {
        String type = sort == null ? "综合" : sort;
        switch (type) {
            case "价格最低" -> result.sort(Comparator.comparing(MerchantCompareVO::getTotalPrice));
            case "好评率" -> result.sort(Comparator.comparing(MerchantCompareVO::getRating).reversed());
            case "距离最近" -> result.sort(Comparator.comparing(MerchantCompareVO::getDistanceKm));
            default -> result.sort(Comparator
                    .comparing((MerchantCompareVO m) -> m.getTags() != null && m.getTags().contains("综合推荐") ? 0 : 1)
                    .thenComparing(Comparator.comparing(MerchantCompareVO::getRating).reversed()));
        }
    }

    private static class Item {
        private Long ingredientId;
        private String name;
        private String amount;
        private String unit;
    }
}
