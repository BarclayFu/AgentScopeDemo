package com.example.customerservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent监控服务，用于跟踪和记录Agent的活动
 */
@Service
public class AgentMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(AgentMonitoringService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AtomicLong toolCallCount = new AtomicLong(0);
    private final AtomicLong messageCount = new AtomicLong(0);

    /**
     * 记录Agent开始处理消息
     */
    public void recordAgentStart(String agentName, String message) {
        long msgId = messageCount.incrementAndGet();
        logger.info("=== Agent Activity Start ===");
        logger.info("Agent: {} | Message ID: {} | Time: {}",
            agentName, msgId, LocalDateTime.now().format(formatter));
        logger.info("Input Message: {}", message);
        logger.info("==========================");
    }

    /**
     * 记录工具调用
     */
    public void recordToolCall(String toolName, String parameters) {
        long callId = toolCallCount.incrementAndGet();
        logger.info(">>> Tool Call #{} - {} <<<", callId, toolName);
        logger.info("Parameters: {}", parameters);
        logger.info("Time: {}", LocalDateTime.now().format(formatter));
    }

    /**
     * 记录工具调用结果
     */
    public void recordToolResult(String toolName, String result) {
        logger.info("<<< Tool Result - {} >>>", toolName);
        logger.info("Result: {}", result);
        logger.info("Time: {}", LocalDateTime.now().format(formatter));
    }

    /**
     * 记录Agent思考过程
     */
    public void recordAgentThought(String thought) {
        logger.info("🧠 Agent Thought: {}", thought);
    }

    /**
     * 记录Agent最终响应
     */
    public void recordAgentResponse(String agentName, String response) {
        logger.info("=== Agent Response ===");
        logger.info("Agent: {} | Time: {}",
            agentName, LocalDateTime.now().format(formatter));
        logger.info("Response: {}", response);
        logger.info("=====================");
    }

    /**
     * 获取统计信息
     */
    public String getStatistics() {
        return String.format("Total Messages: %d | Total Tool Calls: %d",
            messageCount.get(), toolCallCount.get());
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        toolCallCount.set(0);
        messageCount.set(0);
        logger.info("Statistics reset completed");
    }
}
