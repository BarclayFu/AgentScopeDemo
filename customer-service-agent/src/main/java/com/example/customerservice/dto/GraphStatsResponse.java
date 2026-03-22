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