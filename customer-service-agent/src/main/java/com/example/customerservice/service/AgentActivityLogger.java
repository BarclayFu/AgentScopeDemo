package com.example.customerservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Agent活动记录服务
 * 用于记录Agent的工具调用和活动日志
 */
@Service
public class AgentActivityLogger {

    private static final Logger logger = LoggerFactory.getLogger(AgentActivityLogger.class);

    /**
     * 记录工具调用开始
     */
    public void logToolCallStart(String toolName, String parameters) {
        logger.info("🔧 工具调用开始: {} | 参数: {}", toolName, parameters);
    }

    /**
     * 记录工具调用完成
     */
    public void logToolCallEnd(String toolName, String result) {
        logger.info("✅ 工具调用完成: {} | 结果长度: {} 字符",
            toolName, result != null ? result.length() : 0);
    }

    /**
     * 记录工具调用错误
     */
    public void logToolCallError(String toolName, String error) {
        logger.error("❌ 工具调用错误: {} | 错误信息: {}", toolName, error);
    }

    /**
     * 记录Agent消息处理开始
     */
    public void logMessageProcessingStart(String agentName, String message) {
        logger.info("🚀 Agent消息处理开始: {} | 消息: {}", agentName, message);
    }

    /**
     * 记录Agent消息处理完成
     */
    public void logMessageProcessingEnd(String agentName, String response) {
        logger.info("🏁 Agent消息处理完成: {} | 响应长度: {} 字符",
            agentName, response != null ? response.length() : 0);
    }

    /**
     * 记录Agent思考过程
     */
    public void logAgentThought(String agentName, String thought) {
        logger.info("💭 Agent思考 [{}]: {}", agentName, thought);
    }
}
