package com.example.customerservice.controller;

import com.example.customerservice.dto.GraphEdgeResponse;
import com.example.customerservice.dto.GraphNodeResponse;
import com.example.customerservice.dto.GraphStatsResponse;
import com.example.customerservice.service.KnowledgeGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
public class GraphApiController {
    private final KnowledgeGraphService knowledgeGraphService;

    public GraphApiController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @GetMapping("/stats")
    public GraphStatsResponse getStats() {
        return knowledgeGraphService.getStats();
    }

    @GetMapping("/nodes")
    public List<GraphNodeResponse> getNodes(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return knowledgeGraphService.getAllNodes(limit, offset);
    }

    @GetMapping("/edges")
    public List<GraphEdgeResponse> getEdges(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return knowledgeGraphService.getAllEdges(limit, offset);
    }

    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearGraph() {
        knowledgeGraphService.clearGraph();
        return ResponseEntity.ok(Map.of("message", "Graph cleared successfully"));
    }
}