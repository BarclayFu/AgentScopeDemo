package com.example.customerservice.service.retriever;

import com.example.customerservice.dto.GraphSearchResult;
import com.example.customerservice.dto.VectorSearchResult;
import com.example.customerservice.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class HybridRAGService {
    private static final Logger logger = LoggerFactory.getLogger(HybridRAGService.class);
    private static final double ALPHA = 0.5;

    private final KnowledgeBaseService knowledgeBaseService;
    private final GraphRAGRetriever graphRAGRetriever;

    public HybridRAGService(KnowledgeBaseService knowledgeBaseService, GraphRAGRetriever graphRAGRetriever) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.graphRAGRetriever = graphRAGRetriever;
    }

    public HybridSearchResult hybridSearch(String query, int limit) {
        CompletableFuture<VectorSearchResult> vectorFuture = CompletableFuture.supplyAsync(
            () -> knowledgeBaseService.searchKnowledgeBaseStructured(query, limit)
        );

        CompletableFuture<GraphSearchResult> graphFuture = CompletableFuture.supplyAsync(
            () -> graphRAGRetriever.search(query, limit)
        );

        VectorSearchResult vectorResult = vectorFuture.join();
        GraphSearchResult graphResult = graphFuture.join();

        return new HybridSearchResult(vectorResult, graphResult);
    }

    public VectorSearchResult searchVectorOnly(String query, int limit) {
        return knowledgeBaseService.searchKnowledgeBaseStructured(query, limit);
    }

    public GraphSearchResult searchGraphOnly(String query, int limit) {
        return graphRAGRetriever.search(query, limit);
    }

    public record HybridSearchResult(VectorSearchResult vectorResult, GraphSearchResult graphResult) {}
}