package com.example.customerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class CustomerServiceAgentApplication {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceAgentApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceAgentApplication.class, args);
    }

    @PostConstruct
    public void startup() {
        logger.info("===========================================");
        logger.info("  智能客服Agent系统启动中...");
        logger.info("===========================================");
        logger.info("请确保已设置DASHSCOPE_API_KEY环境变量");
        logger.info("API接口地址: http://localhost:8080/api/chat/");
        logger.info("===========================================");
    }
}
