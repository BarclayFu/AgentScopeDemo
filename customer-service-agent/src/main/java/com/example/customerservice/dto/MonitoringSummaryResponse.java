package com.example.customerservice.dto;

/**
 * 监控摘要响应
 */
public class MonitoringSummaryResponse {

    private final MonitoringSummary summary;
    private final long checkedAt;

    public MonitoringSummaryResponse(MonitoringSummary summary, long checkedAt) {
        this.summary = summary;
        this.checkedAt = checkedAt;
    }

    public MonitoringSummary getSummary() {
        return summary;
    }

    public long getCheckedAt() {
        return checkedAt;
    }
}
