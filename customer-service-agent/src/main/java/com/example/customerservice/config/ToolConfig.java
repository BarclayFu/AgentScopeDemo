package com.example.customerservice.config;

import com.example.customerservice.tools.CustomerServiceTools;
import io.agentscope.core.rag.Knowledge;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * 工具配置类，用于初始化工具类所需的依赖
 */
@Configuration
public class ToolConfig {

    @Autowired
    private Knowledge knowledgeBase;

    @Autowired
    private CustomerServiceTools customerServiceTools;

    /**
     * 在Spring容器初始化完成后，将Knowledge实例注入到CustomerServiceTools中
     */
    @PostConstruct
    public void initializeTools() {
        customerServiceTools.setKnowledgeBase(knowledgeBase);
    }
}
