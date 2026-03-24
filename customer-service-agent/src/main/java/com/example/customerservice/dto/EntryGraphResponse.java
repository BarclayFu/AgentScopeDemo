package com.example.customerservice.dto;

import java.util.List;

public class EntryGraphResponse {
    private final String entryId;
    private final String title;
    private final List<GraphNodeResponse> nodes;
    private final List<GraphEdgeResponse> edges;
    private final List<String> relatedEntries;

    public EntryGraphResponse(String entryId, String title, List<GraphNodeResponse> nodes, List<GraphEdgeResponse> edges, List<String> relatedEntries) {
        this.entryId = entryId;
        this.title = title;
        this.nodes = nodes;
        this.edges = edges;
        this.relatedEntries = relatedEntries;
    }

    public String getEntryId() { return entryId; }
    public String getTitle() { return title; }
    public List<GraphNodeResponse> getNodes() { return nodes; }
    public List<GraphEdgeResponse> getEdges() { return edges; }
    public List<String> getRelatedEntries() { return relatedEntries; }
}