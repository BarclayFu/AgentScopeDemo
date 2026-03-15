package com.example.customerservice.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.customerservice.dto.MonitoringResetResponse;
import com.example.customerservice.dto.MonitoringStatusResponse;
import com.example.customerservice.dto.MonitoringSummary;
import com.example.customerservice.dto.MonitoringSummaryResponse;
import com.example.customerservice.service.AgentMonitoringService;
import com.example.customerservice.service.ChatSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MonitoringController.class)
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentMonitoringService monitoringService;

    @MockBean
    private ChatSessionService chatSessionService;

    @Test
    void shouldReturnStructuredMonitoringSummary() throws Exception {
        when(chatSessionService.getActiveSessionCount()).thenReturn(3);
        when(monitoringService.getSummary(3)).thenReturn(
            new MonitoringSummaryResponse(
                new MonitoringSummary(3, 12, 7, 1, 250, 1710000000000L, 1710000005000L),
                1710000009999L
            )
        );

        mockMvc.perform(get("/api/monitoring/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.activeSessions").value(3))
            .andExpect(jsonPath("$.summary.totalMessages").value(12))
            .andExpect(jsonPath("$.summary.totalToolCalls").value(7))
            .andExpect(jsonPath("$.summary.errorCount").value(1))
            .andExpect(jsonPath("$.summary.avgResponseTimeMs").value(250))
            .andExpect(jsonPath("$.checkedAt").value(1710000009999L));
    }

    @Test
    void shouldReturnStructuredHealthStatus() throws Exception {
        when(monitoringService.getStatus()).thenReturn(
            new MonitoringStatusResponse("Customer Service Agent", "UP", 1710000011111L)
        );

        mockMvc.perform(get("/api/monitoring/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("Customer Service Agent"))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.checkedAt").value(1710000011111L));
    }

    @Test
    void shouldResetStatisticsWithJsonResponse() throws Exception {
        when(monitoringService.resetAndGetResponse()).thenReturn(
            new MonitoringResetResponse("Statistics reset successfully", 1710000022222L)
        );

        mockMvc.perform(post("/api/monitoring/reset"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Statistics reset successfully"))
            .andExpect(jsonPath("$.checkedAt").value(1710000022222L));

        verify(monitoringService).resetAndGetResponse();
    }
}
