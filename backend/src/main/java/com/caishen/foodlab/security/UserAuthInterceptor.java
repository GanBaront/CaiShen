package com.caishen.foodlab.security;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用户端登录拦截器。
 * 未登录可浏览首页 / 菜谱推荐 / 菜谱详情等只读接口；
 * 保存偏好、收藏、加入购物清单、下单、评价、AI 生成、会员购买等写操作要求登录。
 */
@Component
@RequiredArgsConstructor
public class UserAuthInterceptor implements HandlerInterceptor {

    /** 无需登录即可访问（任意方法） */
    private static final List<String> PUBLIC_ANY = List.of(
            "/api/auth/register",
            "/api/auth/login"
    );

    /** 无需登录即可访问的只读接口（仅 GET） */
    private static final List<String> PUBLIC_GET = List.of(
            "/api/auth/captcha",
            "/api/recipes",
            "/api/recipes/today",
            "/api/recipes/*",
            "/api/recipes/*/similar",
            "/api/recipes/*/reviews",
            "/api/ingredients/**",
            "/api/merchants/**",
            "/api/member/plans",
            "/api/ai/recipes/preview"
    );

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 尝试解析 token（公开只读接口也允许带 token，用于返回收藏状态等）
        String token = resolveToken(request);
        Claims claims = jwtUtil.parse(token);
        if (claims != null && JwtUtil.ROLE_USER.equals(claims.get("role"))) {
            AuthContext.setUserId(Long.valueOf(claims.getSubject()));
        }

        String path = request.getRequestURI();
        for (String pattern : PUBLIC_ANY) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            for (String pattern : PUBLIC_GET) {
                if (MATCHER.match(pattern, path)) {
                    return true;
                }
            }
        }
        if (AuthContext.getUserId() == null) {
            writeUnauthorized(response);
            return false;
        }
        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return request.getHeader("token");
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.error(ResultCode.UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
