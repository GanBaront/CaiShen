package com.caishen.foodlab.config;

import com.caishen.foodlab.security.AdminAuthInterceptor;
import com.caishen.foodlab.security.UserAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * WebMvc 配置（拦截器注册 + 上传文件静态映射）
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserAuthInterceptor userAuthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final AppProperties appProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 用户端：/api/** 但排除管理端
        registry.addInterceptor(userAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/admin/**")
                .order(1);
        // 管理端：/api/admin/**
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .order(2);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(appProperties.getUploadDir()).toAbsolutePath().normalize();
        File dir = uploadPath.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        registry.addResourceHandler(appProperties.getUploadUrlPrefix() + "/**")
                .addResourceLocations("file:" + uploadPath + File.separator);
    }
}
