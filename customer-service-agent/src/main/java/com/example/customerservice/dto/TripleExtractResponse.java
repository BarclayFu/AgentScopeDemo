package com.example.customerservice.dto;

import java.util.List;
import java.util.Map;

public class TripleExtractResponse {
    private final List<Map<String, String>> triples;

    public TripleExtractResponse(List<Map<String, String>> triples) {
        this.triples = triples;
    }

    public List<Map<String, String>> getTriples() { return triples; }
}