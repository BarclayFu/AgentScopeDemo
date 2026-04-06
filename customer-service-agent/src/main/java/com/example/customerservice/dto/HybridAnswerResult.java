package com.example.customerservice.dto;

import java.util.List;

public class HybridAnswerResult {

    private final String answer;
    private final List<HybridCitation> citations;
    private final String retrievalMode;
    private final String fallbackMode;

    public HybridAnswerResult(
        String answer,
        List<HybridCitation> citations,
        String retrievalMode,
        String fallbackMode
    ) {
        this.answer = answer;
        this.citations = citations;
        this.retrievalMode = retrievalMode;
        this.fallbackMode = fallbackMode;
    }

    public String getAnswer() {
        return answer;
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
}
