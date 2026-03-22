package com.example.customerservice.dto;

public class CompareSearchResponse {
    private final String query;
    private final VectorSearchResult vectorResult;
    private final GraphSearchResult graphResult;

    public CompareSearchResponse(String query, VectorSearchResult vectorResult, GraphSearchResult graphResult) {
        this.query = query;
        this.vectorResult = vectorResult;
        this.graphResult = graphResult;
    }

    public String getQuery() { return query; }
    public VectorSearchResult getVectorResult() { return vectorResult; }
    public GraphSearchResult getGraphResult() { return graphResult; }
}