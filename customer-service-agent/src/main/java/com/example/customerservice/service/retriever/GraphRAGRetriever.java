package com.example.customerservice.service.retriever;

import com.example.customerservice.dto.*;
import com.example.customerservice.service.KnowledgeGraphService;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GraphRAG检索器
 *
 * 基于知识图谱的RAG（检索增强生成）检索实现。
 * 通过以下步骤完成检索：
 * 1. 规则匹配：根据关键词匹配实体
 * 2. LLM链接：当规则匹配失败时，使用LLM进行实体链接
 * 3. 子图构建：根据匹配的实体构建N跳内的子图
 * 4. 结果返回：返回检索到的实体、答案上下文和子图结构
 *
 * 支持的实体类型：Product, Service, Order, QA, Concept
 */
@Component
public class GraphRAGRetriever {

    private static final Logger logger = LoggerFactory.getLogger(GraphRAGRetriever.class);

    /**
     * 关键词到实体类型的映射
     *
     * 用于规则匹配阶段的关键词识别。
     * 当用户问题包含这些关键词时，优先使用规则匹配。
     */
    private static final Map<String, List<String>> KEYWORD_ENTITY_MAP = Map.of(
        "Product", List.of("产品", "商品", "东西", "型号"),
        "Service", List.of("保修", "质保", "维修", "退换", "退款", "退货", "换货", "服务"),
        "Order", List.of("订单", "单号"),
        "QA", List.of("怎么", "如何", "是什么", "为什么", "?", "？")
    );

    /** 图谱遍历的最大跳数 */
    private static final int MAX_HOPS = 3;

    /** 实体相关性分数阈值，低于此分数的实体会被过滤 */
    private static final double SCORE_THRESHOLD = 0.3;

    private final Driver driver;
    private final KnowledgeGraphService knowledgeGraphService;

    public GraphRAGRetriever(Driver driver, KnowledgeGraphService knowledgeGraphService) {
        this.driver = driver;
        this.knowledgeGraphService = knowledgeGraphService;
    }

    /**
     * 执行GraphRAG检索
     *
     * @param query 用户查询问题
     * @param limit 返回结果的数量限制
     * @return 包含答案、检索到的实体和子图结构的搜索结果
     */
    public GraphSearchResult search(String query, int limit) {
        Set<String> matchedEntityIds = new HashSet<>();

        // 第一步：规则匹配 - 根据关键词快速定位实体
        matchedEntityIds.addAll(ruleBasedMatch(query));

        // 第二步：如果规则匹配没有结果，使用LLM进行实体链接
        if (matchedEntityIds.isEmpty()) {
            logger.info("规则匹配未找到实体，尝试LLM实体链接");
            matchedEntityIds.addAll(llmEntityLinking(query));
        }

        // 第三步：根据匹配的实体构建子图
        Map<String, Object> subgraph = buildSubgraph(matchedEntityIds, MAX_HOPS);

        // 第四步：生成答案上下文
        String answer = generateAnswerContext(query, matchedEntityIds);

        // 返回完整的搜索结果
        return new GraphSearchResult(
            answer,
            buildRetrievedEntities(subgraph, query),
            (List<GraphNodeResponse>) subgraph.get("nodes"),
            (List<GraphEdgeResponse>) subgraph.get("edges")
        );
    }

    /**
     * 基于规则的实体匹配
     *
     * 通过检查用户问题是否包含预设的关键词，
     * 来快速识别问题中可能涉及的实体类型和名称。
     *
     * 匹配策略：
     * - 如果问题包含"产品"、"保修"等关键词，则匹配对应类型的实体
     * - 使用CONTAINS进行模糊匹配
     *
     * @param query 用户查询
     * @return 匹配到的实体ID集合
     */
    private Set<String> ruleBasedMatch(String query) {
        Set<String> matchedIds = new HashSet<>();
        try (Session session = driver.session()) {
            // 遍历所有实体类型和关键词
            for (Map.Entry<String, List<String>> entry : KEYWORD_ENTITY_MAP.entrySet()) {
                String entityType = entry.getKey();
                for (String keyword : entry.getValue()) {
                    // 如果查询包含该关键词，则匹配该类型的所有实体
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

    /**
     * 基于LLM的实体链接（暂未实现）
     *
     * 当规则匹配无法找到相关实体时，作为后备方案。
     * 使用LLM理解用户问题的语义，识别其中的实体。
     *
     * @param query 用户查询
     * @return 匹配到的实体ID集合
     */
    private Set<String> llmEntityLinking(String query) {
        Set<String> matchedIds = new HashSet<>();
        logger.debug("LLM entity linking for query: {}", query);
        // TODO: 实现LLM实体链接
        return matchedIds;
    }

    /**
     * 根据匹配的实体构建N跳内的子图
     *
     * 从匹配的实体出发，查找它们在指定跳数范围内的所有关联节点和边。
     * 用于获取完整的上下文信息。
     *
     * @param entityIds 中心实体ID集合
     * @param hops     最大跳数
     * @return 包含节点列表和边列表的Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSubgraph(Set<String> entityIds, int hops) {
        Map<String, Object> subgraph = new HashMap<>();
        List<GraphNodeResponse> nodes = new ArrayList<>();
        List<GraphEdgeResponse> edges = new ArrayList<>();

        // 如果没有匹配的实体，返回空子图
        if (entityIds.isEmpty()) {
            subgraph.put("nodes", nodes);
            subgraph.put("edges", edges);
            return subgraph;
        }

        try (Session session = driver.session()) {
            // 将ID列表转换为逗号分隔的字符串，用于Cypher查询
            String idsParam = entityIds.stream().collect(Collectors.joining(","));

            // 执行Cypher查询：
            // 1. 从匹配的实体出发
            // 2. 查找N跳内的所有关联节点和边
            // 3. 使用DISTINCT去重
            var result = session.run(
                "MATCH (n)-[r*1.." + hops + "]-(m) WHERE id(n) IN [" + idsParam + "] " +
                "WITH DISTINCT n, r, m " +
                "RETURN n, r, m"
            );

            Set<String> seenNodes = new HashSet<>();
            Set<String> seenEdges = new HashSet<>();

            for (Record record : result.list()) {
                // 获取当前记录中的中心节点n和关联节点m
                var n = record.get("n").asNode();
                var m = record.get("m").asNode();
                // 获取节点n和m之间的所有关系
                var rels = record.get("r").asList();

                // 处理中心节点n
                String nId = String.valueOf(n.id());
                if (!seenNodes.contains(nId)) {
                    seenNodes.add(nId);
                    nodes.add(new GraphNodeResponse(nId, n.labels().iterator().next(), n.get("name").asString(), n.asMap()));
                }

                // 处理关联节点m
                String mId = String.valueOf(m.id());
                if (!seenNodes.contains(mId)) {
                    seenNodes.add(mId);
                    nodes.add(new GraphNodeResponse(mId, m.labels().iterator().next(), m.get("name").asString(), m.asMap()));
                }

                // 处理所有关系边
                for (Object relObj : rels) {
                    Relationship rel = (Relationship) relObj;
                    String rId = String.valueOf(rel.id());
                    if (!seenEdges.contains(rId)) {
                        seenEdges.add(rId);
                        edges.add(new GraphEdgeResponse(rId, rel.startNodeElementId(), rel.endNodeElementId(), rel.type()));
                    }
                }
            }
        }

        subgraph.put("nodes", nodes);
        subgraph.put("edges", edges);
        return subgraph;
    }

    /**
     * 根据子图构建检索到的实体列表
     *
     * @param subgraph 子图结构（包含nodes和edges）
     * @param query    用户查询，用于计算相关性分数
     * @return 检索到的实体列表
     */
    private List<RetrievedEntity> buildRetrievedEntities(Map<String, Object> subgraph, String query) {
        List<RetrievedEntity> entities = new ArrayList<>();
        List<GraphNodeResponse> nodes = (List<GraphNodeResponse>) subgraph.get("nodes");

        for (GraphNodeResponse node : nodes) {
            // 计算该节点与查询的相关性分数
            double score = calculateRelevanceScore(node, query);
            // 只返回超过阈值的实体
            if (score >= SCORE_THRESHOLD) {
                // 查找该节点的关联路径
                List<RetrievedPath> paths = findPathsToNode(node.getId());
                entities.add(new RetrievedEntity(node.getId(), node.getType(), node.getName(), paths, score));
            }
        }
        return entities;
    }

    /**
     * 计算节点与查询的相关性分数
     *
     * 简单的基于关键词匹配的评分规则：
     * - 完全匹配（节点名包含查询）：1.0分
     * - 部分匹配（节点名包含查询中的单个字符）：0.5分
     * - 其他：0.1分
     *
     * @param node 知识图谱节点
     * @param query 用户查询
     * @return 相关性分数，范围0-1
     */
    private double calculateRelevanceScore(GraphNodeResponse node, String query) {
        String name = node.getName().toLowerCase();
        String queryLower = query.toLowerCase();
        // 完全匹配
        if (name.contains(queryLower)) return 1.0;
        // 部分匹配（按字符）
        for (String keyword : queryLower.split("")) {
            if (name.contains(keyword)) return 0.5;
        }
        return 0.1;
    }

    /**
     * 查找到指定节点的路径
     *
     * @param nodeId 目标节点ID
     * @return 该节点的关联路径列表（最多5条）
     */
    private List<RetrievedPath> findPathsToNode(String nodeId) {
        List<RetrievedPath> paths = new ArrayList<>();
        try (Session session = driver.session()) {
            // 查找1-2跳内到达该节点的路径
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

    /**
     * 生成答案上下文
     *
     * 根据检索到的实体，生成用于增强生成的上下文信息。
     * 格式为：实体类型: 实体名称
     *
     * @param query     用户查询
     * @param entityIds 匹配的实体ID集合
     * @return 格式化的上下文字符串
     */
    private String generateAnswerContext(String query, Set<String> entityIds) {
        StringBuilder context = new StringBuilder();
        try (Session session = driver.session()) {
            for (String id : entityIds) {
                // 查询该节点的详细信息及其直接关联
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