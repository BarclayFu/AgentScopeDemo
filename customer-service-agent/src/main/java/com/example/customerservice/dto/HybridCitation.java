package com.example.customerservice.dto;

public class HybridCitation {

    private final String type;
    private final String title;
    private final String snippet;
    private final String entityName;
    private final String path;
    private final Double score;

    public HybridCitation(
        String type,
        String title,
        String snippet,
        String entityName,
        String path,
        Double score
    ) {
        this.type = type;
        this.title = title;
        this.snippet = snippet;
        this.entityName = entityName;
        this.path = path;
        this.score = score;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getPath() {
        return path;
    }

    public Double getScore() {
        return score;
    }
}
