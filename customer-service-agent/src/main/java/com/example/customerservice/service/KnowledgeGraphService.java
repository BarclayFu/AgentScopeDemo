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

/**
 * 知识图谱服务
 *
 * 负责与Neo4j图数据库交互，提供知识图谱的增删改查操作。
 * 支持节点查询、边查询、三元组添加、图谱统计等功能。
 *
 * 实体类型: Product, Service, Order, QA, Concept
 * 关系类型: BELONGS_TO, HAS_SERVICE, RELATED_TO, REFERENCES, MENTIONS
 */
@Service
public class KnowledgeGraphService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphService.class);

    /** Neo4j数据库连接驱动 */
    private final Driver driver;

    public KnowledgeGraphService(Driver driver) {
        this.driver = driver;
    }

    /**
     * 获取图谱统计信息
     *
     * @return 包含节点数量和边数量的统计对象
     */
    public GraphStatsResponse getStats() {
        try (Session session = driver.session()) {
            // 统计所有节点数量
            long nodeCount = session.run("MATCH (n) RETURN count(n) as cnt").single().get("cnt").asLong();
            // 统计所有边数量
            long edgeCount = session.run("MATCH ()-[r]->() RETURN count(r) as cnt").single().get("cnt").asLong();
            return new GraphStatsResponse(nodeCount, edgeCount);
        }
    }

    /**
     * 分页获取所有节点
     *
     * @param limit  每页返回的最大节点数
     * @param offset 偏移量，用于分页
     * @return 节点列表
     */
    public List<GraphNodeResponse> getAllNodes(int limit, int offset) {
        try (Session session = driver.session()) {
            Result result = session.run(
                // 查询节点及其属性，按ID排序进行分页
                "MATCH (n) RETURN id(n) as id, labels(n)[0] as type, n.name as name, properties(n) as props ORDER BY id SKIP $offset LIMIT $limit",
                Map.of("offset", offset, "limit", limit)
            );
            List<GraphNodeResponse> nodes = new ArrayList<>();
            for (Record record : result.list()) {
                String nodeId = String.valueOf(record.get("id").asLong());
                String type = record.get("type").asString();
                // 处理可能为null的name字段
                String name = record.get("name").isNull() ? "" : record.get("name").asString();
                nodes.add(new GraphNodeResponse(nodeId, type, name, record.get("props").asMap()));
            }
            return nodes;
        }
    }

    /**
     * 分页获取所有边
     *
     * @param limit  每页返回的最大边数
     * @param offset 偏移量，用于分页
     * @return 边列表
     */
    public List<GraphEdgeResponse> getAllEdges(int limit, int offset) {
        try (Session session = driver.session()) {
            Result result = session.run(
                // 查询边的起止节点和关系类型，按ID排序进行分页
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

    /**
     * 添加三元组到图谱
     *
     * 使用MERGE操作确保不会创建重复的节点和边。
     * 如果节点或关系已存在，则直接关联，不会创建新的。
     *
     * @param subject     主语实体名称
     * @param subjectType 主语实体类型（如Product, Service等）
     * @param relation    关系类型（如HAS_SERVICE, RELATED_TO等）
     * @param object      宾语实体名称
     * @param objectType  宾语实体类型
     */
    public void addTriple(String subject, String subjectType, String relation, String object, String objectType) {
        try (Session session = driver.session()) {
            // MERGE: 如果存在则使用，不存在则创建
            session.run(
                "MERGE (s:" + subjectType + " {name: $subject}) MERGE (o:" + objectType + " {name: $object}) MERGE (s)-[r:" + relation + "]->(o)",
                Map.of("subject", subject, "object", object)
            );
        }
    }

    /**
     * 清空整个图谱
     *
     * 谨慎使用！此操作会删除所有节点和边。
     */
    public void clearGraph() {
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
        }
    }
}