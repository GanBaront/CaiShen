-- =====================================================================
-- 兼容升级脚本（2026-10-27）
--
-- 现象：点「一键下单」返回 500 / "系统异常，请稍后重试"，
--       后端日志：Unknown column 'sell_unit' in 'field list'
-- 原因：MerchantProduct 实体有 sellUnit 字段，但旧版建库脚本的
--       t_merchant_product 表里没有 sell_unit 列，查询直接报 SQL 语法错。
--
-- 新库无需执行（一键建库建表sql.txt 已包含该列）。
-- 旧库执行本脚本；若报 1060 Duplicate column name，说明已经加过，忽略即可。
-- =====================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

ALTER TABLE t_merchant_product
  ADD COLUMN sell_unit VARCHAR(32) NOT NULL DEFAULT '1 份' COMMENT '售卖规格（购买单位）' AFTER price;

-- 按品类回填售卖规格，避免订单里显示成"1 份"
UPDATE t_merchant_product SET sell_unit = '1 桶(900ml)'  WHERE name = '食用油';
UPDATE t_merchant_product SET sell_unit = '1 袋(400g)'   WHERE name = '白糖';
UPDATE t_merchant_product SET sell_unit = '1 瓶(500ml)'  WHERE name = '白醋';
UPDATE t_merchant_product SET sell_unit = '1 把(约300g)' WHERE name = '葱';
UPDATE t_merchant_product SET sell_unit = '1 袋(100g)'   WHERE name = '干辣椒';
UPDATE t_merchant_product SET sell_unit = '1 袋(1kg)'    WHERE name = '面粉';
UPDATE t_merchant_product SET sell_unit = '1 盒(10枚)'   WHERE name = '鸡蛋';
UPDATE t_merchant_product SET sell_unit = '1kg'           WHERE name = '土豆';
UPDATE t_merchant_product SET sell_unit = '1 块(400g)'   WHERE name = '豆腐';
UPDATE t_merchant_product SET sell_unit = '500g'
  WHERE name IN ('番茄', '青椒', '五花肉', '胡萝卜', '牛腩', '鸡胸肉', '莲藕', '洋葱');

SELECT name, price, sell_unit FROM t_merchant_product ORDER BY id LIMIT 20;
