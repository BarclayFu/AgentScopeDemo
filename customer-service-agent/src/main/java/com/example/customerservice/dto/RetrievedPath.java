package com.example.customerservice.dto;

public class RetrievedPath {
    private final String path;
    private final int hopCount;

    public RetrievedPath(String path, int hopCount) {
        this.path = path;
        this.hopCount = hopCount;
    }

    public String getPath() { return path; }
    public int getHopCount() { return hopCount; }
}