package com.example.customerservice.dto;

import java.util.List;

public class ChatMessageResult {

    private final String response;
    private final List<HybridCitation> citations;
    private final String retrievalMode;
    private final String fallbackMode;
    private final long timestamp;

    public ChatMessageResult(
        String response,
        List<HybridCitation> citations,
        String retrievalMode,
        String fallbackMode,
        long timestamp
    ) {
        this.response = response;
        this.citations = citations;
        this.retrievalMode = retrievalMode;
        this.fallbackMode = fallbackMode;
        this.timestamp = timestamp;
    }

    public String getResponse() {
        return response;
    }

    public List<HybridCitation> getCitations() {
        return citations;
    }

    public String getRetrievalMode() {
        return retrievalMode;
    }

    public String getFallbackMode() {
        return fallbackMode;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
