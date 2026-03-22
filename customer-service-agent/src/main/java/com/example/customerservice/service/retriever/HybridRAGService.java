package com.example.customerservice.service.retriever;

import com.example.customerservice.dto.GraphSearchResult;
import com.example.customerservice.dto.VectorSearchResult;
import com.example.customerservice.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 混合RAG服务
 *
 * 整合Vector RAG（向量检索）和GraphRAG（图谱检索）的混合检索服务。
 *
 * 检索策略：
 * - 混合搜索：同时执行两种检索，利用两者的优势
 * - 向量检索：基于语义相似度的传统RAG方式
 * - 图谱检索：基于知识图谱结构的关系推理
 *
 * ALPHA参数控制两种检索结果的融合权重：
 * - ALPHA = 0.5 表示两种检索同等重要
 *
 * @see GraphRAGRetriever
 * @see KnowledgeBaseService
 */
@Service
public class HybridRAGService {

    private static final Logger logger = LoggerFactory.getLogger(HybridRAGService.class);

    /**
     * 混合搜索的权重因子
     *
     * ALPHA = 0.5 表示：
     * - 向量检索结果占50%权重
     * - 图谱检索结果占50%权重
     *
     * 公式：Score = α * VectorScore + (1-α) * GraphScore
     */
    private static final double ALPHA = 0.5;

    /** 知识库服务（向量检索） */
    private final KnowledgeBaseService knowledgeBaseService;

    /** GraphRAG检索器（图谱检索） */
    private final GraphRAGRetriever graphRAGRetriever;

    public HybridRAGService(KnowledgeBaseService knowledgeBaseService, GraphRAGRetriever graphRAGRetriever) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.graphRAGRetriever = graphRAGRetriever;
    }

    /**
     * 混合搜索
     *
     * 同时执行向量检索和图谱检索，返回两者的结果。
     * 两种检索并行执行，提高响应速度。
     *
     * @param query 用户查询
     * @param limit 返回结果的数量限制
     * @return 包含两种检索结果的混合搜索结果
     */
    public HybridSearchResult hybridSearch(String query, int limit) {
        // 并行执行两种检索任务
        CompletableFuture<VectorSearchResult> vectorFuture = CompletableFuture.supplyAsync(
            () -> knowledgeBaseService.searchKnowledgeBaseStructured(query, limit)
        );

        CompletableFuture<GraphSearchResult> graphFuture = CompletableFuture.supplyAsync(
            () -> graphRAGRetriever.search(query, limit)
        );

        // 等待两种检索都完成
        VectorSearchResult vectorResult = vectorFuture.join();
        GraphSearchResult graphResult = graphFuture.join();

        return new HybridSearchResult(vectorResult, graphResult);
    }

    /**
     * 仅使用向量检索
     *
     * 方便单独测试和对比Vector RAG的效果。
     *
     * @param query 用户查询
     * @param limit 返回结果的数量限制
     * @return 向量检索结果
     */
    public VectorSearchResult searchVectorOnly(String query, int limit) {
        return knowledgeBaseService.searchKnowledgeBaseStructured(query, limit);
    }

    /**
     * 仅使用图谱检索
     *
     * 方便单独测试和对比GraphRAG的效果。
     *
     * @param query 用户查询
     * @param limit 返回结果的数量限制
     * @return 图谱检索结果
     */
    public GraphSearchResult searchGraphOnly(String query, int limit) {
        return graphRAGRetriever.search(query, limit);
    }

    /**
     * 混合搜索结果记录
     *
     * 包含向量检索和图谱检索两种结果，供调用方进行展示或进一步处理。
     *
     * @param vectorResult 向量检索结果
     * @param graphResult  图谱检索结果
     */
    public record HybridSearchResult(VectorSearchResult vectorResult, GraphSearchResult graphResult) {}
}