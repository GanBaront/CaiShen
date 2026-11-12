-- =====================================================================
-- 补充商家在售商品（2026-10-27）
-- 背景：t_merchant_product 里「已通过」的只有 6 种调味料，
--       番茄/鸡蛋/豆腐/五花肉/胡萝卜 等生鲜全是「待审核」，
--       导致菜谱「一键下单」生成的订单里大部分食材价格为 0，看着像免费。
-- 本脚本为两家在营生鲜店补充已上架商品，保留原有待审核数据用于后台审核演示。
-- 可重复执行（按 merchant_id + name 去重）。
-- =====================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

-- 邻里鲜蔬铺（merchant 1）
INSERT INTO t_merchant_product (merchant_id, ingredient_id, name, price, sell_unit, stock, audit_status, status)
SELECT 1, i.id, t.name, t.price, t.unit, t.stock, '已通过', 1
FROM (
  SELECT '豆腐'   AS name, 3.80  AS price, '1 块(400g)' AS unit, 120 AS stock UNION ALL
  SELECT '胡萝卜', 2.60, '500g', 150 UNION ALL
  SELECT '五花肉', 18.00, '500g', 60  UNION ALL
  SELECT '生抽',   9.90, '1 瓶(500ml)', 80 UNION ALL
  SELECT '盐',     2.50, '1 袋(400g)', 200 UNION ALL
  SELECT '牛腩',   42.00, '500g', 30  UNION ALL
  SELECT '鸡胸肉', 15.00, '500g', 50  UNION ALL
  SELECT '大米',   25.00, '5kg',  70  UNION ALL
  SELECT '莲藕',   6.50, '500g', 40
) t
JOIN t_ingredient i ON i.name = t.name
WHERE NOT EXISTS (
  SELECT 1 FROM t_merchant_product p WHERE p.merchant_id = 1 AND p.name = t.name
);

-- 家门口菜场（merchant 3），价格略低，便于演示多商家比价
INSERT INTO t_merchant_product (merchant_id, ingredient_id, name, price, sell_unit, stock, audit_status, status)
SELECT 3, i.id, t.name, t.price, t.unit, t.stock, '已通过', 1
FROM (
  SELECT '豆腐'   AS name, 3.50  AS price, '1 块(400g)' AS unit, 100 AS stock UNION ALL
  SELECT '胡萝卜', 2.40, '500g', 130 UNION ALL
  SELECT '五花肉', 17.50, '500g', 50  UNION ALL
  SELECT '生抽',   9.50, '1 瓶(500ml)', 70 UNION ALL
  SELECT '盐',     2.30, '1 袋(400g)', 180 UNION ALL
  SELECT '青椒',   3.00, '500g', 90  UNION ALL
  SELECT '番茄',   4.20, '500g', 90  UNION ALL
  SELECT '鸡蛋',   7.90, '1 盒(10枚)', 60 UNION ALL
  SELECT '土豆',   2.60, '1kg',  110 UNION ALL
  SELECT '洋葱',   2.80, '500g', 80
) t
JOIN t_ingredient i ON i.name = t.name
WHERE NOT EXISTS (
  SELECT 1 FROM t_merchant_product p WHERE p.merchant_id = 3 AND p.name = t.name
);

SELECT audit_status, COUNT(*) AS 商品数 FROM t_merchant_product GROUP BY audit_status;
