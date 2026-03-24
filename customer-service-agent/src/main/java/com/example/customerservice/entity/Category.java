package com.example.customerservice.entity;

public record Category(
    String id,
    String name,
    String parentId,
    String path,
    int level,
    long createdAt,
    long updatedAt
) {}