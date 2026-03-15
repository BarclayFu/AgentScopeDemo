package com.example.customerservice.dto;

/**
 * 知识库状态响应
 */
public class KnowledgeStatusResponse {

    private final boolean initialized;
    private final int totalEntries;
    private final Long lastUpdatedAt;
    private final Long lastRebuildAt;
    private final String lastOperationMessage;
    private final long checkedAt;

    public KnowledgeStatusResponse(
        boolean initialized,
        int totalEntries,
        Long lastUpdatedAt,
        Long lastRebuildAt,
        String lastOperationMessage,
        long checkedAt
    ) {
        this.initialized = initialized;
        this.totalEntries = totalEntries;
        this.lastUpdatedAt = lastUpdatedAt;
        this.lastRebuildAt = lastRebuildAt;
        this.lastOperationMessage = lastOperationMessage;
        this.checkedAt = checkedAt;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public Long getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public Long getLastRebuildAt() {
        return lastRebuildAt;
    }

    public String getLastOperationMessage() {
        return lastOperationMessage;
    }

    public long getCheckedAt() {
        return checkedAt;
    }
}
