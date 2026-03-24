package com.example.customerservice.dto;

import java.util.List;

/**
 * 知识条目响应
 */
public class KnowledgeEntryResponse {

    private final String entryId;
    private final String title;
    private final String content;
    private final String contentPreview;
    private final String source;
    private final String type;
    private final List<String> categoryIds;
    private final List<String> tagIds;
    private final long createdAt;
    private final long updatedAt;

    public KnowledgeEntryResponse(
        String entryId,
        String title,
        String content,
        String contentPreview,
        String source,
        String type,
        List<String> categoryIds,
        List<String> tagIds,
        long createdAt,
        long updatedAt
    ) {
        this.entryId = entryId;
        this.title = title;
        this.content = content;
        this.contentPreview = contentPreview;
        this.source = source;
        this.type = type;
        this.categoryIds = categoryIds != null ? categoryIds : List.of();
        this.tagIds = tagIds != null ? tagIds : List.of();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getEntryId() {
        return entryId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getContentPreview() {
        return contentPreview;
    }

    public String getSource() {
        return source;
    }

    public String getType() {
        return type;
    }

    public List<String> getCategoryIds() {
        return categoryIds;
    }

    public List<String> getTagIds() {
        return tagIds;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
