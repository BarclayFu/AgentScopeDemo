package com.example.customerservice.controller;

import com.example.customerservice.dto.GraphEdgeResponse;
import com.example.customerservice.dto.GraphNodeResponse;
import com.example.customerservice.dto.GraphStatsResponse;
import com.example.customerservice.service.KnowledgeGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱API控制器
 *
 * 提供知识图谱的查询和管理接口：
 * - 统计信息查询
 * - 节点和边的分页查询
 * - 图谱清空（危险操作）
 *
 * 前端可通过这些接口获取图谱数据，进行可视化展示。
 */
@RestController
@RequestMapping("/api/graph")
public class GraphApiController {

    private final KnowledgeGraphService knowledgeGraphService;

    public GraphApiController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    /**
     * 获取图谱统计信息
     *
     * @return 包含节点总数和边总数的统计对象
     */
    @GetMapping("/stats")
    public GraphStatsResponse getStats() {
        return knowledgeGraphService.getStats();
    }

    /**
     * 分页获取图谱节点
     *
     * @param limit  每页返回的节点数，默认100
     * @param offset 偏移量，用于分页，默认0
     * @return 节点列表
     */
    @GetMapping("/nodes")
    public List<GraphNodeResponse> getNodes(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return knowledgeGraphService.getAllNodes(limit, offset);
    }

    /**
     * 分页获取图谱边
     *
     * @param limit  每页返回的边数，默认100
     * @param offset 偏移量，用于分页，默认0
     * @return 边列表
     */
    @GetMapping("/edges")
    public List<GraphEdgeResponse> getEdges(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return knowledgeGraphService.getAllEdges(limit, offset);
    }

    /**
     * 清空整个知识图谱
     *
     * 危险操作！会删除所有节点和边，且无法恢复。
     * 仅用于测试或初始化场景。
     *
     * @return 操作成功消息
     */
    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearGraph() {
        knowledgeGraphService.clearGraph();
        return ResponseEntity.ok(Map.of("message", "Graph cleared successfully"));
    }
}