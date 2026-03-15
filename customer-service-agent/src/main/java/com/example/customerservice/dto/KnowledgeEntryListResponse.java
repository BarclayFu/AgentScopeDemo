package com.example.customerservice.dto;

import java.util.List;

/**
 * 知识条目列表响应
 */
public class KnowledgeEntryListResponse {

    private final List<KnowledgeEntryResponse> entries;
    private final int total;
    private final long checkedAt;

    public KnowledgeEntryListResponse(
        List<KnowledgeEntryResponse> entries,
        int total,
        long checkedAt
    ) {
        this.entries = entries;
        this.total = total;
        this.checkedAt = checkedAt;
    }

    public List<KnowledgeEntryResponse> getEntries() {
        return entries;
    }

    public int getTotal() {
        return total;
    }

    public long getCheckedAt() {
        return checkedAt;
    }
}
