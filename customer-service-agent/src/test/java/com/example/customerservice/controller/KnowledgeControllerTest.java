package com.example.customerservice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.customerservice.dto.KnowledgeEntryCreateRequest;
import com.example.customerservice.dto.KnowledgeEntryListResponse;
import com.example.customerservice.dto.KnowledgeEntryResponse;
import com.example.customerservice.dto.KnowledgeOperationResponse;
import com.example.customerservice.dto.KnowledgeStatusResponse;
import com.example.customerservice.service.KnowledgeBaseService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KnowledgeController.class)
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeBaseService knowledgeBaseService;

    @Test
    void shouldReturnKnowledgeEntries() throws Exception {
        when(knowledgeBaseService.listEntries()).thenReturn(
            new KnowledgeEntryListResponse(
                List.of(
                    new KnowledgeEntryResponse(
                        "kb-1",
                        "退换货政策",
                        "7天无理由退货",
                        "seed",
                        "text",
                        1710000000000L,
                        1710000001000L
                    )
                ),
                1,
                1710000009999L
            )
        );

        mockMvc.perform(get("/api/knowledge/entries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.entries[0].entryId").value("kb-1"))
            .andExpect(jsonPath("$.entries[0].title").value("退换货政策"));
    }

    @Test
    void shouldCreateKnowledgeEntry() throws Exception {
        when(
            knowledgeBaseService.createEntry(org.mockito.ArgumentMatchers.any())
        ).thenReturn(
            new KnowledgeOperationResponse("知识条目已创建", "kb-1", 1710000010000L)
        );

        mockMvc.perform(
            post("/api/knowledge/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"新政策\",\"content\":\"这里是知识内容\"}")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("知识条目已创建"))
            .andExpect(jsonPath("$.entryId").value("kb-1"));
    }

    @Test
    void shouldDeleteKnowledgeEntry() throws Exception {
        when(knowledgeBaseService.deleteEntry("kb-1")).thenReturn(
            new KnowledgeOperationResponse("知识条目已删除", "kb-1", 1710000020000L)
        );

        mockMvc.perform(delete("/api/knowledge/entries/kb-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("知识条目已删除"))
            .andExpect(jsonPath("$.entryId").value("kb-1"));
    }

    @Test
    void shouldReturnKnowledgeStatus() throws Exception {
        when(knowledgeBaseService.getStatus()).thenReturn(
            new KnowledgeStatusResponse(
                true,
                5,
                1710000030000L,
                1710000040000L,
                "知识库已刷新",
                1710000050000L
            )
        );

        mockMvc.perform(get("/api/knowledge/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.initialized").value(true))
            .andExpect(jsonPath("$.totalEntries").value(5))
            .andExpect(jsonPath("$.lastOperationMessage").value("知识库已刷新"));
    }

    @Test
    void shouldRebuildKnowledgeBase() throws Exception {
        when(knowledgeBaseService.rebuildKnowledgeBase()).thenReturn(
            new KnowledgeOperationResponse("知识库刷新完成", null, 1710000060000L)
        );

        mockMvc.perform(post("/api/knowledge/rebuild"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("知识库刷新完成"));
    }
}
