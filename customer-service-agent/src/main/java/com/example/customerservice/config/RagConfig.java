package com.example.customerservice.config;

import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.openai.OpenAITextEmbedding;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.store.MilvusStore;
import io.milvus.v2.common.IndexParam;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG相关组件配置
 * 提供EmbeddingModel与Knowledge的单例Bean
 */
@Configuration
public class RagConfig {

    private static final Logger logger = LoggerFactory.getLogger(
        RagConfig.class
    );

    @Value("${agentscope.embedding.api-key}")
    private String apiKey;

    @Value("${agentscope.embedding.base-url}")
    private String baseUrl;

    @Value("${agentscope.embedding.model}")
    private String embeddingModelName;

    @Value("${agentscope.embedding.dimensions:1024}")
    private int embeddingDimensions;

    @Value("${milvus.uri}")
    private String milvusUri;

    @Value("${milvus.collection-name}")
    private String milvusCollectionName;

    @Value("${milvus.username:}")
    private String milvusUsername;

    @Value("${milvus.password:}")
    private String milvusPassword;

    @Value("${milvus.token:}")
    private String milvusToken;

    // Store reference to MilvusStore for cleanup
    private MilvusStore milvusStore;

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAITextEmbedding.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(embeddingModelName)
            .dimensions(embeddingDimensions)
            .build();
    }

    @Bean
    public Knowledge knowledgeBase(EmbeddingModel embeddingModel)
        throws Exception {
        // Configure Milvus store
        MilvusStore.Builder builder = MilvusStore.builder()
            .uri(milvusUri)
            .collectionName(milvusCollectionName)
            .dimensions(embeddingDimensions)
            .metricType(IndexParam.MetricType.COSINE); // 使用正确的导入路径

        // Add authentication if provided
        if (
            milvusUsername != null &&
            !milvusUsername.isEmpty() &&
            milvusPassword != null &&
            !milvusPassword.isEmpty()
        ) {
            builder.username(milvusUsername).password(milvusPassword);
        } else if (milvusToken != null && !milvusToken.isEmpty()) {
            builder.token(milvusToken);
        }

        // Build the Milvus store
        milvusStore = builder.build();

        return SimpleKnowledge.builder()
            .embeddingModel(embeddingModel)
            .embeddingStore(milvusStore)
            .build();
    }

    /**
     * Cleanup resources when the application shuts down
     */
    @PreDestroy
    public void cleanup() {
        if (milvusStore != null) {
            try {
                milvusStore.close();
            } catch (Exception e) {
                logger.warn("关闭Milvus store时发生异常", e);
            }
        }
    }
}
