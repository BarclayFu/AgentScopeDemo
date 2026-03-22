package com.example.customerservice.service.retriever;

import com.example.customerservice.dto.*;
import com.example.customerservice.service.KnowledgeGraphService;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GraphRAGRetriever {
    private static final Logger logger = LoggerFactory.getLogger(GraphRAGRetriever.class);

    private static final Map<String, List<String>> KEYWORD_ENTITY_MAP = Map.of(
        "Product", List.of("产品", "商品", "东西", "型号"),
        "Service", List.of("保修", "质保", "维修", "退换", "退款", "退货", "换货", "服务"),
        "Order", List.of("订单", "单号"),
        "QA", List.of("怎么", "如何", "是什么", "为什么", "?", "？")
    );

    private static final int MAX_HOPS = 3;
    private static final double SCORE_THRESHOLD = 0.3;

    private final Driver driver;
    private final KnowledgeGraphService knowledgeGraphService;

    public GraphRAGRetriever(Driver driver, KnowledgeGraphService knowledgeGraphService) {
        this.driver = driver;
        this.knowledgeGraphService = knowledgeGraphService;
    }

    public GraphSearchResult search(String query, int limit) {
        Set<String> matchedEntityIds = new HashSet<>();
        matchedEntityIds.addAll(ruleBasedMatch(query));

        if (matchedEntityIds.isEmpty()) {
            matchedEntityIds.addAll(llmEntityLinking(query));
        }

        Map<String, Object> subgraph = buildSubgraph(matchedEntityIds, MAX_HOPS);
        String answer = generateAnswerContext(query, matchedEntityIds);

        return new GraphSearchResult(
            answer,
            buildRetrievedEntities(subgraph, query),
            (List<GraphNodeResponse>) subgraph.get("nodes"),
            (List<GraphEdgeResponse>) subgraph.get("edges")
        );
    }

    private Set<String> ruleBasedMatch(String query) {
        Set<String> matchedIds = new HashSet<>();
        try (Session session = driver.session()) {
            for (Map.Entry<String, List<String>> entry : KEYWORD_ENTITY_MAP.entrySet()) {
                String entityType = entry.getKey();
                for (String keyword : entry.getValue()) {
                    if (query.contains(keyword)) {
                        var result = session.run(
                            "MATCH (n:" + entityType + ") WHERE n.name CONTAINS $keyword RETURN id(n) as id",
                            Map.of("keyword", keyword)
                        );
                        for (Record record : result.list()) {
                            matchedIds.add(String.valueOf(record.get("id").asLong()));
                        }
                    }
                }
            }
        }
        return matchedIds;
    }

    private Set<String> llmEntityLinking(String query) {
        Set<String> matchedIds = new HashSet<>();
        logger.debug("LLM entity linking for query: {}", query);
        return matchedIds;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSubgraph(Set<String> entityIds, int hops) {
        Map<String, Object> subgraph = new HashMap<>();
        List<GraphNodeResponse> nodes = new ArrayList<>();
        List<GraphEdgeResponse> edges = new ArrayList<>();

        if (entityIds.isEmpty()) {
            subgraph.put("nodes", nodes);
            subgraph.put("edges", edges);
            return subgraph;
        }

        try (Session session = driver.session()) {
            String idsParam = entityIds.stream().collect(Collectors.joining(","));
            var result = session.run(
                "MATCH path = (n) WHERE id(n) IN [" + idsParam + "] " +
                "MATCH path = (n)-[r*1.." + hops + "]-(m) " +
                "WITH n, r, m, path " +
                "RETURN distinct n, r, m"
            );

            Set<String> seenNodes = new HashSet<>();
            Set<String> seenEdges = new HashSet<>();

            for (Record record : result.list()) {
                var n = record.get("n").asNode();
                var m = record.get("m").asNode();
                var rels = record.get("r").asList();

                String nId = String.valueOf(n.id());
                if (!seenNodes.contains(nId)) {
                    seenNodes.add(nId);
                    nodes.add(new GraphNodeResponse(nId, n.labels().iterator().next(), n.get("name").asString(), n.asMap()));
                }

                String mId = String.valueOf(m.id());
                if (!seenNodes.contains(mId)) {
                    seenNodes.add(mId);
                    nodes.add(new GraphNodeResponse(mId, m.labels().iterator().next(), m.get("name").asString(), m.asMap()));
                }

                for ( var rel : rels) {
                    String rId = String.valueOf(rel.id());
                    if (!seenEdges.contains(rId)) {
                        seenEdges.add(rId);
                        edges.add(new GraphEdgeResponse(rId, String.valueOf(rel.start().id()), String.valueOf(rel.end().id()), rel.type()));
                    }
                }
            }
        }

        subgraph.put("nodes", nodes);
        subgraph.put("edges", edges);
        return subgraph;
    }

    private List<RetrievedEntity> buildRetrievedEntities(Map<String, Object> subgraph, String query) {
        List<RetrievedEntity> entities = new ArrayList<>();
        List<GraphNodeResponse> nodes = (List<GraphNodeResponse>) subgraph.get("nodes");

        for (GraphNodeResponse node : nodes) {
            double score = calculateRelevanceScore(node, query);
            if (score >= SCORE_THRESHOLD) {
                List<RetrievedPath> paths = findPathsToNode(node.getId());
                entities.add(new RetrievedEntity(node.getId(), node.getType(), node.getName(), paths, score));
            }
        }
        return entities;
    }

    private double calculateRelevanceScore(GraphNodeResponse node, String query) {
        String name = node.getName().toLowerCase();
        String queryLower = query.toLowerCase();
        if (name.contains(queryLower)) return 1.0;
        for (String keyword : queryLower.split("")) {
            if (name.contains(keyword)) return 0.5;
        }
        return 0.1;
    }

    private List<RetrievedPath> findPathsToNode(String nodeId) {
        List<RetrievedPath> paths = new ArrayList<>();
        try (Session session = driver.session()) {
            var result = session.run(
                "MATCH path = (m)-[r*1..2]-(n) WHERE id(n) = $nodeId RETURN path, length(path) as hops LIMIT 5",
                Map.of("nodeId", Long.parseLong(nodeId))
            );
            for (Record record : result.list()) {
                String pathStr = record.get("path").asPath().toString();
                int hops = record.get("hops").asInt();
                paths.add(new RetrievedPath(pathStr, hops));
            }
        }
        return paths;
    }

    private String generateAnswerContext(String query, Set<String> entityIds) {
        StringBuilder context = new StringBuilder();
        try (Session session = driver.session()) {
            for (String id : entityIds) {
                var result = session.run(
                    "MATCH (n) WHERE id(n) = $id OPTIONAL MATCH (n)-[r]-(m) RETURN n.name as name, labels(n)[0] as type, collect({rel: type(r), target: m.name}) as connections",
                    Map.of("id", Long.parseLong(id))
                );
                var record = result.single();
                context.append(record.get("type").asString())
                       .append(": ")
                       .append(record.get("name").asString())
                       .append("\n");
            }
        }
        return context.toString();
    }
}