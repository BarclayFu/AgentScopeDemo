package com.example.customerservice.dto;

public class RetrievedChunk {
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