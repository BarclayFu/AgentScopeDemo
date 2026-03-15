package com.example.customerservice.service;

import com.example.customerservice.dto.MonitoringResetResponse;
import com.example.customerservice.dto.MonitoringStatusResponse;
import com.example.customerservice.dto.MonitoringSummary;
import com.example.customerservice.dto.MonitoringSummaryResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Agent监控服务，用于跟踪和记录Agent的活动
 */
@Service
public class AgentMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(AgentMonitoringService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SERVICE_NAME = "Customer Service Agent";

    private final AtomicLong toolCallCount = new AtomicLong(0);
    private final AtomicLong messageCount = new AtomicLong(0);
    private final AtomicLong completedMessageCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong totalResponseTimeMs = new AtomicLong(0);
    private final AtomicLong lastMessageAt = new AtomicLong(0);
    private final AtomicLong lastErrorAt = new AtomicLong(0);
    private final Map<String, Long> messageStartTimes = new ConcurrentHashMap<>();

    /**
     * 记录Agent开始处理消息
     */
    public void recordAgentStart(String agentName, String message) {
        long msgId = messageCount.incrementAndGet();
        messageStartTimes.put(agentName, System.currentTimeMillis());
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
        long completedAt = System.currentTimeMillis();
        Long startedAt = messageStartTimes.remove(agentName);
        if (startedAt != null) {
            totalResponseTimeMs.addAndGet(Math.max(0, completedAt - startedAt));
        }
        completedMessageCount.incrementAndGet();
        lastMessageAt.set(completedAt);
        logger.info("=== Agent Response ===");
        logger.info("Agent: {} | Time: {}",
            agentName, LocalDateTime.now().format(formatter));
        logger.info("Response: {}", response);
        logger.info("=====================");
    }

    /**
     * 记录处理错误
     */
    public void recordError(String source, String error) {
        errorCount.incrementAndGet();
        lastErrorAt.set(System.currentTimeMillis());
        logger.error("!!! Agent Error [{}] {}", source, error);
    }

    /**
     * 获取统计信息
     */
    public String getStatistics() {
        return String.format("Total Messages: %d | Total Tool Calls: %d",
            messageCount.get(), toolCallCount.get());
    }

    /**
     * 获取结构化监控摘要
     */
    public MonitoringSummaryResponse getSummary(int activeSessions) {
        return new MonitoringSummaryResponse(
            new MonitoringSummary(
                activeSessions,
                messageCount.get(),
                toolCallCount.get(),
                errorCount.get(),
                getAverageResponseTimeMs(),
                toNullableTimestamp(lastMessageAt.get()),
                toNullableTimestamp(lastErrorAt.get())
            ),
            Instant.now().toEpochMilli()
        );
    }

    /**
     * 获取服务健康状态
     */
    public MonitoringStatusResponse getStatus() {
        return new MonitoringStatusResponse(
            SERVICE_NAME,
            "UP",
            Instant.now().toEpochMilli()
        );
    }

    /**
     * 重置统计并返回结果
     */
    public MonitoringResetResponse resetAndGetResponse() {
        resetStatistics();
        return new MonitoringResetResponse(
            "Statistics reset successfully",
            Instant.now().toEpochMilli()
        );
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        toolCallCount.set(0);
        messageCount.set(0);
        completedMessageCount.set(0);
        errorCount.set(0);
        totalResponseTimeMs.set(0);
        lastMessageAt.set(0);
        lastErrorAt.set(0);
        messageStartTimes.clear();
        logger.info("Statistics reset completed");
    }

    private long getAverageResponseTimeMs() {
        long completed = completedMessageCount.get();
        if (completed <= 0) {
            return 0;
        }
        return totalResponseTimeMs.get() / completed;
    }

    private Long toNullableTimestamp(long timestamp) {
        return timestamp > 0 ? timestamp : null;
    }
}
