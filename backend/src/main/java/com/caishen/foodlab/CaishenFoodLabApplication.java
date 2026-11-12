package com.caishen.foodlab;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 材神食验室 · 后端启动类
 */
@SpringBootApplication
@MapperScan("com.caishen.foodlab.mapper")
@EnableScheduling
public class CaishenFoodLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaishenFoodLabApplication.class, args);
    }
}
