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