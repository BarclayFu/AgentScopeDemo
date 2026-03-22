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

class RetrievedChunk {
    private final String content;
    private final double score;
    private final String source;

    public RetrievedChunk(String content, double score, String source) {
        this.content = content;
        this.score = score;
        this.source = source;
    }

    public String getContent() { return content; }
    public double getScore() { return score; }
    public String getSource() { return source; }
}