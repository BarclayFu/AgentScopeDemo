package com.example.customerservice.controller;

import com.example.customerservice.service.AgentMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 监控统计REST控制器
 * 提供监控信息的API接口
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    @Autowired
    private AgentMonitoringService monitoringService;

    /**
     * 获取监控统计信息
     */
    @GetMapping("/stats")
    public String getStatistics() {
        return monitoringService.getStatistics();
    }

    /**
     * 重置监控统计信息
     */
    @PostMapping("/reset")
    public String resetStatistics() {
        monitoringService.resetStatistics();
        return "Statistics reset successfully";
    }

    /**
     * 获取应用状态
     */
    @GetMapping("/status")
    public String getStatus() {
        return "Customer Service Agent is running. " + monitoringService.getStatistics();
    }
}
