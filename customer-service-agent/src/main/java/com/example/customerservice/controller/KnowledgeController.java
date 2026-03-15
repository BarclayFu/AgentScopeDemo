package com.example.customerservice.controller;

import com.example.customerservice.dto.KnowledgeEntryCreateRequest;
import com.example.customerservice.dto.KnowledgeEntryListResponse;
import com.example.customerservice.dto.KnowledgeOperationResponse;
import com.example.customerservice.dto.KnowledgeStatusResponse;
import com.example.customerservice.service.KnowledgeBaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 知识库管理控制器
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping("/entries")
    public KnowledgeEntryListResponse listEntries() {
        return knowledgeBaseService.listEntries();
    }

    @PostMapping("/entries")
    public KnowledgeOperationResponse createEntry(
        @RequestBody KnowledgeEntryCreateRequest request
    ) throws IOException {
        return knowledgeBaseService.createEntry(request);
    }

    @DeleteMapping("/entries/{entryId}")
    public KnowledgeOperationResponse deleteEntry(@PathVariable String entryId) throws IOException {
        return knowledgeBaseService.deleteEntry(entryId);
    }

    @PostMapping("/rebuild")
    public KnowledgeOperationResponse rebuildKnowledgeBase() throws IOException {
        return knowledgeBaseService.rebuildKnowledgeBase();
    }

    @GetMapping("/status")
    public KnowledgeStatusResponse getStatus() {
        return knowledgeBaseService.getStatus();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<KnowledgeOperationResponse> handleBadRequest(
        IllegalArgumentException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new KnowledgeOperationResponse(
                exception.getMessage(),
                null,
                System.currentTimeMillis()
            )
        );
    }
}
