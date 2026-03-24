package com.example.customerservice.dto;

public class CategoryResponse {
    private final String id;
    private final String name;
    private final String parentId;
    private final String path;
    private final int level;
    private final long createdAt;
    private final long updatedAt;

    public CategoryResponse(String id, String name, String parentId, String path, int level, long createdAt, long updatedAt) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.path = path;
        this.level = level;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getParentId() { return parentId; }
    public String getPath() { return path; }
    public int getLevel() { return level; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}