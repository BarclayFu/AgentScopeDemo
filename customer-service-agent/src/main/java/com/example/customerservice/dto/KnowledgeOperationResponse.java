package com.example.customerservice.dto;

/**
 * 知识库操作响应
 */
public class KnowledgeOperationResponse {

    private final String message;
    private final String entryId;
    private final long checkedAt;

    public KnowledgeOperationResponse(String message, String entryId, long checkedAt) {
        this.message = message;
        this.entryId = entryId;
        this.checkedAt = checkedAt;
    }

    public String getMessage() {
        return message;
    }

    public String getEntryId() {
        return entryId;
    }

    public long getCheckedAt() {
        return checkedAt;
    }
}
