package com.zct.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * 简单产品状态查询应用程序启动类
 * 参考vendor-b系统架构，基于Spring Boot
 */
@SpringBootApplication
@EnableWebMvc
public class SimpleProductStatusQueryApplication {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("简单产品状态查询服务启动中...");
        System.out.println("参考vendor-b系统架构实现");
        System.out.println("==========================================");

        SpringApplication.run(SimpleProductStatusQueryApplication.class, args);

        System.out.println("==========================================");
        System.out.println("服务启动完成!");
        System.out.println("Swagger UI: http://localhost:8080/swagger-ui.html");
        System.out.println("API文档: http://localhost:8080/v2/api-docs");
        System.out.println("健康检查: http://localhost:8080/api/product-status/health");
        System.out.println("==========================================");
    }
}