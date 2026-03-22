package com.example.customerservice.dto;

import jakarta.validation.constraints.NotBlank;

public class TripleExtractRequest {
    @NotBlank(message = "Content cannot be blank")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}