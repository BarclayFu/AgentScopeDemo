package com.example.customerservice.dto;

import java.util.List;

public class CategoryTreeResponse {
    private final List<CategoryNode> categories;

    public CategoryTreeResponse(List<CategoryNode> categories) {
        this.categories = categories;
    }

    public List<CategoryNode> getCategories() { return categories; }

    public static class CategoryNode {
        private final String id;
        private final String name;
        private final String parentId;
        private final String path;
        private final int level;
        private final int entryCount;
        private final List<CategoryNode> children;

        public CategoryNode(String id, String name, String parentId, String path, int level, int entryCount, List<CategoryNode> children) {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
            this.path = path;
            this.level = level;
            this.entryCount = entryCount;
            this.children = children;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getParentId() { return parentId; }
        public String getPath() { return path; }
        public int getLevel() { return level; }
        public int getEntryCount() { return entryCount; }
        public List<CategoryNode> getChildren() { return children; }
    }
}