package com.yechijia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 椰吃家后端服务启动入口。
 * 解决用户「不知道吃什么、喝什么」的决策工具后端，
 * 提供出去吃 / 在家做 / 喝什么三条核心链路的接口。
 */
@SpringBootApplication
public class YechijiaApplication {

    /**
     * 服务主入口：启动内嵌 Tomcat 并加载全部 Spring 组件。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(YechijiaApplication.class, args);
    }
}
