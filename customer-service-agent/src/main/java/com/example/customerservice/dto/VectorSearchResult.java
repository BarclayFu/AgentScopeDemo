package com.example.customerservice.dto;

import java.util.List;

public class VectorSearchResult {
    private final String answer;
    private final List<RetrievedChunk> retrievedChunks;

    public VectorSearchResult(String answer, List<RetrievedChunk> retrievedChunks) {
        this.answer = answer;
        this.retrievedChunks = retrievedChunks;
    }

    public String getAnswer() { return answer; }
    public List<RetrievedChunk> getRetrievedChunks() { return retrievedChunks; }
}