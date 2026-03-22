package com.example.customerservice.dto;

import jakarta.validation.constraints.NotBlank;

public class CompareSearchRequest {
    @NotBlank(message = "Query cannot be blank")
    private String query;
    private int limit = 5;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}