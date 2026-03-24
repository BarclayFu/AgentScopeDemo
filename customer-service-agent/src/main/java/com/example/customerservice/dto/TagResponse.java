package com.example.customerservice.dto;

public class TagResponse {
    private final String id;
    private final String name;
    private final long createdAt;

    public TagResponse(String id, String name, long createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getCreatedAt() { return createdAt; }
}