package com.example.customerservice.config;

import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.openai.OpenAITextEmbedding;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.store.InMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG相关组件配置
 * 提供EmbeddingModel与Knowledge的单例Bean
 */
@Configuration
public class RagConfig {

    @Value("${agentscope.embedding.api-key}")
    private String apiKey;

    @Value("${agentscope.embedding.base-url}")
    private String baseUrl;

    @Value("${agentscope.embedding.model}")
    private String embeddingModelName;

    @Value("${agentscope.embedding.dimensions:1024}")
    private int embeddingDimensions;

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
    public Knowledge knowledgeBase(EmbeddingModel embeddingModel) {
        return SimpleKnowledge.builder()
            .embeddingModel(embeddingModel)
            .embeddingStore(InMemoryStore.builder().dimensions(embeddingDimensions).build())
            .build();
    }
}
