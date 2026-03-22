package com.example.customerservice.dto;

import java.util.Map;

public class GraphNodeResponse {
    private final String id;
    private final String type;
    private final String name;
    private final Map<String, Object> properties;

    public GraphNodeResponse(String id, String type, String name, Map<String, Object> properties) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.properties = properties;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public Map<String, Object> getProperties() { return properties; }
}