package com.example.customerservice.controller;

import com.example.customerservice.dto.*;
import com.example.customerservice.service.retriever.HybridRAGService;
import com.example.customerservice.service.extractor.TripleExtractor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 对比实验API控制器
 *
 * 提供Vector RAG与GraphRAG的对比实验接口：
 * - 同一问题双路径搜索：同时执行两种检索方式，对比结果
 * - 三元组抽取预览：预览从文本中抽取的三元组
 *
 * 用于展示GraphRAG相比传统Vector RAG的优势。
 */
@RestController
@RequestMapping("/api/compare")
public class ComparisonController {

    /** 混合RAG服务，同时支持Vector和Graph检索 */
    private final HybridRAGService hybridRAGService;

    /** 三元组抽取器 */
    private final TripleExtractor tripleExtractor;

    public ComparisonController(HybridRAGService hybridRAGService, TripleExtractor tripleExtractor) {
        this.hybridRAGService = hybridRAGService;
        this.tripleExtractor = tripleExtractor;
    }

    /**
     * 对比搜索：同时执行Vector RAG和GraphRAG
     *
     * 接收用户问题，分别使用两种检索方式获取结果，
     * 返回并排的对比结果，供前端展示和比较。
     *
     * @param request 包含query（问题）和limit（结果数量限制）
     * @return 包含Vector RAG结果和GraphRAG结果的对比响应
     */
    @PostMapping("/search")
    public CompareSearchResponse compareSearch(@Valid @RequestBody CompareSearchRequest request) {
        // 调用混合RAG服务，同时获取两种检索结果
        HybridRAGService.HybridSearchResult result = hybridRAGService.hybridSearch(request.getQuery(), request.getLimit());
        return new CompareSearchResponse(request.getQuery(), result.vectorResult(), result.graphResult());
    }

    /**
     * 预览三元组抽取结果
     *
     * 输入一段文本，预览从中能抽取出的知识图谱三元组。
     * 用于在添加知识条目前进行预览和验证。
     *
     * @param request 包含content（文本内容）
     * @return 抽取出的三元组列表
     */
    @PostMapping("/extract")
    public ResponseEntity<TripleExtractResponse> extractTriples(@Valid @RequestBody TripleExtractRequest request) {
        // TODO: 实现三元组预览功能
        return ResponseEntity.ok(new TripleExtractResponse(List.of()));
    }
}