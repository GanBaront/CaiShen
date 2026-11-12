-- =====================================================================
-- 补充菜谱种子数据（2026-10-27）
-- 背景：原库仅 12 道在线菜谱，且全部集中在 番茄/鸡蛋/土豆/青椒，
--       用户用「豆腐、胡萝卜、五花肉」生成时命中数为 0，页面只能显示"0 道菜谱"。
-- 本脚本补充 12 道家常菜，覆盖 豆腐/胡萝卜/五花肉/洋葱/大米/牛腩/鸡胸肉 等缺口。
-- 可重复执行：先按菜名删除（含子表），再重新插入。
-- =====================================================================

-- 表的排序规则是 utf8mb4_general_ci，会话变量必须同collation，
-- 否则 FIND_IN_SET 会报 "Illegal mix of collations"
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

SET @names := '麻婆豆腐,家常豆腐,红烧五花肉,五花肉炒胡萝卜,胡萝卜炒蛋,洋葱炒蛋,糖醋豆腐,番茄豆腐汤,胡萝卜土豆丝,土豆胡萝卜炖牛腩,黑椒煎鸡胸,家常蛋炒饭';

-- 幂等清理：先删子表，再删主表
DELETE ri FROM t_recipe_ingredient ri JOIN t_recipe r ON r.id = ri.recipe_id
  WHERE FIND_IN_SET(r.name, @names) > 0;
DELETE rs FROM t_recipe_step rs JOIN t_recipe r ON r.id = rs.recipe_id
  WHERE FIND_IN_SET(r.name, @names) > 0;
DELETE FROM t_recipe WHERE FIND_IN_SET(name, @names) > 0;

-- ---------------------------------------------------------------- 1
INSERT INTO t_recipe (name, cuisine, difficulty, taste, cook_time, description, servings, source, status, calories, protein, fat, carbs)
VALUES ('麻婆豆腐', '家常菜', '简单', '麻辣', 20,
        '豆腐嫩滑、肉末酥香，一勺豆瓣香辣汁拌饭能吃两碗，全程 20 分钟。', 2, '人工', '已上线', 268, 16.4, 17.2, 9.1);
SET @rid = LAST_INSERT_ID();
INSERT INTO t_recipe_ingredient (recipe_id, ingredient_id, name, amount, unit, is_main, is_required, sort) VALUES
(@rid, (SELECT id FROM t_ingredient WHERE name = '豆腐' LIMIT 1),   '豆腐',   '400', '克', 1, 1, 1),
(@rid, (SELECT id FROM t_ingredient WHERE name = '五花肉' LIMIT 1), '五花肉', '80',  '克', 1, 0, 2),
(@rid, (SELECT id FROM t_ingredient WHERE name = '干辣椒' LIMIT 1), '干辣椒', '3',   '个', 0, 0, 3),
(@rid, (SELECT id FROM t_ingredient WHERE name = '生抽' LIMIT 1),   '生抽',   '1',   '勺', 0, 1, 4),
(@rid, (SELECT id FROM t_ingredient WHERE name = '葱花' LIMIT 1),   '葱花',   '少许', NULL, 0, 0, 5),
(@rid, (SELECT id FROM t_ingredient WHERE name = '盐' LIMIT 1),     '盐',     '2',   '克', 0, 1, 6);
INSERT INTO t_recipe_step (recipe_id, step_no, title, description, tips, sort) VALUES
(@rid, 1, '豆腐切块焯水', '豆腐切 2 厘米方块，放入淡盐水中焯 1 分钟捞出沥干。', '淡盐水焯过不易碎，还能去豆腥味', 1),
(@rid, 2, '炒肉末出香', '五花肉切末下锅中火煸出油，加干辣椒段炒香，加生抽上色。', '肉末要煸到微微金黄，香味才出得来', 2),
(@rid, 3, '下豆腐烧入味', '加半碗热水煮开，下豆腐块小火烧 5 分钟，轻推翻动不要铲。', '全程中小火，推动锅身代替翻炒，豆腐才完整', 3),
(@rid, 4, '勾薄芡装盘', '淋少许水淀粉收汁，撒葱花与盐调味即可。', '芡要薄，能挂住豆腐表面就够', 4);

-- ---------------------------------------------------------------- 2
INSERT INTO t_recipe (name, cuisine, difficulty, taste, cook_time, description, servings, source, status, calories, protein, fat, carbs)
VALUES ('家常豆腐', '家常菜', '简单', '咸香', 18,
        '豆腐两面煎出焦壳，再和五花肉、青椒一起烧，外韧内嫩咸香下饭。', 2, '人工', '已上线', 302, 17.1, 21.5, 8.4);
SET @rid = LAST_INSERT_ID();
INSERT INTO t_recipe_ingredient (recipe_id, ingredient_id, name, amount, unit, is_main, is_required, sort) VALUES
(@rid, (SELECT id FROM t_ingredient WHERE name = '豆腐' LIMIT 1),   '豆腐',   '400', '克', 1, 1, 1),
(@rid, (SELECT id FROM t_ingredient WHERE name = '五花肉' LIMIT 1), '五花肉', '100', '克', 1, 0, 2),
(@rid, (SELECT id FROM t_ingredient WHERE name = '青椒' LIMIT 1),   '青椒',   '1',   '个', 0, 0, 3),
(@rid, (SELECT id FROM t_ingredient WHERE name = '生抽' LIMIT 1),   '生抽',   '1',   '勺', 0, 1, 4),
(@rid, (SELECT id FROM t_ingredient WHERE name = '食用油' LIMIT 1), '食用油', '2',   '勺', 0, 1, 5),
(@rid, (SELECT id FROM t_ingredient WHERE name = '盐' LIMIT 1),     '盐',     '2',   '克', 0, 1, 6);
INSERT INTO t_recipe_step (recipe_id, step_no, title, description, tips, sort) VALUES
(@rid, 1, '豆腐煎香', '豆腐切厚片，厨房纸吸干水分，热油下锅两面煎至金黄盛出。', '吸干水分才不粘锅、不溅油', 1),
(@rid, 2, '煸五花肉', '五花肉切薄片下锅煸出油，边缘卷起时加生抽炒匀。', '用煸出的猪油烧豆腐最香', 2),
(@rid, 3, '合烧入味', '下青椒片翻炒，倒回豆腐，加半碗水烧 3 分钟收汁，加盐调味。', '豆腐最后放，避免翻碎', 3);

-- ---------------------------------------------------------------- 3
INSERT INTO t_recipe (name, cuisine, difficulty, taste, cook_time, description, servings, source, status, calories, protein, fat, carbs)
VALUES ('红烧五花肉', '家常菜', '中等', '咸甜', 55,
        '只用糖和生抽炒出红亮糖色，肥而不腻入口即化，冷天最下饭的一道硬菜。', 3, '人工', '已上线', 486, 18.2, 42.6, 11.3);
SET @rid = LAST_INSERT_ID();
INSERT INTO t_recipe_ingredient (recipe_id, ingredient_id, name, amount, unit, is_main, is_required, sort) VALUES
(@rid, (SELECT id FROM t_ingredient WHERE name = '五花肉' LIMIT 1), '五花肉', '600', '克', 1, 1, 1),
(@rid, (SELECT id FROM t_ingredient WHERE name = '白糖' LIMIT 1),   '白糖',   '30',  '克', 0, 1, 2),
(@rid, (SELECT id FROM t_ingredient WHERE name = '生抽' LIMIT 1),   '生抽',   '3',   '勺', 0, 1, 3),
(@rid, (SELECT id FROM t_ingredient WHERE name = '葱' LIMIT 1),     '葱',     '2',   '段', 0, 0, 4),
(@rid, (SELECT id FROM t_ingredient WHERE name = '盐' LIMIT 1),     '盐',     '2',   '克', 0, 0, 5);
INSERT INTO t_recipe_step (recipe_id, step_no, title, description, tips, sort) VALUES
(@rid, 1, '切块焯水', '五花肉切 3 厘米见方块，冷水下锅煮开撇净浮沫，捞出冲净。', '冷水下锅血沫才出得干净', 1),
(@rid, 2, '炒糖色', '锅中少油下白糖，小火熬到枣红色起小泡，立刻下肉块翻炒上色。', '糖色一到枣红就要下肉，再熬会发苦', 2),
(@rid, 3, '加水慢炖', '加生抽、葱段和没过肉的热水，大火烧开转小火炖 40 分钟。', '一定加热水，冷水会让肉质变紧', 3),
(@rid, 4, '大火收汁', '开盖转大火收至汤汁浓稠挂肉，尝味补盐。', '收汁时要翻动，避免粘底糊锅', 4);

-- ---------------------------------------------------------------- 4
INSERT INTO t_recipe (name, cuisine, difficulty, taste, cook_time, description, servings, source, status, calories, protein, fat, carbs)
VALUES ('五花肉炒胡萝卜', '家常菜', '简单', '咸香', 15,
        '五花肉煸出的油把胡萝卜炒得甜软，荤素搭配，15 分钟出锅。', 2, '人工', '已上线', 338, 14.6, 26.8, 10.2);
SET @rid = LAST_INSERT_ID();
INSERT INTO t_recipe_ingredient (recipe_id, ingredient_id, name, amount, unit, is_main, is_required, sort) VALUES
(@rid, (SELECT id FROM t_ingredient WHERE name = '五花肉' LIMIT 1), '五花肉', '200', '克', 1, 1, 1),
(@rid, (SELECT id FROM t_ingredient WHERE name = '胡萝卜' LIMIT 1), '胡萝卜', '2',   '根', 1, 1, 2),
(@rid, (SELECT id FROM t_ingredient WHERE name = '青椒' LIMIT 1),   '青椒',   '1',   '个', 0, 0, 3),
(@rid, (SELECT id FROM t_ingredient WHERE name = '生抽' LIMIT 1),   '生抽',   '1',   '勺', 0, 1, 4),
(@rid, (SELECT id FROM t_ingredient WHERE name = '盐' LIMIT 1),     '盐',     '2',   '克', 0, 1, 5);
INSERT INTO t_recipe_step (recipe_id, step_no, title, description, tips, sort) VALUES
(@rid, 1, '切片煸油', '五花肉切薄片，冷锅下锅小火煸至出油、边缘微卷。', '冷锅下肉，油慢慢逼出来不粘锅', 1),
(@rid, 2, '炒胡萝卜', '下胡萝卜片翻炒 3 分钟，让胡萝卜吸足猪油。', '胡萝卜素是脂溶性的，有油才好吸收', 2),
(@rid, 3, '调味出锅', '加青椒片、生抽和盐，大火翻炒 1 分钟即可。', '青椒最后放，保持脆口和翠绿', 3);

-- ---------------------------------------------------------------- 5
INSERT INTO t_recipe (name, cuisine, difficulty, taste, cook_time, description, servings, source, status, calories, protein, fat, carbs)
VALUES ('胡萝卜炒蛋', '家常菜', '简单', '清淡', 12,
        '胡萝卜丝炒软后和鸡蛋合炒，微甜带蛋香，孩子也爱吃。', 2, '人工', '已上线', 218, 12.3, 13.4, 11.6);
SET @rid = LAST_INSERT_ID();
INSERT INTO t_recipe_ingredient (recipe_id, ingredient_id, name, amount, unit, is_main, is_required, sort) VALUES
(@rid, (SELECT id FROM t_ingredient WHERE name = '胡萝卜' LIMIT 1), '胡萝卜', '2', '根', 1, 1, 1),
(@rid, (SELECT id FROM t_ingredient WHERE name = '鸡蛋' LIMIT 1),   '鸡蛋',   '3', '个', 1, 1, 2),
(@rid, (SELECT id FROM t_ingredient WHERE name = '食用油' LIMIT 1), '食用油', '2', '勺', 0, 1, 3),
(@rid, (SELECT id FROM t_ingredient WHERE name = '葱花' LIMIT 1),   '葱花',   '少许', NULL, 0, 0, 4),
(@rid, (SELECT id FROM t_ingredient WHERE name = '盐' LIMIT 1),     '盐',     '2', '克', 0, 1, 5);
INSERT INTO t_recipe_step (recipe_id, step_no, title, description, tips, sort) VALUES
(@rid, 1, '胡萝卜切丝', '胡萝卜去皮切细丝，越细越容易炒软。', '刀工不熟练可用擦丝器', 1),
(@rid, 2, '炒软胡萝卜', '热油下胡萝卜丝中火炒 3 分钟至变软出甜味，盛出。', '胡萝卜要多炒一会儿才不硬', 2),
(@rid, 3, '炒蛋合炒', '鸡蛋打散加盐，热油炒至半凝固，倒回胡萝卜翻炒均匀撒葱花。', '蛋液半凝固时下胡萝卜，口感最嫩', 3);

-- ---------------------------------------------------------------- 6
INSERT INTO t_recipe (name, cuisine, difficulty, taste, cook_time, description, servings, source, status, calories, protein, fat, carbs)
VALUES ('洋葱炒蛋', '家常菜', '简单', '咸香', 12,
        '洋葱炒到半透明带甜，裹着嫩蛋，十分钟搞定的下饭快手菜。', 2, '人工', '已上线', 226, 12.8, 14.2, 10.8);
SET @rid = LAST_INSERT_ID();
INSERT INTO t_recipe_ingredient (recipe_id, ingredient_id, name, amount, unit, is_main, is_required, sort) VALUES
(@rid, (SELECT id FROM t_ingredient WHERE name = '洋葱' LIMIT 1),   '洋葱',   '1', '个', 1, 1, 1),
(@rid, (SELECT id FROM t_ingredient WHERE name = '鸡蛋' LIMIT 1),   '鸡蛋',   '3', '个', 1, 1, 2),
(@rid, (SELECT id FROM t_ingredient WHERE name = '食用油' LIMIT 1), '食用油', '2', '勺', 0, 1, 3),
(@rid, (SELECT id FROM t_ingredient WHERE name = '盐' LIMIT 1),     '盐',     '2', '克', 0, 1, 4);
INSERT INTO t_recipe_step (recipe_id, step_no, title, description, tips, sort) VALUES
(@rid, 1, '洋葱切丝', '洋葱切细丝，怕辣眼可先放冰箱冷藏 10 分钟再切。', '冷藏后切不流泪', 1),
(@rid, 2, '炒洋葱', '热油下洋葱中火炒 2 分钟至边缘透明、香味变甜。', '别炒到发黄，透明就够', 2),
(@rid, 3, '合炒鸡蛋', '倒入加了盐的蛋液，静置 3 秒再翻炒，蛋液凝固即出锅。', '下蛋液后稍等再翻，蛋才成块', 3);

-- ---------------------------------------------------------------- 7
INSERT INTO t_recipe (name, cuisine, difficulty, taste, cook_time, description, servings, source, status, calories, protein, fat, carbs)
VALUES ('糖醋豆腐', '家常菜', '简单', '酸甜', 18,
        '豆腐煎出脆壳，裹上酸甜汁，素菜也能做出糖醋排骨的满足感。', 2, '人工', '已上线', 246, 13.2, 14.8, 15.6);
SET @rid = LAST_INSERT_ID();
INSERT INTO t_recipe_ingredient (recipe_id, ingredient_id, name, amount, unit, is_main, is_required, sort) VALUES
(@rid, (SELECT id FROM t_ingredient WHERE name = '豆腐' LIMIT 1),   '豆腐',   '400', '克', 1, 1, 1),
(@rid, (SELECT id FROM t_ingredient WHERE name = '白醋' LIMIT 1),   '白醋',   '2',   '勺', 0, 1, 2),
(@rid, (SELECT id FROM t_ingredient WHERE name = '白糖' LIMIT 1),   '白糖',   '2',   '勺', 0, 1, 3),
(@rid, (SELECT id FROM t_ingredient WHERE name = '生抽' LIMIT 1),   '生抽',   '1',   '勺', 0, 1, 4),
(@rid, (SELECT id FROM t_ingredient WHERE name = '面粉' LIMIT 1),   '面粉',   '2',   '勺', 0, 0, 5),
(@rid, (SELECT id FROM t_ingredient WHERE name = '食用油' LIMIT 1), '食用油', '3',   '勺', 0, 1, 6);
INSERT INTO t_recipe_step (recipe_id, step_no, title, description, tips, sort) VALUES
(@rid, 1, '豆腐裹粉', '豆腐切块吸干水分，薄薄裹一层面粉。', '裹粉是形成脆壳的关键', 1),
(@rid, 2, '煎至金黄', '中火下锅，六面煎至金黄定型后盛出。', '定型前不要翻动，容易破', 2),
(@rid, 3, '调糖醋汁', '白醋、白糖、生抽加两勺水调匀，倒入锅中煮到冒泡微稠。', '糖醋比例 1:1 最好入口', 3),
(@rid, 4, '裹汁出锅', '倒回豆腐快速翻匀，让每块都裹上汁即可。', '动作要快，久煮脆壳会软', 4);

-- ---------------------------------------------------------------- 8
INSERT INTO t_recipe (name, cuisine, difficulty, taste, cook_time, description, servings, source, status, calories, protein, fat, carbs)
VALUES ('番茄豆腐汤', '家常菜', '简单', '酸甜', 15,
        '番茄炒出沙再加豆腐煮，汤色红亮、豆腐滑嫩，一碗下去很舒服。', 2, '人工', '已上线', 148, 10.6, 7.2, 9.8);
SET @rid = LAST_INSERT_ID();
INSERT INTO t_recipe_ingredient (recipe_id, ingredient_id, name, amount, unit, is_main, is_required, sort) VALUES
(@rid, (SELECT id FROM t_ingredient WHERE name = '番茄' LIMIT 1),   '番茄',   '2',   '个', 1, 1, 1),
(@rid, (SELECT id FROM t_ingredient WHERE name = '豆腐' LIMIT 1),   '豆腐',   '300', '克', 1, 1, 2),
(@rid, (SELECT id FROM t_ingredient WHERE name = '葱花' LIMIT 1),   '葱花',   '少许', NULL, 0, 0, 3),
(@rid, (SELECT id FROM t_ingredient WHERE name = '食用油' LIMIT 1), '食用油', '1',   '勺', 0, 1, 4),
(@rid, (SELECT id FROM t_ingredient WHERE name = '盐' LIMIT 1),     '盐',     '2',   '克', 0, 1, 5);
INSERT INTO t_recipe_step (recipe_id, step_no, title, description, tips, sort) VALUES
(@rid, 1, '番茄炒沙', '番茄切块，热油下锅中火炒到出汁变沙。', '炒出沙汤才浓、颜色才红', 1),
(@rid, 2, '加水煮开', '加两碗热水煮开，下豆腐块转中小火煮 5 分钟。', '热水下锅汤更清甜', 2),
(@rid, 3, '调味出锅', '加盐调味，撒葱花即可。', '豆腐易碎，用推动锅身代替搅拌', 3);

-- ---------------------------------------------------------------- 9
INSERT INTO t_recipe (name, cuisine, difficulty, taste, cook_time, description, servings, source, status, calories, protein, fat, carbs)
VALUES ('胡萝卜土豆丝', '家常菜', '简单', '酸香', 15,
        '土豆丝脆、胡萝卜丝甜，一点白醋提味，配色好看又下饭。', 2, '人工', '已上线', 186, 5.2, 6.4, 27.8);
SET @rid = LAST_INSERT_ID();
INSERT INTO t_recipe_ingredient (recipe_id, ingredient_id, name, amount, unit, is_main, is_required, sort) VALUES
(@rid, (SELECT id FROM t_ingredient WHERE name = '土豆' LIMIT 1),   '土豆',   '2', '个', 1, 1, 1),
(@rid, (SELECT id FROM t_ingredient WHERE name = '胡萝卜' LIMIT 1), '胡萝卜', '1', '根', 1, 1, 2),
(@rid, (SELECT id FROM t_ingredient WHERE name = '青椒' LIMIT 1),   '青椒',   '1', '个', 0, 0, 3),
(@rid, (SELECT id FROM t_ingredient WHERE name = '白醋' LIMIT 1),   '白醋',   '1', '勺', 0, 1, 4),
(@rid, (SELECT id FROM t_ingredient WHERE name = '食用油' LIMIT 1), '食用油', '2', '勺', 0, 1, 5),
(@rid, (SELECT id FROM t_ingredient WHERE name = '盐' LIMIT 1),     '盐',     '2', '克', 0, 1, 6);
INSERT INTO t_recipe_step (recipe_id, step_no, title, description, tips, sort) VALUES
(@rid, 1, '切丝泡水', '土豆、胡萝卜切细丝，土豆丝用清水冲两遍去淀粉后沥干。', '冲掉淀粉才不粘锅、更脆', 1),
(@rid, 2, '大火快炒', '热油下土豆丝和胡萝卜丝，大火翻炒 2 分钟。', '全程大火，火小了会出水变软', 2),
(@rid, 3, '烹醋调味', '沿锅边淋白醋，加青椒丝和盐，再炒 30 秒出锅。', '醋沿锅边淋，酸香更足', 3);

-- ---------------------------------------------------------------- 10
INSERT INTO t_recipe (name, cuisine, difficulty, taste, cook_time, description, servings, source, status, calories, protein, fat, carbs)
VALUES ('土豆胡萝卜炖牛腩', '家常菜', '中等', '咸香', 90,
        '牛腩炖到软烂，土豆吸饱肉汁、胡萝卜回甜，一锅端上桌。', 3, '人工', '已上线', 412, 26.4, 24.8, 22.6);
SET @rid = LAST_INSERT_ID();
INSERT INTO t_recipe_ingredient (recipe_id, ingredient_id, name, amount, unit, is_main, is_required, sort) VALUES
(@rid, (SELECT id FROM t_ingredient WHERE name = '牛腩' LIMIT 1),   '牛腩',   '500', '克', 1, 1, 1),
(@rid, (SELECT id FROM t_ingredient WHERE name = '土豆' LIMIT 1),   '土豆',   '2',   '个', 1, 1, 2),
(@rid, (SELECT id FROM t_ingredient WHERE name = '胡萝卜' LIMIT 1), '胡萝卜', '2',   '根', 0, 1, 3),
(@rid, (SELECT id FROM t_ingredient WHERE name = '生抽' LIMIT 1),   '生抽',   '2',   '勺', 0, 1, 4),
(@rid, (SELECT id FROM t_ingredient WHERE name = '葱' LIMIT 1),     '葱',     '2',   '段', 0, 0, 5),
(@rid, (SELECT id FROM t_ingredient WHERE name = '盐' LIMIT 1),     '盐',     '3',   '克', 0, 1, 6);
INSERT INTO t_recipe_step (recipe_id, step_no, title, description, tips, sort) VALUES
(@rid, 1, '牛腩焯水', '牛腩切块冷水下锅，煮开撇沫后捞出冲净。', '冷水下锅才能把血水逼出来', 1),
(@rid, 2, '炖牛腩', '牛腩加生抽、葱段和没过食材的热水，大火烧开转小火炖 50 分钟。', '水一次加够，中途加水会变柴', 2),
(@rid, 3, '下根茎', '加土豆块、胡萝卜块继续炖 20 分钟至软糯。', '土豆后放，炖久了会化在汤里', 3),
(@rid, 4, '调味收汁', '加盐调味，大火收至汤汁略浓即可。', '盐最后放，牛肉更容易烂', 4);

-- ---------------------------------------------------------------- 11
INSERT INTO t_recipe (name, cuisine, difficulty, taste, cook_time, description, servings, source, status, calories, protein, fat, carbs)
VALUES ('黑椒煎鸡胸', '轻食', '简单', '咸香', 20,
        '鸡胸提前拍松腌入味，外焦里嫩不柴，配洋葱一起煎更香。', 2, '人工', '已上线', 286, 34.2, 12.6, 7.4);
SET @rid = LAST_INSERT_ID();
INSERT INTO t_recipe_ingredient (recipe_id, ingredient_id, name, amount, unit, is_main, is_required, sort) VALUES
(@rid, (SELECT id FROM t_ingredient WHERE name = '鸡胸肉' LIMIT 1), '鸡胸肉', '2',   '块', 1, 1, 1),
(@rid, (SELECT id FROM t_ingredient WHERE name = '洋葱' LIMIT 1),   '洋葱',   '1',   '个', 1, 0, 2),
(@rid, (SELECT id FROM t_ingredient WHERE name = '生抽' LIMIT 1),   '生抽',   '1',   '勺', 0, 1, 3),
(@rid, (SELECT id FROM t_ingredient WHERE name = '食用油' LIMIT 1), '食用油', '1',   '勺', 0, 1, 4),
(@rid, (SELECT id FROM t_ingredient WHERE name = '盐' LIMIT 1),     '盐',     '2',   '克', 0, 1, 5);
INSERT INTO t_recipe_step (recipe_id, step_no, title, description, tips, sort) VALUES
(@rid, 1, '拍松腌制', '鸡胸横刀片薄、用刀背拍松，加生抽、盐抓匀腌 10 分钟。', '拍松断筋，煎完不柴不卷', 1),
(@rid, 2, '中火煎制', '热锅少油，中小火每面煎 3 分钟至金黄。', '火别大，外焦里生是常见失败', 2),
(@rid, 3, '洋葱同煎', '利用余油把洋葱圈煎香，撒在鸡胸上即可。', '煎完静置 2 分钟再切，肉汁不流失', 3);

-- ---------------------------------------------------------------- 12
INSERT INTO t_recipe (name, cuisine, difficulty, taste, cook_time, description, servings, source, status, calories, protein, fat, carbs)
VALUES ('家常蛋炒饭', '家常菜', '简单', '咸香', 12,
        '隔夜米饭粒粒分明，鸡蛋裹住米粒，加胡萝卜丁配色，一人食首选。', 1, '人工', '已上线', 428, 15.6, 16.8, 54.2);
SET @rid = LAST_INSERT_ID();
INSERT INTO t_recipe_ingredient (recipe_id, ingredient_id, name, amount, unit, is_main, is_required, sort) VALUES
(@rid, (SELECT id FROM t_ingredient WHERE name = '大米' LIMIT 1),   '大米',   '1', '碗', 1, 1, 1),
(@rid, (SELECT id FROM t_ingredient WHERE name = '鸡蛋' LIMIT 1),   '鸡蛋',   '2', '个', 1, 1, 2),
(@rid, (SELECT id FROM t_ingredient WHERE name = '胡萝卜' LIMIT 1), '胡萝卜', '半', '根', 0, 0, 3),
(@rid, (SELECT id FROM t_ingredient WHERE name = '葱花' LIMIT 1),   '葱花',   '少许', NULL, 0, 0, 4),
(@rid, (SELECT id FROM t_ingredient WHERE name = '食用油' LIMIT 1), '食用油', '2', '勺', 0, 1, 5),
(@rid, (SELECT id FROM t_ingredient WHERE name = '盐' LIMIT 1),     '盐',     '2', '克', 0, 1, 6);
INSERT INTO t_recipe_step (recipe_id, step_no, title, description, tips, sort) VALUES
(@rid, 1, '米饭打散', '隔夜米饭用手抓散，胡萝卜切小丁。', '新煮的饭水分多，炒不散', 1),
(@rid, 2, '炒蛋', '鸡蛋打散加盐，热油下锅快速划散至刚凝固盛出。', '蛋炒到七成熟就盛出，回锅才嫩', 2),
(@rid, 3, '炒饭', '下胡萝卜丁炒香，倒入米饭中大火翻炒到粒粒分明。', '用铲背压散饭团，别用铲刃剁', 3),
(@rid, 4, '合炒调味', '倒回鸡蛋，加盐和葱花翻炒均匀出锅。', '葱花最后放，香气才冲', 4);

-- ---------------------------------------------------------------- 校验
SELECT COUNT(*) AS 在线菜谱数 FROM t_recipe WHERE status = '已上线';
SELECT '豆腐' AS 食材, COUNT(DISTINCT recipe_id) AS 覆盖菜谱数 FROM t_recipe_ingredient WHERE name = '豆腐'
UNION ALL SELECT '胡萝卜', COUNT(DISTINCT recipe_id) FROM t_recipe_ingredient WHERE name = '胡萝卜'
UNION ALL SELECT '五花肉', COUNT(DISTINCT recipe_id) FROM t_recipe_ingredient WHERE name = '五花肉'
UNION ALL SELECT '洋葱',   COUNT(DISTINCT recipe_id) FROM t_recipe_ingredient WHERE name = '洋葱';
