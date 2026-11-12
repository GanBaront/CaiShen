package com.caishen.foodlab.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 接口文档配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI caishenOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("材神食验室 接口文档")
                .description("用户端 /api/** 与管理端 /api/admin/** 接口")
                .version("1.0.0"));
    }
}
