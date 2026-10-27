package com.yechijia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 跨域配置。
 * 前端开发服务器（5173 端口）与后端（8080 端口）端口不同，
 * 需要放行跨域请求，前端才能正常调用后端接口。
 */
@Configuration
public class CorsConfig {

    /** 允许的跨域来源：默认 * 全放开（演示环境），生产环境在配置文件收敛为域名白名单 */
    @Value("${yechijia.cors.allowed-origins:*}")
    private List<String> allowedOrigins;

    /**
     * 注册全局跨域规则：按配置的来源白名单放行 /api 接口。
     *
     * @return Spring MVC 跨域配置对象
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            /** 对所有 /api 路径应用白名单跨域规则，允许常用方法 */
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns(allowedOrigins.toArray(new String[0]))
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(3600);
            }
        };
    }
}
