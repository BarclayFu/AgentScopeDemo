package com.example.customerservice.service.retriever;

import com.example.customerservice.dto.GraphSearchResult;
import com.example.customerservice.dto.HybridAnswerResult;
import com.example.customerservice.dto.HybridCitation;
import com.example.customerservice.dto.RetrievedChunk;
import com.example.customerservice.dto.RetrievedEntity;
import com.example.customerservice.dto.RetrievedPath;
import com.example.customerservice.dto.VectorSearchResult;
import com.example.customerservice.service.KnowledgeBaseService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 混合检索主链路编排服务
 *
 * 负责对向量检索和图谱检索结果做统一编排，
 * 为聊天主链路生成可直接返回的答案和引用信息。
 */
@Service
public class HybridAnswerService {

    private static final Logger logger = LoggerFactory.getLogger(
        HybridAnswerService.class
    );

    private static final int DEFAULT_LIMIT = 5;

    private final KnowledgeBaseService knowledgeBaseService;
    private final GraphRAGRetriever graphRAGRetriever;

    public HybridAnswerService(
        KnowledgeBaseService knowledgeBaseService,
        GraphRAGRetriever graphRAGRetriever
    ) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.graphRAGRetriever = graphRAGRetriever;
    }

    public HybridAnswerResult answerQuestion(String question) {
        VectorSearchResult vectorResult =
            knowledgeBaseService.searchKnowledgeBaseStructured(
                question,
                DEFAULT_LIMIT
            );

        GraphSearchResult graphResult = emptyGraphResult();
        boolean graphErrored = false;

        try {
            graphResult = graphRAGRetriever.search(question, DEFAULT_LIMIT);
        } catch (Exception exception) {
            graphErrored = true;
            logger.warn("GraphRAG检索失败，自动降级到向量检索", exception);
        }

        boolean hasVector =
            vectorResult != null &&
            vectorResult.getRetrievedChunks() != null &&
            !vectorResult.getRetrievedChunks().isEmpty();
        boolean hasGraph =
            graphResult != null &&
            graphResult.getRetrievedEntities() != null &&
            !graphResult.getRetrievedEntities().isEmpty();

        List<HybridCitation> citations = buildCitations(
            vectorResult,
            graphResult
        );
        String retrievalMode = determineRetrievalMode(hasVector, hasGraph);
        String fallbackMode = determineFallbackMode(
            hasVector,
            hasGraph,
            graphErrored
        );
        String answer = buildAnswer(
            vectorResult,
            graphResult,
            hasVector,
            hasGraph,
            fallbackMode
        );

        return new HybridAnswerResult(
            answer,
            citations,
            retrievalMode,
            fallbackMode
        );
    }

    private String buildAnswer(
        VectorSearchResult vectorResult,
        GraphSearchResult graphResult,
        boolean hasVector,
        boolean hasGraph,
        String fallbackMode
    ) {
        StringBuilder answer = new StringBuilder();

        if (hasVector && hasGraph) {
            answer.append("根据知识库检索和知识图谱分析，为您解答如下：\n\n");
            appendVectorSection(answer, vectorResult.getRetrievedChunks());
            appendGraphSection(answer, graphResult.getRetrievedEntities());
            return answer.toString().trim();
        }

        if (hasVector) {
            answer.append("根据知识库检索结果，为您整理如下：\n\n");
            appendVectorSection(answer, vectorResult.getRetrievedChunks());
            return answer.toString().trim();
        }

        if (hasGraph) {
            answer.append("根据知识图谱分析，为您找到以下关联信息：\n\n");
            appendGraphSection(answer, graphResult.getRetrievedEntities());
            return answer.toString().trim();
        }

        if ("vector_only_graph_error".equals(fallbackMode)) {
            return "抱歉，知识图谱服务暂时不可用，当前也没有从知识库中检索到足够信息。请稍后重试或联系人工客服。";
        }

        return "抱歉，当前知识库中没有找到与您问题直接相关的信息。您可以换一种说法，或联系人工客服获取帮助。";
    }

    private void appendVectorSection(
        StringBuilder answer,
        List<RetrievedChunk> chunks
    ) {
        answer.append("【知识库参考】\n");
        int idx = 1;
        for (RetrievedChunk chunk : chunks) {
            if (chunk.getContent() == null || chunk.getContent().isBlank()) {
                continue;
            }
            String source = chunk.getSource() != null ? chunk.getSource() : "未知来源";
            answer.append(idx++).append(". ").append(source).append("\n");
            answer.append("   ").append(shorten(chunk.getContent(), 200)).append("\n\n");
            if (idx > 3) break; // 最多3条
        }
    }

    private void appendGraphSection(
        StringBuilder answer,
        List<RetrievedEntity> entities
    ) {
        answer.append("【知识图谱关联】\n");
        int idx = 1;
        for (RetrievedEntity entity : entities) {
            if (entity.getEntityName() == null) continue;
            answer.append(idx++).append(". ");
            String type = entity.getEntityType() != null ? entity.getEntityType() : "未知类型";
            answer.append(entity.getEntityName())
                  .append("（")
                  .append(type)
                  .append("）");
            String pathSummary = summarizePath(entity.getRelations());
            if (!pathSummary.isBlank()) {
                answer.append("\n   关联：").append(pathSummary);
            }
            answer.append("\n\n");
            if (idx > 2) break; // 最多2条
        }
    }

    private List<HybridCitation> buildCitations(
        VectorSearchResult vectorResult,
        GraphSearchResult graphResult
    ) {
        List<HybridCitation> citations = new ArrayList<>();

        if (vectorResult != null && vectorResult.getRetrievedChunks() != null) {
            vectorResult
                .getRetrievedChunks()
                .stream()
                .filter(chunk -> chunk.getContent() != null && !chunk.getContent().isBlank())
                .limit(3)
                .forEach(chunk ->
                    citations.add(
                        new HybridCitation(
                            "vector_chunk",
                            chunk.getSource(),
                            shorten(chunk.getContent(), 120),
                            null,
                            null,
                            chunk.getScore()
                        )
                    )
                );
        }

        if (graphResult != null && graphResult.getRetrievedEntities() != null) {
            graphResult
                .getRetrievedEntities()
                .stream()
                .sorted(Comparator.comparingDouble(RetrievedEntity::getScore).reversed())
                .limit(2)
                .forEach(entity ->
                    citations.add(
                        new HybridCitation(
                            "graph_path",
                            null,
                            null,
                            entity.getEntityName(),
                            summarizePath(entity.getRelations()),
                            entity.getScore()
                        )
                    )
                );
        }

        return citations;
    }

    private String determineRetrievalMode(boolean hasVector, boolean hasGraph) {
        if (hasVector && hasGraph) {
            return "hybrid";
        }
        if (hasVector) {
            return "vector";
        }
        if (hasGraph) {
            return "graph";
        }
        return "none";
    }

    private String determineFallbackMode(
        boolean hasVector,
        boolean hasGraph,
        boolean graphErrored
    ) {
        if (graphErrored && hasVector) {
            return "vector_only_graph_error";
        }
        if (!hasGraph && hasVector) {
            return "vector_only_graph_empty";
        }
        if (hasGraph && !hasVector) {
            return "graph_only_vector_empty";
        }
        if (graphErrored) {
            return "no_hit_graph_error";
        }
        if (!hasVector && !hasGraph) {
            return "no_hit";
        }
        return "none";
    }

    private GraphSearchResult emptyGraphResult() {
        return new GraphSearchResult("", List.of(), List.of(), List.of());
    }

    private String summarizePath(List<RetrievedPath> paths) {
        if (paths == null || paths.isEmpty()) {
            return "";
        }

        return paths
            .stream()
            .map(RetrievedPath::getPath)
            .filter(path -> path != null && !path.isBlank())
            .map(path -> shorten(path, 120))
            .findFirst()
            .orElse("");
    }

    private String shorten(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
