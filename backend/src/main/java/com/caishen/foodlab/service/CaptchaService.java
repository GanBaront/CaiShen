package com.caishen.foodlab.service;

import com.caishen.foodlab.common.BusinessException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码：内存存储，5 分钟有效，一次性使用。
 * 用于登录 / 注册的人机验证。
 */
@Service
public class CaptchaService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final long EXPIRE_MS = 5 * 60 * 1000L;
    private static final int CODE_LEN = 4;

    private final SecureRandom random = new SecureRandom();

    /** captchaId -> {code, expireAt} */
    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    private record CaptchaEntry(String code, long expireAt) {}

    /** 生成验证码，返回 captchaId 与 base64 图片（dataURL） */
    public CaptchaVO generate() {
        String code = randomCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        store.put(captchaId, new CaptchaEntry(code, System.currentTimeMillis() + EXPIRE_MS));
        cleanup();
        String image = toBase64Png(code);
        return new CaptchaVO(captchaId, image);
    }

    /** 校验并消费验证码（一次性） */
    public void verify(String captchaId, String code) {
        if (captchaId == null || captchaId.isBlank() || code == null || code.isBlank()) {
            throw new BusinessException("请输入图形验证码");
        }
        CaptchaEntry entry = store.remove(captchaId.trim());
        if (entry == null) {
            throw new BusinessException("验证码已失效，请刷新后重试");
        }
        if (System.currentTimeMillis() > entry.expireAt()) {
            throw new BusinessException("验证码已过期，请刷新后重试");
        }
        if (!entry.code().equalsIgnoreCase(code.trim())) {
            throw new BusinessException("验证码错误");
        }
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String toBase64Png(String code) {
        int width = 130, height = 44;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 背景
        g.setColor(new Color(242, 246, 243));
        g.fillRect(0, 0, width, height);
        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(150 + random.nextInt(80), 170 + random.nextInt(60), 150 + random.nextInt(80)));
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(random.nextInt(width), random.nextInt(height),
                    random.nextInt(width), random.nextInt(height));
        }
        // 干扰点
        for (int i = 0; i < 40; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.fillRect(random.nextInt(width), random.nextInt(height), 1, 1);
        }
        // 字符
        int charWidth = width / (CODE_LEN + 1);
        for (int i = 0; i < CODE_LEN; i++) {
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.setColor(new Color(31 + random.nextInt(60), 78 + random.nextInt(40), 59 + random.nextInt(40)));
            double theta = (random.nextDouble() - 0.5) * 0.5; // ±约14度
            int x = charWidth * (i + 1) - 10;
            int y = height / 2 + 10;
            g.rotate(theta, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-theta, x, y);
        }
        g.dispose();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("生成验证码图片失败", e);
        }
    }

    /** 清理过期条目，防止内存膨胀 */
    private void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now > e.getValue().expireAt());
    }

    /** 验证码返回体 */
    public record CaptchaVO(String captchaId, String image) {}
}
