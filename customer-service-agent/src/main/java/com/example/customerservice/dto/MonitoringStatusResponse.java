package com.example.customerservice.dto;

/**
 * 监控健康状态响应
 */
public class MonitoringStatusResponse {

    private final String service;
    private final String status;
    private final long checkedAt;

    public MonitoringStatusResponse(String service, String status, long checkedAt) {
        this.service = service;
        this.status = status;
        this.checkedAt = checkedAt;
    }

    public String getService() {
        return service;
    }

    public String getStatus() {
        return status;
    }

    public long getCheckedAt() {
        return checkedAt;
    }
}
