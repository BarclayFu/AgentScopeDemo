package com.example.customerservice.dto;

import jakarta.validation.constraints.NotBlank;

public class TripleExtractRequest {
    /** 知识条目标题（可选） */
    private String title;

    @NotBlank(message = "Content cannot be blank")
    private String content;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}