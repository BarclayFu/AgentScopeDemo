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
                "MERGE (s:" + subjectType + " {name: $subject}) MERGE (o:" + objectType + " {name: $object}) MERGE (s)-[r:" + relation + "]->(o)",
                Map.of("subject", subject, "object", object)
            );
        }
    }

    public void clearGraph() {
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
        }
    }
}