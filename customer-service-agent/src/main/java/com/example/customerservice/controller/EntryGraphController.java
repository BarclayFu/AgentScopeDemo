package com.example.customerservice.controller;

import com.example.customerservice.dto.EntryGraphResponse;
import com.example.customerservice.dto.GraphEdgeResponse;
import com.example.customerservice.dto.GraphNodeResponse;
import com.example.customerservice.service.KnowledgeBaseService;
import com.example.customerservice.service.KnowledgeGraphService;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Relationship;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/knowledge/entries")
public class EntryGraphController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final Driver driver;

    public EntryGraphController(
        KnowledgeBaseService knowledgeBaseService,
        KnowledgeGraphService knowledgeGraphService,
        Driver driver
    ) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeGraphService = knowledgeGraphService;
        this.driver = driver;
    }

    @GetMapping("/{entryId}/graph")
    public EntryGraphResponse getEntryGraph(@PathVariable String entryId) {
        // Get entry title
        String title = knowledgeBaseService.getEntryTitle(entryId);

        Set<String> matchedEntityIds = knowledgeGraphService.findEntityIdsByEntryId(
            entryId
        );
        if (matchedEntityIds.isEmpty()) {
            matchedEntityIds = findRelatedEntities(title);
        }

        // Build subgraph
        Map<String, Object> subgraph = buildSubgraph(matchedEntityIds, 2);

        // Find related entries
        List<String> relatedEntries = findRelatedEntries(entryId, matchedEntityIds);

        return new EntryGraphResponse(
            entryId,
            title,
            (List<GraphNodeResponse>) subgraph.get("nodes"),
            (List<GraphEdgeResponse>) subgraph.get("edges"),
            relatedEntries
        );
    }

    private Set<String> findRelatedEntities(String title) {
        Set<String> ids = new HashSet<>();
        if (title == null) return ids;
        try (Session session = driver.session()) {
            var result = session.run(
                "MATCH (n) WHERE n.name CONTAINS $keyword RETURN id(n) as id LIMIT 20",
                Map.of("keyword", title.length() > 10 ? title.substring(0, 10) : title)
            );
            for (Record record : result.list()) {
                ids.add(String.valueOf(record.get("id").asLong()));
            }
        }
        return ids;
    }

    private Map<String, Object> buildSubgraph(Set<String> entityIds, int hops) {
        Map<String, Object> subgraph = new HashMap<>();
        List<GraphNodeResponse> nodes = new ArrayList<>();
        List<GraphEdgeResponse> edges = new ArrayList<>();

        subgraph.put("nodes", nodes);
        subgraph.put("edges", edges);

        if (entityIds.isEmpty()) {
            return subgraph;
        }

        try (Session session = driver.session()) {
            List<Long> idLongs = entityIds.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());
            var result = session.run(
                "MATCH (n)-[r*1.." + hops + "]-(m) WHERE id(n) IN $ids " +
                "WITH DISTINCT n, r, m " +
                "RETURN n, r, m",
                Map.of("ids", idLongs)
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
                    String nLabel = n.labels().iterator().hasNext() ? n.labels().iterator().next() : "UNKNOWN";
                    String nName = n.get("name").asString("");
                    nodes.add(new GraphNodeResponse(nId, nLabel, nName, n.asMap()));
                }

                String mId = String.valueOf(m.id());
                if (!seenNodes.contains(mId)) {
                    seenNodes.add(mId);
                    String mLabel = m.labels().iterator().hasNext() ? m.labels().iterator().next() : "UNKNOWN";
                    String mName = m.get("name").asString("");
                    nodes.add(new GraphNodeResponse(mId, mLabel, mName, m.asMap()));
                }

                for (Object relObj : rels) {
                    Relationship rel = (Relationship) relObj;
                    String rId = String.valueOf(rel.id());
                    if (!seenEdges.contains(rId)) {
                        seenEdges.add(rId);
                        // Use startNodeId/endNodeId to match the node id format from n.id()
                        edges.add(new GraphEdgeResponse(rId, String.valueOf(rel.startNodeId()), String.valueOf(rel.endNodeId()), rel.type()));
                    }
                }
            }
        } catch (Exception e) {
            // Log error but return empty subgraph on failure
            // In production, use a proper logger: log.error("Failed to build subgraph", e);
        }

        return subgraph;
    }

    private List<String> findRelatedEntries(String entryId, Set<String> entityIds) {
        // Stub: Returns empty list. Future implementation should find entries
        // that share similar graph entities with the given entryId.
        return List.of();
    }
}
