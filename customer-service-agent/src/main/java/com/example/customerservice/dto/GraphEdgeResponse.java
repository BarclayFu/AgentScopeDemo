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