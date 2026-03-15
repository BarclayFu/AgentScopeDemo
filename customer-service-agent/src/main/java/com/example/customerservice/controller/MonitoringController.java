package com.example.customerservice.controller;

import com.example.customerservice.dto.MonitoringResetResponse;
import com.example.customerservice.dto.MonitoringStatusResponse;
import com.example.customerservice.dto.MonitoringSummaryResponse;
import com.example.customerservice.service.ChatSessionService;
import com.example.customerservice.service.AgentMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 监控统计REST控制器
 * 提供监控信息的API接口
 */
@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "*")
public class MonitoringController {

    @Autowired
    private AgentMonitoringService monitoringService;

    @Autowired
    private ChatSessionService chatSessionService;

    /**
     * 获取监控统计信息
     */
    @GetMapping("/stats")
    public MonitoringSummaryResponse getStatistics() {
        return monitoringService.getSummary(
            chatSessionService.getActiveSessionCount()
        );
    }

    /**
     * 重置监控统计信息
     */
    @PostMapping("/reset")
    public MonitoringResetResponse resetStatistics() {
        return monitoringService.resetAndGetResponse();
    }

    /**
     * 获取应用状态
     */
    @GetMapping("/status")
    public MonitoringStatusResponse getStatus() {
        return monitoringService.getStatus();
    }
}
