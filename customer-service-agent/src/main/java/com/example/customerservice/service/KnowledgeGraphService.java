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
     * 同时在节点和关系上维护 entryIds，用于后续按知识条目增量更新或删除。
     *
     * @param entryId      知识条目ID
     * @param subject     主语实体名称
     * @param subjectType 主语实体类型（如Product, Service等）
     * @param relation    关系类型（如HAS_SERVICE, RELATED_TO等）
     * @param object      宾语实体名称
     * @param objectType  宾语实体类型
     */
    public void addTriple(
        String entryId,
        String subject,
        String subjectType,
        String relation,
        String object,
        String objectType
    ) {
        long now = System.currentTimeMillis();
        try (Session session = driver.session()) {
            session.run(
                "MERGE (s:" + subjectType + " {name: $subject}) " +
                "ON CREATE SET s.entryIds = [$entryId], s.createdAt = $now, s.updatedAt = $now " +
                "ON MATCH SET s.entryIds = CASE " +
                "  WHEN $entryId IN coalesce(s.entryIds, []) THEN coalesce(s.entryIds, []) " +
                "  ELSE coalesce(s.entryIds, []) + $entryId END, " +
                "  s.updatedAt = $now " +
                "MERGE (o:" + objectType + " {name: $object}) " +
                "ON CREATE SET o.entryIds = [$entryId], o.createdAt = $now, o.updatedAt = $now " +
                "ON MATCH SET o.entryIds = CASE " +
                "  WHEN $entryId IN coalesce(o.entryIds, []) THEN coalesce(o.entryIds, []) " +
                "  ELSE coalesce(o.entryIds, []) + $entryId END, " +
                "  o.updatedAt = $now " +
                "MERGE (s)-[r:" + relation + "]->(o) " +
                "ON CREATE SET r.entryIds = [$entryId], r.createdAt = $now, r.updatedAt = $now " +
                "ON MATCH SET r.entryIds = CASE " +
                "  WHEN $entryId IN coalesce(r.entryIds, []) THEN coalesce(r.entryIds, []) " +
                "  ELSE coalesce(r.entryIds, []) + $entryId END, " +
                "  r.updatedAt = $now",
                Map.of(
                    "entryId",
                    entryId,
                    "subject",
                    subject,
                    "object",
                    object,
                    "now",
                    now
                )
            );
        }
    }

    /**
     * 移除某个知识条目在图谱中的引用。
     *
     * 仅删除该 entryId 在节点和关系上的归属标记；
     * 当节点或关系不再被任何知识条目引用时，才真正删除。
     *
     * @param entryId 知识条目ID
     */
    public void removeEntryReferences(String entryId) {
        if (entryId == null || entryId.isBlank()) {
            return;
        }

        try (Session session = driver.session()) {
            session.run(
                "MATCH ()-[r]-() " +
                "WHERE $entryId IN coalesce(r.entryIds, []) " +
                "SET r.entryIds = [id IN coalesce(r.entryIds, []) WHERE id <> $entryId]",
                Map.of("entryId", entryId)
            );

            session.run(
                "MATCH ()-[r]-() " +
                "WHERE size(coalesce(r.entryIds, [])) = 0 " +
                "DELETE r"
            );

            session.run(
                "MATCH (n) " +
                "WHERE $entryId IN coalesce(n.entryIds, []) " +
                "SET n.entryIds = [id IN coalesce(n.entryIds, []) WHERE id <> $entryId]",
                Map.of("entryId", entryId)
            );

            session.run(
                "MATCH (n) " +
                "WHERE size(coalesce(n.entryIds, [])) = 0 AND NOT (n)--() " +
                "DELETE n"
            );
        }
    }

    public Set<String> findEntityIdsByEntryId(String entryId) {
        Set<String> ids = new HashSet<>();
        if (entryId == null || entryId.isBlank()) {
            return ids;
        }

        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (n) WHERE $entryId IN coalesce(n.entryIds, []) RETURN id(n) as id",
                Map.of("entryId", entryId)
            );
            for (Record record : result.list()) {
                ids.add(String.valueOf(record.get("id").asLong()));
            }
        }

        return ids;
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
