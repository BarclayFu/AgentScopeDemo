package com.example.customerservice.controller;

import com.example.customerservice.dto.*;
import com.example.customerservice.service.retriever.HybridRAGService;
import com.example.customerservice.service.extractor.TripleExtractor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compare")
public class ComparisonController {
    private final HybridRAGService hybridRAGService;
    private final TripleExtractor tripleExtractor;

    public ComparisonController(HybridRAGService hybridRAGService, TripleExtractor tripleExtractor) {
        this.hybridRAGService = hybridRAGService;
        this.tripleExtractor = tripleExtractor;
    }

    @PostMapping("/search")
    public CompareSearchResponse compareSearch(@Valid @RequestBody CompareSearchRequest request) {
        HybridRAGService.HybridSearchResult result = hybridRAGService.hybridSearch(request.getQuery(), request.getLimit());
        return new CompareSearchResponse(request.getQuery(), result.vectorResult(), result.graphResult());
    }

    @PostMapping("/extract")
    public ResponseEntity<TripleExtractResponse> extractTriples(@Valid @RequestBody TripleExtractRequest request) {
        return ResponseEntity.ok(new TripleExtractResponse(List.of()));
    }
}