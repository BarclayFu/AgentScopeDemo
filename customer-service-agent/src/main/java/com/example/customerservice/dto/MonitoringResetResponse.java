package com.example.customerservice.dto;

/**
 * 监控重置响应
 */
public class MonitoringResetResponse {

    private final String message;
    private final long checkedAt;

    public MonitoringResetResponse(String message, long checkedAt) {
        this.message = message;
        this.checkedAt = checkedAt;
    }

    public String getMessage() {
        return message;
    }

    public long getCheckedAt() {
        return checkedAt;
    }
}
