package com.example.customerservice.dto;

/**
 * 知识条目响应
 */
public class KnowledgeEntryResponse {

    private final String entryId;
    private final String title;
    private final String contentPreview;
    private final String source;
    private final String type;
    private final long createdAt;
    private final long updatedAt;

    public KnowledgeEntryResponse(
        String entryId,
        String title,
        String contentPreview,
        String source,
        String type,
        long createdAt,
        long updatedAt
    ) {
        this.entryId = entryId;
        this.title = title;
        this.contentPreview = contentPreview;
        this.source = source;
        this.type = type;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getEntryId() {
        return entryId;
    }

    public String getTitle() {
        return title;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
