package com.example.customerservice.dto;

import java.util.List;

public class RetrievedEntity {
    private final String entityId;
    private final String entityType;
    private final String entityName;
    private final List<RetrievedPath> relations;
    private final double score;

    public RetrievedEntity(String entityId, String entityType, String entityName, List<RetrievedPath> relations, double score) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.entityName = entityName;
        this.relations = relations;
        this.score = score;
    }

    public String getEntityId() { return entityId; }
    public String getEntityType() { return entityType; }
    public String getEntityName() { return entityName; }
    public List<RetrievedPath> getRelations() { return relations; }
    public double getScore() { return score; }
}

class RetrievedPath {
    private final String path;
    private final int hopCount;

    public RetrievedPath(String path, int hopCount) {
        this.path = path;
        this.hopCount = hopCount;
    }

    public String getPath() { return path; }
    public int getHopCount() { return hopCount; }
}