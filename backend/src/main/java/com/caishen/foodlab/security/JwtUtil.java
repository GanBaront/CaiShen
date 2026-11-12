package com.caishen.foodlab.security;

import com.caishen.foodlab.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：用户端与管理端共用密钥，通过 role 声明区分身份
 */
@Component
public class JwtUtil {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    private final SecretKey secretKey;
    private final int expireHours;

    public JwtUtil(AppProperties appProperties) {
        this.secretKey = Keys.hmacShaKeyFor(
                appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.expireHours = appProperties.getJwt().getExpireHours();
    }

    /** 生成用户端 token */
    public String generateUserToken(Long userId, String account) {
        return generate(String.valueOf(userId), account, ROLE_USER);
    }

    /** 生成管理端 token */
    public String generateAdminToken(Long adminId, String username) {
        return generate(String.valueOf(adminId), username, ROLE_ADMIN);
    }

    private String generate(String subject, String name, String role) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expireHours * 3600_000L);
        return Jwts.builder()
                .subject(subject)
                .claim("name", name)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expire)
                .signWith(secretKey)
                .compact();
    }

    /** 解析 token，失败返回 null */
    public Claims parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
