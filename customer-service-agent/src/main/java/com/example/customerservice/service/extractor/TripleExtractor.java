package com.example.customerservice.service.extractor;

import com.example.customerservice.service.KnowledgeGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TripleExtractor {
    private static final Logger logger = LoggerFactory.getLogger(TripleExtractor.class);

    private final RulePreprocessor rulePreprocessor;
    private final LLMTripleExtractor llmTripleExtractor;
    private final KnowledgeGraphService knowledgeGraphService;

    public TripleExtractor(RulePreprocessor rulePreprocessor, LLMTripleExtractor llmTripleExtractor, KnowledgeGraphService knowledgeGraphService) {
        this.rulePreprocessor = rulePreprocessor;
        this.llmTripleExtractor = llmTripleExtractor;
        this.knowledgeGraphService = knowledgeGraphService;
    }

    public void extractAndStore(String knowledgeEntryId, String title, String content) {
        String fullText = title + "\n" + content;
        List<Map<String, String>> triples = new ArrayList<>();

        // 1. Rule-based extraction
        Map<String, List<String>> preprocessed = rulePreprocessor.preprocess(fullText);

        // Convert preprocessed data to triples
        for (String product : preprocessed.getOrDefault("products", List.of())) {
            triples.add(Map.of("subject", product, "relation", "MENTIONS", "object", title));
        }

        for (String service : preprocessed.getOrDefault("services", List.of())) {
            triples.add(Map.of("subject", title, "relation", "HAS_SERVICE", "object", service));
        }

        for (Map<String, String> qa : preprocessed.getOrDefault("qas", List.of())) {
            triples.add(Map.of(
                "subject", qa.get("question"),
                "relation", "RELATED_TO",
                "object", qa.get("answer")
            ));
        }

        // 2. LLM-based extraction
        try {
            List<Map<String, String>> llmTriples = llmTripleExtractor.extractTriples(fullText);
            triples.addAll(llmTriples);
        } catch (Exception e) {
            logger.warn("LLM extraction failed for entry {}: {}", knowledgeEntryId, e.getMessage());
        }

        // 3. Store to Neo4j
        for (Map<String, String> triple : triples) {
            String subject = triple.get("subject");
            String relation = triple.get("relation");
            String object = triple.get("object");
            String subjectType = inferEntityType(subject, preprocessed);
            String objectType = inferEntityType(object, preprocessed);

            if (subjectType != null && objectType != null) {
                try {
                    knowledgeGraphService.addTriple(subject, subjectType, relation, object, objectType);
                    logger.debug("Stored triple: ({})-[:{}]->({})", subject, relation, object);
                } catch (Exception e) {
                    logger.warn("Failed to store triple: ({})-[:{}]->({})", subject, relation, object);
                }
            }
        }

        logger.info("Extracted and stored {} triples for entry {}", triples.size(), knowledgeEntryId);
    }

    private String inferEntityType(String entity, Map<String, List<String>> preprocessed) {
        if (preprocessed.getOrDefault("products", List.of()).contains(entity)) return "Product";
        if (preprocessed.getOrDefault("services", List.of()).contains(entity)) return "Service";
        if (preprocessed.getOrDefault("orders", List.of()).contains(entity)) return "Order";
        if (entity.contains("?") || entity.contains("如何") || entity.contains("怎么")) return "QA";
        return "Concept";
    }
}
