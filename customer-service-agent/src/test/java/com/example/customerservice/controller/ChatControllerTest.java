package com.example.customerservice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.customerservice.dto.ChatMessageResult;
import com.example.customerservice.dto.HybridCitation;
import com.example.customerservice.service.AgentMonitoringService;
import com.example.customerservice.service.ChatSessionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatSessionService chatSessionService;

    @MockBean
    private AgentMonitoringService monitoringService;

    @Test
    void shouldReturnChatMessageWithHybridMetadata() throws Exception {
        when(
            chatSessionService.processUserMessageWithMetadata(
                "user001",
                "智能手表保修多久？"
            )
        ).thenReturn(
            new ChatMessageResult(
                "根据知识库检索结果和图谱关联信息，为您整理如下。",
                List.of(
                    new HybridCitation(
                        "vector_chunk",
                        "智能手表售后政策",
                        "智能手表支持两年保修。",
                        null,
                        null,
                        0.92
                    )
                ),
                "hybrid",
                "none",
                1710000000000L
            )
        );

        mockMvc.perform(
            post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"user001\",\"message\":\"智能手表保修多久？\"}")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("user001"))
            .andExpect(jsonPath("$.response").value("根据知识库检索结果和图谱关联信息，为您整理如下。"))
            .andExpect(jsonPath("$.retrievalMode").value("hybrid"))
            .andExpect(jsonPath("$.fallbackMode").value("none"))
            .andExpect(jsonPath("$.citations[0].type").value("vector_chunk"))
            .andExpect(jsonPath("$.citations[0].title").value("智能手表售后政策"));
    }
}
