package com.caishen.foodlab.common;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 食材 / 菜名归一化。
 *
 * 解决的真实问题：大模型和用户的叫法与菜谱库的书面名常常对不上——
 * 模型看到「西红柿炒鸡蛋」，库里存的是「番茄炒蛋」，直接字符串比较会判为完全不同，
 * 于是"识别成功了但匹配不到任何菜谱"。
 *
 * 两层处理：
 * 1. 精确别名表（aliases）：西红柿 → 番茄，马铃薯 → 土豆；
 * 2. 子串替换（仅长度≥2 的别名）：让「西红柿炒鸡蛋」整体归一为「番茄炒鸡蛋」，
 *    再配合字符集合相似度即可匹配到「番茄炒蛋」。
 */
public final class NameNormalizer {

    private NameNormalizer() {
    }

    /** 精确别名（用户/模型常用叫法 → 库内书面名） */
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("西红柿", "番茄"),
            Map.entry("蕃茄", "番茄"),
            Map.entry("番茄仔", "番茄"),
            Map.entry("圣女果", "番茄"),
            Map.entry("蛋", "鸡蛋"),
            Map.entry("土鸡蛋", "鸡蛋"),
            Map.entry("马铃薯", "土豆"),
            Map.entry("洋芋", "土豆"),
            Map.entry("薯仔", "土豆"),
            Map.entry("尖椒", "青椒"),
            Map.entry("柿子椒", "青椒"),
            Map.entry("菜椒", "青椒"),
            Map.entry("青尖椒", "青椒"),
            Map.entry("小葱", "葱"),
            Map.entry("香葱", "葱"),
            Map.entry("大葱", "葱"),
            Map.entry("葱段", "葱花"),
            Map.entry("葱丝", "葱花"),
            Map.entry("葱末", "葱花"),
            Map.entry("蒜", "大蒜"),
            Map.entry("蒜瓣", "大蒜"),
            Map.entry("蒜头", "大蒜"),
            Map.entry("白砂糖", "白糖"),
            Map.entry("砂糖", "白糖"),
            Map.entry("绵白糖", "白糖"),
            Map.entry("米醋", "白醋"),
            Map.entry("白米醋", "白醋"),
            Map.entry("中筋面粉", "面粉"),
            Map.entry("小麦粉", "面粉"),
            Map.entry("普通面粉", "面粉"),
            Map.entry("红萝卜", "胡萝卜"),
            Map.entry("鸡胸脯肉", "鸡胸肉"),
            Map.entry("鸡胸", "鸡胸肉"),
            Map.entry("牛腩肉", "牛腩"),
            Map.entry("嫩豆腐", "豆腐"),
            Map.entry("老豆腐", "豆腐"),
            Map.entry("五花", "五花肉"),
            Map.entry("大米饭", "大米"),
            Map.entry("生油", "食用油"),
            Map.entry("沙拉油", "食用油"),
            Map.entry("色拉油", "食用油"),
            Map.entry("花生米", "花生")
    );

    /** 只对长度≥2 的别名做子串替换，避免「蛋」把「蛋糕」改成「鸡蛋糕」这类误伤 */
    private static final List<Map.Entry<String, String>> SUBSTRING_RULES = ALIASES.entrySet().stream()
            .filter(e -> e.getKey().length() >= 2)
            .sorted((a, b) -> b.getKey().length() - a.getKey().length())
            .toList();

    /** 单个食材名归一化（精确匹配 + 子串替换） */
    public static String canonical(String name) {
        if (name == null) {
            return "";
        }
        String n = name.trim().replaceAll("\\s+", "");
        if (n.isEmpty()) {
            return n;
        }
        String exact = ALIASES.get(n);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, String> rule : SUBSTRING_RULES) {
            if (n.contains(rule.getKey())) {
                n = n.replace(rule.getKey(), rule.getValue());
            }
        }
        return n;
    }

    /** 批量归一化为集合（去重） */
    public static Set<String> canonicalSet(Collection<String> names) {
        Set<String> out = new LinkedHashSet<>();
        if (names == null) {
            return out;
        }
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                out.add(canonical(name));
            }
        }
        return out;
    }

    /**
     * 菜名相似度：字符集合的 Dice 系数（2×交集 / 两串长度和）。
     * 「番茄炒蛋」vs「番茄炒鸡蛋」≈0.89；「番茄炒蛋」vs「青椒炒蛋」≈0.44，区分度足够。
     */
    public static double similarity(String a, String b) {
        String x = strip(canonical(a));
        String y = strip(canonical(b));
        if (x.isEmpty() || y.isEmpty()) {
            return 0;
        }
        if (x.equals(y)) {
            return 1;
        }
        Set<Character> setX = toCharSet(x);
        Set<Character> setY = toCharSet(y);
        int total = x.length() + y.length();
        if (total == 0) {
            return 0;
        }
        int inter = 0;
        for (Character c : setX) {
            if (setY.contains(c)) {
                inter++;
            }
        }
        return 2.0 * inter / total;
    }

    private static String strip(String text) {
        return text.replaceAll("[\\s（）()、,，.。·\\[\\]【】]", "");
    }

    private static Set<Character> toCharSet(String text) {
        Set<Character> set = new LinkedHashSet<>();
        for (char c : text.toCharArray()) {
            set.add(c);
        }
        return set;
    }
}
