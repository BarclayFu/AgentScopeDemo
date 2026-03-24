package com.example.customerservice.controller;

import com.example.customerservice.dto.EntryGraphResponse;
import com.example.customerservice.dto.GraphEdgeResponse;
import com.example.customerservice.dto.GraphNodeResponse;
import com.example.customerservice.service.KnowledgeBaseService;
import com.example.customerservice.service.retriever.GraphRAGRetriever;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Relationship;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/knowledge/entries")
public class EntryGraphController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final GraphRAGRetriever graphRAGRetriever;
    private final Driver driver;

    public EntryGraphController(KnowledgeBaseService knowledgeBaseService, GraphRAGRetriever graphRAGRetriever, Driver driver) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.graphRAGRetriever = graphRAGRetriever;
        this.driver = driver;
    }

    @GetMapping("/{entryId}/graph")
    public EntryGraphResponse getEntryGraph(@PathVariable String entryId) {
        // Get entry title
        String title = knowledgeBaseService.getEntryTitle(entryId);

        // Find related graph nodes based on entry title/content
        Set<String> matchedEntityIds = findRelatedEntities(title);

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

        if (entityIds.isEmpty()) {
            subgraph.put("nodes", nodes);
            subgraph.put("edges", edges);
            return subgraph;
        }

        try (Session session = driver.session()) {
            String idsParam = entityIds.stream().collect(Collectors.joining(","));
            var result = session.run(
                "MATCH (n)-[r*1.." + hops + "]-(m) WHERE id(n) IN [" + idsParam + "] " +
                "WITH DISTINCT n, r, m " +
                "RETURN n, r, m"
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

    private List<String> findRelatedEntries(String entryId, Set<String> entityIds) {
        // Return empty for now - can be enhanced to find entries with similar entities
        return List.of();
    }
}