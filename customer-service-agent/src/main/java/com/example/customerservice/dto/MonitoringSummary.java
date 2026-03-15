package com.example.customerservice.dto;

/**
 * 仪表盘监控摘要
 */
public class MonitoringSummary {

    private final int activeSessions;
    private final long totalMessages;
    private final long totalToolCalls;
    private final long errorCount;
    private final long avgResponseTimeMs;
    private final Long lastMessageAt;
    private final Long lastErrorAt;

    public MonitoringSummary(
        int activeSessions,
        long totalMessages,
        long totalToolCalls,
        long errorCount,
        long avgResponseTimeMs,
        Long lastMessageAt,
        Long lastErrorAt
    ) {
        this.activeSessions = activeSessions;
        this.totalMessages = totalMessages;
        this.totalToolCalls = totalToolCalls;
        this.errorCount = errorCount;
        this.avgResponseTimeMs = avgResponseTimeMs;
        this.lastMessageAt = lastMessageAt;
        this.lastErrorAt = lastErrorAt;
    }

    public int getActiveSessions() {
        return activeSessions;
    }

    public long getTotalMessages() {
        return totalMessages;
    }

    public long getTotalToolCalls() {
        return totalToolCalls;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public long getAvgResponseTimeMs() {
        return avgResponseTimeMs;
    }

    public Long getLastMessageAt() {
        return lastMessageAt;
    }

    public Long getLastErrorAt() {
        return lastErrorAt;
    }
}
