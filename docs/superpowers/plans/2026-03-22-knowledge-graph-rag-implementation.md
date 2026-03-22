# Knowledge Graph RAG Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate Neo4j knowledge graph with existing Vector RAG, enabling graph-based retrieval with visualization and comparison UI

**Architecture:** Add Neo4j as graph storage alongside Milvus, implement hybrid knowledge extraction (rule-based + LLM) to populate the graph, create GraphRAG retriever that works in parallel with existing vector retrieval, and expose graph data + comparison APIs for frontend visualization.

**Tech Stack:** Neo4j Java Driver, Spring Boot, Vue 3, Cytoscape.js, MiniMax M2.7 LLM

---

## File Structure

### Backend (Spring Boot)

**New Files:**
```
customer-service-agent/src/main/java/com/example/customerservice/
├── config/
│   └── Neo4jConfig.java                    # Neo4j connection & session management
├── service/
│   ├── KnowledgeGraphService.java          # Graph CRUD operations
│   ├── extractor/
│   │   ├── RulePreprocessor.java          # Rule-based text preprocessing
│   │   ├── LLMTripleExtractor.java        # LLM-based triple extraction
│   │   └── TripleExtractor.java           # Combined extraction pipeline
│   └── retriever/
│       ├── GraphRAGRetriever.java         # Graph-based retrieval
│       └── HybridRAGService.java          # Parallel Vector + Graph search
├── controller/
│   ├── GraphApiController.java             # /api/graph/* endpoints
│   └── ComparisonController.java          # /api/compare/* endpoints
└── dto/
    ├── GraphStatsResponse.java            # Node/edge counts
    ├── GraphNodeResponse.java              # Single node data
    ├── GraphEdgeResponse.java             # Single edge data
    ├── CompareSearchRequest.java          # {query, limit}
    ├── CompareSearchResponse.java         # {vectorResult, graphResult}
    └── TripleExtractRequest.java          # {content}
```

**Modified Files:**
```
customer-service-agent/
├── pom.xml                                 # Add neo4j-java-driver dependency
├── src/main/resources/application.yml     # Add neo4j.* config
└── src/main/java/.../service/KnowledgeBaseService.java  # Integrate extraction
```

### Frontend (Vue 3)

**New Files:**
```
frontend/src/
├── views/
│   └── GraphView.vue                      # Graph visualization page
│   └── CompareView.vue                    # Comparison experiment page
├── api/
│   └── index.js                           # Add graph/compare API functions
├── stores/
│   └── graph.js                          # Graph state (nodes, edges, selected)
└── router/index.js                       # Add /graph and /compare routes
```

**Modified Files:**
```
frontend/
├── package.json                            # Add cytoscape dependency
└── src/layouts/AppLayout.vue             # Add navigation items
```

---

## Phase 1: Neo4j Integration

### Task 1: Add Neo4j dependency

**Files:**
- Modify: `customer-service-agent/pom.xml`

- [ ] **Step 1: Add Neo4j Java Driver dependency**

Add after existing dependencies:
```xml
<dependency>
    <groupId>org.neo4j.driver</groupId>
    <artifactId>neo4j-java-driver</artifactId>
    <version>5.24.0</version>
</dependency>
```

### Task 2: Neo4j Configuration

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/config/Neo4jConfig.java`
- Modify: `customer-service-agent/src/main/resources/application.yml`

- [ ] **Step 1: Add Neo4j config to application.yml**

Add after milvus config:
```yaml
neo4j:
  uri: bolt://localhost:7687
  username: neo4j
  password: ${NEO4J_PASSWORD:}
  database: neo4j
```

- [ ] **Step 2: Create Neo4jConfig.java**

```java
package com.example.customerservice.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {
    private static final Logger logger = LoggerFactory.getLogger(Neo4jConfig.class);

    @Value("${neo4j.uri}")
    private String neo4jUri;

    @Value("${neo4j.username}")
    private String username;

    @Value("${neo4j.password}")
    private String password;

    @Value("${neo4j.database:neo4j}")
    private String database;

    @Bean
    public Driver neo4jDriver() {
        logger.info("Initializing Neo4j driver: {}", neo4jUri);
        return GraphDatabase.driver(neo4jUri, AuthTokens.basic(username, password));
    }

    public void close() {
        if (neo4jDriver != null) {
            neo4jDriver.close();
        }
    }

    @PreDestroy
    public void destroy() {
        close();
    }
}
```

### Task 3: KnowledgeGraphService - Basic Graph Operations

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/service/KnowledgeGraphService.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/dto/GraphStatsResponse.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/dto/GraphNodeResponse.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/dto/GraphEdgeResponse.java`

- [ ] **Step 1: Create GraphStatsResponse.java**

```java
package com.example.customerservice.dto;

public class GraphStatsResponse {
    private final long nodeCount;
    private final long edgeCount;
    private final long timestamp;

    public GraphStatsResponse(long nodeCount, long edgeCount) {
        this.nodeCount = nodeCount;
        this.edgeCount = edgeCount;
        this.timestamp = System.currentTimeMillis();
    }

    public long getNodeCount() { return nodeCount; }
    public long getEdgeCount() { return edgeCount; }
    public long getTimestamp() { return timestamp; }
}
```

- [ ] **Step 2: Create GraphNodeResponse.java**

```java
package com.example.customerservice.dto;

import java.util.Map;

public class GraphNodeResponse {
    private final String id;
    private final String type;
    private final String name;
    private final Map<String, Object> properties;

    public GraphNodeResponse(String id, String type, String name, Map<String, Object> properties) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.properties = properties;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public Map<String, Object> getProperties() { return properties; }
}
```

- [ ] **Step 3: Create GraphEdgeResponse.java**

```java
package com.example.customerservice.dto;

public class GraphEdgeResponse {
    private final String id;
    private final String source;
    private final String target;
    private final String relation;

    public GraphEdgeResponse(String id, String source, String target, String relation) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.relation = relation;
    }

    public String getId() { return id; }
    public String getSource() { return source; }
    public String getTarget() { return target; }
    public String getRelation() { return relation; }
}
```

- [ ] **Step 4: Create KnowledgeGraphService.java**

```java
package com.example.customerservice.service;

import com.example.customerservice.dto.GraphEdgeResponse;
import com.example.customerservice.dto.GraphNodeResponse;
import com.example.customerservice.dto.GraphStatsResponse;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KnowledgeGraphService {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphService.class);
    private final Driver driver;

    public KnowledgeGraphService(Driver driver) {
        this.driver = driver;
    }

    public GraphStatsResponse getStats() {
        try (Session session = driver.session()) {
            long nodeCount = session.run("MATCH (n) RETURN count(n) as cnt").single().get("cnt").asLong();
            long edgeCount = session.run("MATCH ()-[r]->() RETURN count(r) as cnt").single().get("cnt").asLong();
            return new GraphStatsResponse(nodeCount, edgeCount);
        }
    }

    public List<GraphNodeResponse> getAllNodes(int limit, int offset) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (n) RETURN id(n) as id, labels(n)[0] as type, n.name as name, properties(n) as props ORDER BY id SKIP $offset LIMIT $limit",
                Map.of("offset", offset, "limit", limit)
            );
            List<GraphNodeResponse> nodes = new ArrayList<>();
            for (Record record : result.list()) {
                String nodeId = String.valueOf(record.get("id").asLong());
                String type = record.get("type").asString();
                String name = record.get("name").isNull() ? "" : record.get("name").asString();
                nodes.add(new GraphNodeResponse(nodeId, type, name, record.get("props").asMap()));
            }
            return nodes;
        }
    }

    public List<GraphEdgeResponse> getAllEdges(int limit, int offset) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (a)-[r]->(b) RETURN id(r) as id, id(a) as source, id(b) as target, type(r) as relation ORDER BY id SKIP $offset LIMIT $limit",
                Map.of("offset", offset, "limit", limit)
            );
            List<GraphEdgeResponse> edges = new ArrayList<>();
            for (Record record : result.list()) {
                edges.add(new GraphEdgeResponse(
                    String.valueOf(record.get("id").asLong()),
                    String.valueOf(record.get("source").asLong()),
                    String.valueOf(record.get("target").asLong()),
                    record.get("relation").asString()
                ));
            }
            return edges;
        }
    }

    public void addTriple(String subject, String subjectType, String relation, String object, String objectType) {
        try (Session session = driver.session()) {
            session.run(
                "MERGE (s:$subjectType {name: $subject}) MERGE (o:$objectType {name: $object}) MERGE (s)-[r:$relation]->(o)",
                Map.of("subject", subject, "subjectType", subjectType, "relation", relation, "object", object, "objectType", objectType)
            );
        }
    }

    public void clearGraph() {
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
        }
    }

    public SubgraphResponse getSubgraph(String centerNodeId, int hops) {
        try (Session session = driver.session()) {
            List<GraphNodeResponse> nodes = new ArrayList<>();
            List<GraphEdgeResponse> edges = new ArrayList<>();

            Result result = session.run(
                "MATCH (center) WHERE id(center) = $centerId " +
                "MATCH path = (center)-[r*1.." + hops + "]-(connected) " +
                "WITH center, connected, r " +
                "RETURN distinct center, connected, r",
                Map.of("centerId", Long.parseLong(centerNodeId))
            );

            Set<String> seenNodes = new HashSet<>();
            Set<String> seenEdges = new HashSet<>();

            for (Record record : result.list()) {
                var center = record.get("center").asNode();
                var connected = record.get("connected").asNode();
                var rels = record.get("r").asList();

                String cId = String.valueOf(center.id());
                if (!seenNodes.contains(cId)) {
                    seenNodes.add(cId);
                    nodes.add(new GraphNodeResponse(cId, center.labels().iterator().next(),
                        center.get("name").asString(), center.asMap()));
                }

                String connId = String.valueOf(connected.id());
                if (!seenNodes.contains(connId)) {
                    seenNodes.add(connId);
                    nodes.add(new GraphNodeResponse(connId, connected.labels().iterator().next(),
                        connected.get("name").asString(), connected.asMap()));
                }

                for (var rel : rels) {
                    String rId = String.valueOf(rel.id());
                    if (!seenEdges.contains(rId)) {
                        seenEdges.add(rId);
                        edges.add(new GraphEdgeResponse(rId,
                            String.valueOf(rel.start().id()),
                            String.valueOf(rel.end().id()),
                            rel.type()));
                    }
                }
            }

            return new SubgraphResponse(nodes, edges);
        }
    }

    public PathResponse findPath(String sourceId, String targetId) {
        try (Session session = driver.session()) {
            List<GraphNodeResponse> nodes = new ArrayList<>();
            List<GraphEdgeResponse> edges = new ArrayList<>();

            Result result = session.run(
                "MATCH path = shortestPath((source)-[r*]-(target)) " +
                "WHERE id(source) = $sourceId AND id(target) = $targetId " +
                "RETURN path, length(path) as distance",
                Map.of("sourceId", Long.parseLong(sourceId), "targetId", Long.parseLong(targetId))
            );

            for (Record record : result.list()) {
                var path = record.get("path").asPath();
                int distance = record.get("distance").asInt();

                for (var node : path.nodes()) {
                    nodes.add(new GraphNodeResponse(String.valueOf(node.id()),
                        node.labels().iterator().next(),
                        node.get("name").asString(),
                        node.asMap()));
                }

                for (var rel : path.relationships()) {
                    edges.add(new GraphEdgeResponse(String.valueOf(rel.id()),
                        String.valueOf(rel.start().id()),
                        String.valueOf(rel.end().id()),
                        rel.type()));
                }

                return new PathResponse(nodes, edges, distance);
            }

            return new PathResponse(nodes, edges, -1);
        }
    }
}
```

### Task 4: GraphApiController

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/controller/GraphApiController.java`

- [ ] **Step 1: Create GraphApiController.java**

```java
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

    @PostMapping("/subgraph")
    public SubgraphResponse getSubgraph(@RequestBody SubgraphRequest request) {
        return knowledgeGraphService.getSubgraph(request.getCenterNodeId(), request.getHopCount());
    }

    @PostMapping("/path")
    public PathResponse findPath(@RequestBody PathRequest request) {
        return knowledgeGraphService.findPath(request.getSourceId(), request.getTargetId());
    }
}

// DTOs for new endpoints
class SubgraphRequest {
    private String centerNodeId;
    private int hopCount = 2;

    public String getCenterNodeId() { return centerNodeId; }
    public int getHopCount() { return hopCount; }
}

class PathRequest {
    private String sourceId;
    private String targetId;

    public String getSourceId() { return sourceId; }
    public String getTargetId() { return targetId; }
}

class SubgraphResponse {
    private final List<GraphNodeResponse> nodes;
    private final List<GraphEdgeResponse> edges;

    public SubgraphResponse(List<GraphNodeResponse> nodes, List<GraphEdgeResponse> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<GraphNodeResponse> getNodes() { return nodes; }
    public List<GraphEdgeResponse> getEdges() { return edges; }
}

class PathResponse {
    private final List<GraphNodeResponse> nodes;
    private final List<GraphEdgeResponse> edges;
    private final int distance;

    public PathResponse(List<GraphNodeResponse> nodes, List<GraphEdgeResponse> edges, int distance) {
        this.nodes = nodes;
        this.edges = edges;
        this.distance = distance;
    }

    public List<GraphNodeResponse> getNodes() { return nodes; }
    public List<GraphEdgeResponse> getEdges() { return edges; }
    public int getDistance() { return distance; }
}
```

---

## Phase 2: Knowledge Extraction Pipeline

### Task 5: Rule Preprocessor

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/service/extractor/RulePreprocessor.java`

- [ ] **Step 1: Create RulePreprocessor.java**

```java
package com.example.customerservice.service.extractor;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RulePreprocessor {

    private static final Pattern QA_PATTERN = Pattern.compile("\\?(.*)\\n[答案：:](.*)");
    private static final Pattern PRODUCT_PATTERN = Pattern.compile("[产品商品][：:]([^\n，,。]+)");
    private static final Pattern ORDER_PATTERN = Pattern.compile("[订单单号][：:]([A-Z0-9]+)");
    private static final Pattern SERVICE_PATTERN = Pattern.compile("(保修|质保|维修|退换|退款|退货|换货)[^。，,\n]*(?:的|是|方式|政策|条件|范围)?");

    public Map<String, List<String>> preprocess(String text) {
        Map<String, List<String>> extracted = new HashMap<>();
        extracted.put("products", extractProducts(text));
        extracted.put("orders", extractOrders(text));
        extracted.put("services", extractServices(text));
        extracted.put("qas", extractQAs(text));
        return extracted;
    }

    private List<String> extractProducts(String text) {
        List<String> products = new ArrayList<>();
        Matcher matcher = PRODUCT_PATTERN.matcher(text);
        while (matcher.find()) {
            products.add(matcher.group(1).trim());
        }
        return products;
    }

    private List<String> extractOrders(String text) {
        List<String> orders = new ArrayList<>();
        Matcher matcher = ORDER_PATTERN.matcher(text);
        while (matcher.find()) {
            orders.add(matcher.group(1).trim());
        }
        return orders;
    }

    private List<String> extractServices(String text) {
        List<String> services = new ArrayList<>();
        Matcher matcher = SERVICE_PATTERN.matcher(text);
        while (matcher.find()) {
            services.add(matcher.group(1).trim());
        }
        return services;
    }

    private List<Map<String, String>> extractQAs(String text) {
        List<Map<String, String>> qas = new ArrayList<>();
        Matcher matcher = QA_PATTERN.matcher(text);
        while (matcher.find()) {
            Map<String, String> qa = new HashMap<>();
            qa.put("question", matcher.group(1).trim());
            qa.put("answer", matcher.group(2).trim());
            qas.add(qa);
        }
        return qas;
    }
}
```

### Task 6: LLM Triple Extractor

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/service/extractor/LLMTripleExtractor.java`

- [ ] **Step 1: Create LLMTripleExtractor.java**

```java
package com.example.customerservice.service.extractor;

import com.example.customerservice.dto.ChatRequest;
import com.example.customerservice.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LLMTripleExtractor {
    private static final Logger logger = LoggerFactory.getLogger(LLMTripleExtractor.class);
    private static final String SYSTEM_PROMPT = """
            你是一个知识图谱抽取专家。你的任务是从给定文本中提取实体和关系，输出JSON格式的三元组。

            实体类型: Product, Service, QA, Concept
            关系类型: BELONGS_TO, HAS_SERVICE, RELATED_TO, REFERENCES, MENTIONS

            要求:
            1. 实体名称应简洁明了
            2. 关系必须来自上述关系类型列表
            3. 每个三元组表示 (实体A, 关系, 实体B)
            4. 输出纯JSON数组，不要包含其他文字
            """;

    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LLMTripleExtractor(ChatService chatService) {
        this.chatService = chatService;
    }

    public List<Map<String, String>> extractTriples(String text) {
        String userPrompt = "输入文本: " + text + "\n\n请提取三元组，输出JSON数组:";
        ChatRequest request = new ChatRequest("system", SYSTEM_PROMPT + "\n\n" + userPrompt);
        String response = chatService.getSyncResponseWithoutHistory(List.of(request));

        return parseTriples(response);
    }

    private List<Map<String, String>> parseTriples(String response) {
        List<Map<String, String>> triples = new ArrayList<>();
        try {
            // Try to extract JSON array from response
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start != -1 && end != -1) {
                String jsonArray = response.substring(start, end + 1);
                JsonNode node = objectMapper.readTree(jsonArray);
                if (node.isArray()) {
                    for (JsonNode item : node) {
                        Map<String, String> triple = new HashMap<>();
                        triple.put("subject", item.has("subject") ? item.get("subject").asText() : item.get("s").asText());
                        triple.put("relation", item.has("relation") ? item.get("relation").asText() : item.get("p").asText());
                        triple.put("object", item.has("object") ? item.get("object").asText() : item.get("o").asText());
                        triples.add(triple);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse LLM response as JSON: {}", response, e);
        }
        return triples;
    }
}
```

### Task 7: Combined Triple Extractor

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/service/extractor/TripleExtractor.java`

- [ ] **Step 1: Create TripleExtractor.java**

```java
package com.example.customerservice.service.extractor;

import com.example.customerservice.service.KnowledgeGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TripleExtractor {
    private static final Logger logger = LoggerFactory.getLogger(TripleExtractor.class);

    private static final Map<String, String> ENTITY_TYPE_MAP = Map.of(
        "Product", "Product",
        "Service", "Service",
        "QA", "QA",
        "Concept", "Concept"
    );

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
        List<Map<String, String>> llmTriples = llmTripleExtractor.extractTriples(fullText);
        triples.addAll(llmTriples);

        // 3. Store to Neo4j
        for (Map<String, String> triple : triples) {
            String subject = triple.get("subject");
            String relation = triple.get("relation");
            String object = triple.get("object");
            String subjectType = inferEntityType(subject, preprocessed);
            String objectType = inferEntityType(object, preprocessed);

            if (subjectType != null && objectType != null) {
                knowledgeGraphService.addTriple(subject, subjectType, relation, object, objectType);
                logger.debug("Stored triple: ({})-[:{}]->({})", subject, relation, object);
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
```

### Task 8: Integrate Extraction into KnowledgeBaseService

**Files:**
- Modify: `customer-service-agent/src/main/java/com/example/customerservice/service/KnowledgeBaseService.java`

- [ ] **Step 1: Add TripleExtractor integration**

Add field:
```java
private final TripleExtractor tripleExtractor;
```

Add to constructor:
```java
this.tripleExtractor = tripleExtractor;
```

Add after entry is created in `createEntry`:
```java
// Extract triples to Neo4j
tripleExtractor.extractAndStore(entry.getEntryId(), entry.getTitle(), entry.getContent());
```

---

## Phase 3: GraphRAG Retrieval

### Task 9: GraphRAG Retriever

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/service/retriever/GraphRAGRetriever.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/dto/GraphSearchResult.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/dto/RetrievedEntity.java`

- [ ] **Step 1: Create RetrievedEntity.java**

```java
package com.example.customerservice.dto;

import java.util.List;

public class RetrievedEntity {
    private final String entityId;
    private final String entityType;
    private final String entityName;
    private final List<RetrievedPath> relations;
    private final double score;

    public RetrievedEntity(String entityId, String entityType, String entityName, List<RetrievedPath> relations, double score) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.entityName = entityName;
        this.relations = relations;
        this.score = score;
    }

    public String getEntityId() { return entityId; }
    public String getEntityType() { return entityType; }
    public String getEntityName() { return entityName; }
    public List<RetrievedPath> getRelations() { return relations; }
    public double getScore() { return score; }
}

class RetrievedPath {
    private final String path;
    private final int hopCount;

    public RetrievedPath(String path, int hopCount) {
        this.path = path;
        this.hopCount = hopCount;
    }

    public String getPath() { return path; }
    public int getHopCount() { return hopCount; }
}
```

- [ ] **Step 2: Create GraphSearchResult.java**

```java
package com.example.customerservice.dto;

import java.util.List;

public class GraphSearchResult {
    private final String answer;
    private final List<RetrievedEntity> retrievedEntities;
    private final List<GraphNodeResponse> subgraphNodes;
    private final List<GraphEdgeResponse> subgraphEdges;

    public GraphSearchResult(String answer, List<RetrievedEntity> retrievedEntities,
                             List<GraphNodeResponse> subgraphNodes, List<GraphEdgeResponse> subgraphEdges) {
        this.answer = answer;
        this.retrievedEntities = retrievedEntities;
        this.subgraphNodes = subgraphNodes;
        this.subgraphEdges = subgraphEdges;
    }

    public String getAnswer() { return answer; }
    public List<RetrievedEntity> getRetrievedEntities() { return retrievedEntities; }
    public List<GraphNodeResponse> getSubgraphNodes() { return subgraphNodes; }
    public List<GraphEdgeResponse> getSubgraphEdges() { return subgraphEdges; }
}
```

- [ ] **Step 3: Create GraphRAGRetriever.java**

```java
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        // 1. Rule-based entity matching
        Set<String> matchedEntityIds = new HashSet<>();
        matchedEntityIds.addAll(ruleBasedMatch(query));

        // 2. LLM entity linking fallback - if rule-based didn't find enough, use LLM
        if (matchedEntityIds.isEmpty()) {
            logger.info("Rule-based matching found no entities, falling back to LLM entity linking");
            matchedEntityIds.addAll(llmEntityLinking(query));
        }

        // 3. Build subgraph from matched entities
        Map<String, Object> subgraph = buildSubgraph(matchedEntityIds, MAX_HOPS);

        // 4. Generate answer context
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

                for (var rel : rels) {
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

    private Set<String> llmEntityLinking(String query) {
        Set<String> matchedIds = new HashSet<>();
        // LLM entity linking: use MiniMax M2.7 to identify entities in the query
        // Then match identified entities to Neo4j nodes
        try {
            // Build entity linking prompt
            String entityTypesPrompt = "实体类型: Product, Service, Order, QA, Concept\n" +
                "关键词参考: 产品(Product)相关词汇包括" + String.join(",", KEYWORD_ENTITY_MAP.getOrDefault("Product", List.of())) + "\n" +
                "服务(Service)相关词汇包括" + String.join(",", KEYWORD_ENTITY_MAP.getOrDefault("Service", List.of()));

            String userPrompt = "用户问题: " + query + "\n\n" +
                "请识别问题中提到的实体名称，返回JSON数组格式：\n" +
                "示例: [{\"name\": \"智能手表\", \"type\": \"Product\"}, {\"name\": \"保修\", \"type\": \"Service\"}]";

            // Call LLM via ChatService - implementation depends on existing ChatService interface
            // For now, this is a placeholder that would integrate with your existing LLM call mechanism
            logger.debug("LLM entity linking for query: {}", query);

            // After LLM returns entity names, match them in Neo4j
            // This would be implemented when integrating with your ChatService
        } catch (Exception e) {
            logger.warn("LLM entity linking failed: {}", e.getMessage());
        }
        return matchedIds;
    }

    private String generateAnswerContext(String query, Set<String> entityIds) {
        // Build context from retrieved entities
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
```

### Task 10: Hybrid RAG Service

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/service/retriever/HybridRAGService.java`

- [ ] **Step 1: Create HybridRAGService.java**

```java
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
    private static final double ALPHA = 0.5; // Weight for vector results

    private final KnowledgeBaseService knowledgeBaseService;
    private final GraphRAGRetriever graphRAGRetriever;

    public HybridRAGService(KnowledgeBaseService knowledgeBaseService, GraphRAGRetriever graphRAGRetriever) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.graphRAGRetriever = graphRAGRetriever;
    }

    public HybridSearchResult hybridSearch(String query, int limit) {
        CompletableFuture<VectorSearchResult> vectorFuture = CompletableFuture.supplyAsync(
            () -> knowledgeBaseService.searchKnowledgeBase(query, limit)
        );

        CompletableFuture<GraphSearchResult> graphFuture = CompletableFuture.supplyAsync(
            () -> graphRAGRetriever.search(query, limit)
        );

        VectorSearchResult vectorResult = vectorFuture.join();
        GraphSearchResult graphResult = graphFuture.join();

        return new HybridSearchResult(vectorResult, graphResult);
    }

    public VectorSearchResult searchVectorOnly(String query, int limit) {
        return knowledgeBaseService.searchKnowledgeBase(query, limit);
    }

    public GraphSearchResult searchGraphOnly(String query, int limit) {
        return graphRAGRetriever.search(query, limit);
    }

    public record HybridSearchResult(VectorSearchResult vectorResult, GraphSearchResult graphResult) {}
}
```

### Task 11: Comparison Controller

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/controller/ComparisonController.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/dto/CompareSearchRequest.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/dto/CompareSearchResponse.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/dto/TripleExtractRequest.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/dto/TripleExtractResponse.java`

- [ ] **Step 1: Create CompareSearchRequest.java**

```java
package com.example.customerservice.dto;

import jakarta.validation.constraints.NotBlank;

public class CompareSearchRequest {
    @NotBlank(message = "Query cannot be blank")
    private String query;
    private int limit = 5;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
```

- [ ] **Step 2: Create CompareSearchResponse.java**

```java
package com.example.customerservice.dto;

public class CompareSearchResponse {
    private final String query;
    private final VectorSearchResult vectorResult;
    private final GraphSearchResult graphResult;

    public CompareSearchResponse(String query, VectorSearchResult vectorResult, GraphSearchResult graphResult) {
        this.query = query;
        this.vectorResult = vectorResult;
        this.graphResult = graphResult;
    }

    public String getQuery() { return query; }
    public VectorSearchResult getVectorResult() { return vectorResult; }
    public GraphSearchResult getGraphResult() { return graphResult; }
}
```

- [ ] **Step 3: Create TripleExtractRequest.java**

```java
package com.example.customerservice.dto;

import jakarta.validation.constraints.NotBlank;

public class TripleExtractRequest {
    @NotBlank(message = "Content cannot be blank")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
```

- [ ] **Step 4: Create TripleExtractResponse.java**

```java
package com.example.customerservice.dto;

import java.util.List;
import java.util.Map;

public class TripleExtractResponse {
    private final List<Map<String, String>> triples;

    public TripleExtractResponse(List<Map<String, String>> triples) {
        this.triples = triples;
    }

    public List<Map<String, String>> getTriples() { return triples; }
}
```

- [ ] **Step 5: Create ComparisonController.java**

```java
package com.example.customerservice.controller;

import com.example.customerservice.dto.*;
import com.example.customerservice.service.retriever.HybridRAGService;
import com.example.customerservice.service.extractor.TripleExtractor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compare")
public class ComparisonController {
    private final HybridRAGService hybridRAGService;
    private final TripleExtractor tripleExtractor;

    public ComparisonController(HybridRAGService hybridRAGService, TripleExtractor tripleExtractor) {
        this.hybridRAGService = hybridRAGService;
        this.tripleExtractor = tripleExtractor;
    }

    @PostMapping("/search")
    public CompareSearchResponse compareSearch(@Valid @RequestBody CompareSearchRequest request) {
        HybridRAGService.HybridSearchResult result = hybridRAGService.hybridSearch(request.getQuery(), request.getLimit());
        return new CompareSearchResponse(request.getQuery(), result.vectorResult(), result.graphResult());
    }

    @PostMapping("/extract")
    public ResponseEntity<TripleExtractResponse> extractTriples(@Valid @RequestBody TripleExtractRequest request) {
        // This is a preview endpoint - just return what would be extracted
        return ResponseEntity.ok(new TripleExtractResponse(List.of()));
    }
}
```

---

## Phase 4: Frontend Visualization

### Task 12: Add Cytoscape Dependency

**Files:**
- Modify: `frontend/package.json`

- [ ] **Step 1: Add cytoscape to package.json**

Add to dependencies:
```json
"cytoscape": "^3.28.0"
```

Run: `cd frontend && npm install`

### Task 13: Graph Store

**Files:**
- Create: `frontend/src/stores/graph.js`

- [ ] **Step 1: Create graph.js store**

```javascript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getGraphStats, getGraphNodes, getGraphEdges } from '@/api'

export const useGraphStore = defineStore('graph', () => {
  const nodes = ref([])
  const edges = ref([])
  const stats = ref({ nodeCount: 0, edgeCount: 0 })
  const selectedNodeId = ref(null)
  const highlightedPath = ref([])
  const isLoading = ref(false)

  async function fetchGraphData() {
    isLoading.value = true
    try {
      const [nodesRes, edgesRes, statsRes] = await Promise.all([
        getGraphNodes(1000, 0),
        getGraphEdges(1000, 0),
        getGraphStats()
      ])
      nodes.value = nodesRes
      edges.value = edgesRes
      stats.value = statsRes
    } finally {
      isLoading.value = false
    }
  }

  function selectNode(nodeId) {
    selectedNodeId.value = nodeId
  }

  function highlightPath(path) {
    highlightedPath.value = path
  }

  function clearHighlight() {
    highlightedPath.value = []
  }

  return {
    nodes,
    edges,
    stats,
    selectedNodeId,
    highlightedPath,
    isLoading,
    fetchGraphData,
    selectNode,
    highlightPath,
    clearHighlight
  }
})
```

### Task 14: Graph View Page

**Files:**
- Create: `frontend/src/views/GraphView.vue`

- [ ] **Step 1: Create GraphView.vue**

```vue
<template>
  <div class="graph-view">
    <header class="graph-header">
      <h1>知识图谱</h1>
      <div class="graph-stats">
        <span>节点: {{ graphStore.stats.nodeCount }}</span>
        <span>边: {{ graphStore.stats.edgeCount }}</span>
        <button @click="graphStore.fetchGraphData" :disabled="graphStore.isLoading">
          {{ graphStore.isLoading ? '加载中...' : '刷新' }}
        </button>
      </div>
    </header>

    <div class="graph-container" ref="graphContainer"></div>

    <aside class="node-detail" v-if="selectedNode">
      <h3>{{ selectedNode.name }}</h3>
      <p>类型: {{ selectedNode.type }}</p>
      <div v-for="(value, key) in selectedNode.properties" :key="key">
        <strong>{{ key }}:</strong> {{ value }}
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import cytoscape from 'cytoscape'
import { useGraphStore } from '@/stores/graph'

const graphStore = useGraphStore()
const graphContainer = ref(null)
let cy = null

const selectedNode = computed(() => {
  if (!graphStore.selectedNodeId) return null
  return graphStore.nodes.find(n => n.id === graphStore.selectedNodeId)
})

onMounted(async () => {
  await graphStore.fetchGraphData()
  initCytoscape()
})

onUnmounted(() => {
  if (cy) cy.destroy()
})

watch([() => graphStore.nodes, () => graphStore.edges], () => {
  if (cy) updateGraph()
}, { deep: true })

function initCytoscape() {
  cy = cytoscape({
    container: graphContainer.value,
    style: [
      {
        selector: 'node',
        style: {
          'label': 'data(name)',
          'background-color': '#666',
          'width': 40,
          'height': 40
        }
      },
      {
        selector: 'edge',
        style: {
          'width': 2,
          'line-color': '#ccc',
          'target-arrow-color': '#ccc',
          'target-arrow-shape': 'triangle'
        }
      },
      {
        selector: '.highlighted',
        style: {
          'background-color': '#ff6b6b',
          'line-color': '#ff6b6b',
          'target-arrow-color': '#ff6b6b',
          'width': 4
        }
      }
    ],
    layout: { name: 'circle' }
  })

  cy.on('tap', 'node', (evt) => {
    graphStore.selectNode(evt.target.id())
  })

  updateGraph()
}

function updateGraph() {
  if (!cy) return

  cy.elements().remove()

  const elements = [
    ...graphStore.nodes.map(n => ({
      data: { id: n.id, name: n.name, type: n.type },
      group: 'nodes'
    })),
    ...graphStore.edges.map(e => ({
      data: { id: e.id, source: e.source, target: e.target, relation: e.relation },
      group: 'edges'
    }))
  ]

  cy.add(elements)
  cy.layout({ name: 'circle' }).run()
}
</script>

<style scoped>
.graph-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 1rem;
}

.graph-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.graph-stats {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.graph-container {
  flex: 1;
  min-height: 400px;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.node-detail {
  position: absolute;
  right: 1rem;
  top: 100px;
  width: 250px;
  padding: 1rem;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
</style>
```

### Task 15: Compare View Page

**Files:**
- Create: `frontend/src/views/CompareView.vue`

- [ ] **Step 1: Create CompareView.vue**

```vue
<template>
  <div class="compare-view">
    <header class="compare-header">
      <h1>GraphRAG vs VectorRAG 对比实验</h1>
    </header>

    <div class="query-input">
      <input v-model="query" placeholder="输入问题进行对比..." @keyup.enter="runComparison" />
      <button @click="runComparison" :disabled="isLoading">开始对比</button>
    </div>

    <div class="comparison-results" v-if="result">
      <div class="result-panel vector-panel">
        <h2>Vector RAG</h2>
        <div class="answer">
          <h3>答案:</h3>
          <p>{{ result.vectorResult.answer || '无结果' }}</p>
        </div>
        <div class="retrieved">
          <h3>检索到的知识片段:</h3>
          <ul>
            <li v-for="(chunk, i) in result.vectorResult.retrievedChunks" :key="i">
              {{ chunk.content }} (score: {{ chunk.score?.toFixed(2) }})
            </li>
          </ul>
        </div>
      </div>

      <div class="result-panel graph-panel">
        <h2>Graph RAG</h2>
        <div class="answer">
          <h3>答案:</h3>
          <p>{{ result.graphResult.answer || '无结果' }}</p>
        </div>
        <div class="retrieved">
          <h3>检索到的实体:</h3>
          <ul>
            <li v-for="(entity, i) in result.graphResult.retrievedEntities" :key="i">
              <strong>{{ entity.entityName }}</strong> ({{ entity.entityType }})
              <span v-for="rel in entity.relations" :key="rel.path">
                - {{ rel.path }}
              </span>
            </li>
          </ul>
        </div>
        <div class="mini-graph" ref="miniGraphContainer"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import cytoscape from 'cytoscape'
import { compareSearch } from '@/api'

const query = ref('')
const isLoading = ref(false)
const result = ref(null)
const miniGraphContainer = ref(null)
let miniCy = null

async function runComparison() {
  if (!query.value.trim()) return
  isLoading.value = true
  try {
    result.value = await compareSearch(query.value, 5)
    updateMiniGraph()
  } finally {
    isLoading.value = false
  }
}

function updateMiniGraph() {
  if (!result.value?.graphResult?.subgraphNodes) return

  if (miniCy) miniCy.destroy()

  const elements = [
    ...result.value.graphResult.subgraphNodes.map(n => ({
      data: { id: n.id, name: n.name }
    })),
    ...result.value.graphResult.subgraphEdges.map(e => ({
      data: { source: e.source, target: e.target }
    }))
  ]

  if (elements.length === 0) return

  miniCy = cytoscape({
    container: miniGraphContainer.value,
    elements,
    style: [
      { selector: 'node', style: { 'label': 'data(name)', 'background-color': '#4a90e2' } },
      { selector: 'edge', style: { 'line-color': '#ccc' } }
    ],
    layout: { name: 'circle' }
  })
}
</script>

<style scoped>
.compare-view {
  padding: 1rem;
}

.query-input {
  display: flex;
  gap: 1rem;
  margin: 1rem 0;
}

.query-input input {
  flex: 1;
  padding: 0.5rem;
  font-size: 1rem;
}

.comparison-results {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-top: 1rem;
}

.result-panel {
  padding: 1rem;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.mini-graph {
  height: 200px;
  border: 1px solid #eee;
  margin-top: 1rem;
}
</style>
```

### Task 16: API Functions

**Files:**
- Modify: `frontend/src/api/index.js`

- [ ] **Step 1: Add graph and compare API functions**

Add to the end of the file:
```javascript
// Graph APIs
export async function getGraphStats() {
  return api.get('/api/graph/stats')
}

export async function getGraphNodes(limit = 100, offset = 0) {
  return api.get('/api/graph/nodes', { params: { limit, offset } })
}

export async function getGraphEdges(limit = 100, offset = 0) {
  return api.get('/api/graph/edges', { params: { limit, offset } })
}

export async function clearGraph() {
  return api.post('/api/graph/clear')
}

// Compare APIs
export async function compareSearch(query, limit = 5) {
  return api.post('/api/compare/search', { query, limit })
}

export async function previewTripleExtraction(content) {
  return api.post('/api/compare/extract', { content })
}
```

### Task 17: Router and Navigation

**Files:**
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/layouts/AppLayout.vue`

- [ ] **Step 1: Add routes to router**

Import the new views:
```javascript
import GraphView from '@/views/GraphView.vue'
import CompareView from '@/views/CompareView.vue'
```

Add routes:
```javascript
{
  path: '/graph',
  name: 'graph',
  component: GraphView,
  meta: { title: '知识图谱' }
},
{
  path: '/compare',
  name: 'compare',
  component: CompareView,
  meta: { title: '对比实验' }
}
```

- [ ] **Step 2: Add navigation items to AppLayout**

Add to menuItems array:
```javascript
{ path: '/graph', label: '知识图谱', icon: '◉' },
{ path: '/compare', label: '对比实验', icon: '⇄' }
```

---

## Implementation Order

1. **Phase 1** (Neo4j Integration)
   - Task 1: Add dependency
   - Task 2: Neo4j config
   - Task 3: KnowledgeGraphService
   - Task 4: GraphApiController

2. **Phase 2** (Knowledge Extraction)
   - Task 5: RulePreprocessor
   - Task 6: LLMTripleExtractor
   - Task 7: TripleExtractor
   - Task 8: Integrate into KnowledgeBaseService

3. **Phase 3** (GraphRAG Retrieval)
   - Task 9: GraphRAGRetriever
   - Task 10: HybridRAGService
   - Task 11: ComparisonController

4. **Phase 4** (Frontend)
   - Task 12: Add Cytoscape dependency
   - Task 13: Graph store
   - Task 14: GraphView
   - Task 15: CompareView
   - Task 16: API functions
   - Task 17: Router and navigation
