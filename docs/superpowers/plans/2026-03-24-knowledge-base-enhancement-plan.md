# 知识库管理增强实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现多层分类体系、自由标签系统和条目详情弹窗（含知识图谱展示）

**Architecture:**
- 后端：扩展现有 KnowledgeBaseService，新增 CategoryService、TagService，新增 CategoryController、TagController、EntryGraphController
- 前端：重写 KnowledgeView.vue，新增 CategoryTree、TagInput、EntryDetailModal 等组件
- 数据：采用 JSON 文件存储（复用现有模式），Category/Tag 作为一级实体

**Tech Stack:** Spring Boot 3.1.5 (Java) / Vue 3.5 / Cytoscape / AgentScope RAG

---

## 文件结构

### Backend (customer-service-agent)

**新增文件：**
- `src/main/java/com/example/customerservice/dto/CategoryResponse.java` - 分类响应 DTO
- `src/main/java/com/example/customerservice/dto/CategoryTreeResponse.java` - 分类树响应 DTO
- `src/main/java/com/example/customerservice/dto/TagResponse.java` - 标签响应 DTO
- `src/main/java/com/example/customerservice/dto/EntryDetailResponse.java` - 条目详情响应 DTO
- `src/main/java/com/example/customerservice/dto/EntryGraphResponse.java` - 条目图谱响应 DTO
- `src/main/java/com/example/customerservice/entity/Category.java` - 分类实体
- `src/main/java/com/example/customerservice/entity/Tag.java` - 标签实体
- `src/main/java/com/example/customerservice/entity/EntryCategory.java` - 条目-分类关联实体
- `src/main/java/com/example/customerservice/service/CategoryService.java` - 分类服务
- `src/main/java/com/example/customerservice/service/TagService.java` - 标签服务
- `src/main/java/com/example/customerservice/controller/CategoryController.java` - 分类控制器
- `src/main/java/com/example/customerservice/controller/TagController.java` - 标签控制器
- `src/main/java/com/example/customerservice/controller/EntryGraphController.java` - 条目图谱控制器

**修改文件：**
- `data/categories.json` - 新增分类数据存储
- `data/tags.json` - 新增标签数据存储
- `data/entry-tags.json` - 新增条目-标签关联数据存储（由 TagService 管理）
- `data/entry-categories.json` - 新增条目-分类关联数据存储
- `src/main/java/com/example/customerservice/dto/KnowledgeEntryResponse.java` - 扩展 tagIds 字段
- `src/main/java/com/example/customerservice/dto/KnowledgeEntryCreateRequest.java` - 扩展 categoryIds/tags 字段
- `src/main/java/com/example/customerservice/service/KnowledgeBaseService.java` - 扩展支持分类和标签
- `src/main/java/com/example/customerservice/controller/KnowledgeController.java` - 扩展 GET /api/knowledge/entries 支持过滤

### Frontend (frontend)

**新增文件：**
- `src/components/knowledge/CategoryTree.vue` - 分类树组件
- `src/components/knowledge/EntryCard.vue` - 条目卡片组件
- `src/components/knowledge/EntryList.vue` - 条目列表组件（容器）
- `src/components/knowledge/EntryDetailModal.vue` - 详情弹窗组件
- `src/components/knowledge/TagInput.vue` - 标签输入组件
- `src/components/knowledge/CategorySelector.vue` - 分类选择器组件（用于弹窗内添加分类）
- `src/components/knowledge/EntryFormModal.vue` - 新增/编辑条目弹窗
- `src/components/knowledge/GraphPanel.vue` - 图谱面板组件（可复用 GraphView 中的 Cytoscape）

**修改文件：**
- `src/views/KnowledgeView.vue` - 完全重写
- `src/api/index.js` - 新增 API 函数
- `src/stores/` - 新增 knowledge store（如果需要）

---

## Phase 1: 后端数据模型和 API

### Task 1: 创建分类相关 DTO 和实体

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/dto/CategoryResponse.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/dto/CategoryTreeResponse.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/entity/Category.java`

- [ ] **Step 1: 创建 CategoryResponse.java**

```java
package com.example.customerservice.dto;

public class CategoryResponse {
    private final String id;
    private final String name;
    private final String parentId;
    private final String path;
    private final int level;
    private final long createdAt;
    private final long updatedAt;

    public CategoryResponse(String id, String name, String parentId, String path, int level, long createdAt, long updatedAt) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.path = path;
        this.level = level;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getParentId() { return parentId; }
    public String getPath() { return path; }
    public int getLevel() { return level; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 2: 创建 CategoryTreeResponse.java**

```java
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
```

- [ ] **Step 3: 创建 Category.java (record)**

```java
package com.example.customerservice.entity;

public record Category(
    String id,
    String name,
    String parentId,
    String path,
    int level,
    long createdAt,
    long updatedAt
) {}
```

- [ ] **Step 4: 创建 data/categories.json**

```json
[]
```

- [ ] **Step 5: Commit**

```bash
git add customer-service-agent/src/main/java/com/example/customerservice/dto/CategoryResponse.java
git add customer-service-agent/src/main/java/com/example/customerservice/dto/CategoryTreeResponse.java
git add customer-service-agent/src/main/java/com/example/customerservice/entity/Category.java
git add customer-service-agent/data/categories.json
git commit -m "feat(backend): add Category DTO and entity"
```

---

### Task 2: 创建标签相关 DTO 和实体

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/dto/TagResponse.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/entity/Tag.java`

- [ ] **Step 1: 创建 TagResponse.java**

```java
package com.example.customerservice.dto;

public class TagResponse {
    private final String id;
    private final String name;
    private final long createdAt;

    public TagResponse(String id, String name, long createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 2: 创建 Tag.java**

```java
package com.example.customerservice.entity;

public record Tag(
    String id,
    String name,
    long createdAt
) {}
```

- [ ] **Step 3: 创建 data/tags.json**

```json
[]
```

- [ ] **Step 4: Commit**

```bash
git add customer-service-agent/src/main/java/com/example/customerservice/dto/TagResponse.java
git add customer-service-agent/src/main/java/com/example/customerservice/entity/Tag.java
git add customer-service-agent/data/tags.json
git commit -m "feat(backend): add Tag DTO and entity"
```

---

### Task 3: 创建条目-分类关联实体和数据文件

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/entity/EntryCategory.java`
- Create: `customer-service-agent/data/entry-categories.json`

- [ ] **Step 1: 创建 EntryCategory.java**

```java
package com.example.customerservice.entity;

public record EntryCategory(
    String id,
    String entryId,
    String categoryId
) {}
```

- [ ] **Step 2: 创建 data/entry-categories.json**

```json
[]
```

- [ ] **Step 3: Commit**

```bash
git add customer-service-agent/src/main/java/com/example/customerservice/entity/EntryCategory.java
git add customer-service-agent/data/entry-categories.json
git commit -m "feat(backend): add EntryCategory entity and data file"
```

---

### Task 4: 创建 CategoryService

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/service/CategoryService.java`

- [ ] **Step 1: 创建 CategoryService.java**

```java
package com.example.customerservice.service;

import com.example.customerservice.dto.CategoryResponse;
import com.example.customerservice.dto.CategoryTreeResponse;
import com.example.customerservice.dto.CategoryTreeResponse.CategoryNode;
import com.example.customerservice.entity.Category;
import com.example.customerservice.entity.EntryCategory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);
    private static final Path CATEGORIES_PATH = Paths.get("data", "categories.json");
    private static final Path ENTRY_CATEGORIES_PATH = Paths.get("data", "entry-categories.json");

    private final ObjectMapper objectMapper;
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<String, EntryCategory> entryCategories = new LinkedHashMap<>();

    public CategoryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadCategories();
        loadEntryCategories();
    }

    public List<CategoryResponse> listCategories() {
        return categories.values().stream()
            .map(c -> new CategoryResponse(c.id(), c.name(), c.parentId(), c.path(), c.level(), c.createdAt(), c.updatedAt()))
            .toList();
    }

    public CategoryTreeResponse getCategoryTree() {
        List<CategoryNode> rootNodes = buildTree(null);
        return new CategoryTreeResponse(rootNodes);
    }

    public CategoryResponse createCategory(String name, String parentId) throws IOException {
        validateName(name);

        long now = Instant.now().toEpochMilli();
        String id = "cat-" + UUID.randomUUID().toString().substring(0, 8);

        String path;
        int level;
        if (parentId == null) {
            path = "/" + name;
            level = 0;
        } else {
            Category parent = categories.get(parentId);
            if (parent == null) {
                throw new IllegalArgumentException("父分类不存在: " + parentId);
            }
            path = parent.path() + "/" + name;
            level = parent.level() + 1;
        }

        Category category = new Category(id, name, parentId, path, level, now, now);
        categories.put(id, category);
        persistCategories();

        return new CategoryResponse(id, name, parentId, path, level, now, now);
    }

    public CategoryResponse updateCategory(String id, String newName) throws IOException {
        Category existing = categories.get(id);
        if (existing == null) {
            throw new IllegalArgumentException("分类不存在: " + id);
        }

        validateName(newName);

        long now = Instant.now().toEpochMilli();
        String newPath = existing.parentId() == null
            ? "/" + newName
            : categories.get(existing.parentId()).path() + "/" + newName;

        Category updated = new Category(id, newName, existing.parentId(), newPath, existing.level(), existing.createdAt(), now);
        categories.put(id, updated);

        // Update child paths
        updateChildPaths(id, newPath);

        persistCategories();
        return new CategoryResponse(id, newName, existing.parentId(), newPath, existing.level(), existing.createdAt(), now);
    }

    public void deleteCategory(String id) throws IOException {
        Category category = categories.remove(id);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在: " + id);
        }

        // Remove entry-category associations
        entryCategories.entrySet().removeIf(e -> e.getValue().categoryId().equals(id));

        // Move children to grandparent (reparent to deleted category's parent)
        for (Category child : categories.values()) {
            if (id.equals(child.parentId())) {
                String newPath = category.parentId() == null
                    ? "/" + child.name()
                    : categories.get(category.parentId()).path() + "/" + child.name();
                Category updated = new Category(
                    child.id(), child.name(), category.parentId(), newPath,
                    category.parentId() == null ? 0 : categories.get(category.parentId()).level() + 1,
                    child.createdAt(), Instant.now().toEpochMilli()
                );
                categories.put(child.id(), updated);
                // Recursively update children's paths
                updateChildPaths(child.id(), newPath);
            }
        }

        persistCategories();
        persistEntryCategories();
    }

    public void addEntryToCategory(String entryId, String categoryId) throws IOException {
        if (!categories.containsKey(categoryId)) {
            throw new IllegalArgumentException("分类不存在: " + categoryId);
        }

        // Check if already exists
        for (EntryCategory ec : entryCategories.values()) {
            if (ec.entryId().equals(entryId) && ec.categoryId().equals(categoryId)) {
                return; // Already exists
            }
        }

        String ecId = "ec-" + UUID.randomUUID().toString().substring(0, 8);
        EntryCategory ec = new EntryCategory(ecId, entryId, categoryId);
        entryCategories.put(ecId, ec);
        persistEntryCategories();
    }

    public void removeEntryFromCategory(String entryId, String categoryId) throws IOException {
        entryCategories.entrySet().removeIf(e ->
            e.getValue().entryId().equals(entryId) && e.getValue().categoryId().equals(categoryId)
        );
        persistEntryCategories();
    }

    public List<String> getCategoryIdsForEntry(String entryId) {
        return entryCategories.values().stream()
            .filter(ec -> ec.entryId().equals(entryId))
            .map(EntryCategory::categoryId)
            .toList();
    }

    public List<String> getEntryIdsForCategory(String categoryId) {
        return entryCategories.values().stream()
            .filter(ec -> ec.categoryId().equals(categoryId))
            .map(EntryCategory::entryId)
            .toList();
    }

    public int getEntryCount(String categoryId) {
        return (int) entryCategories.values().stream()
            .filter(ec -> ec.categoryId().equals(categoryId))
            .count();
    }

    private List<CategoryNode> buildTree(String parentId) {
        return categories.values().stream()
            .filter(c -> Objects.equals(parentId, c.parentId()))
            .map(c -> new CategoryNode(
                c.id(),
                c.name(),
                c.parentId(),
                c.path(),
                c.level(),
                getEntryCount(c.id()),
                buildTree(c.id())
            ))
            .toList();
    }

    private void updateChildPaths(String parentId, String parentPath) {
        for (Category child : categories.values()) {
            if (parentId.equals(child.parentId())) {
                String newPath = parentPath + "/" + child.name();
                Category updated = new Category(child.id(), child.name(), child.parentId(), newPath, child.level(), child.createdAt(), Instant.now().toEpochMilli());
                categories.put(child.id(), updated);
                updateChildPaths(child.id(), newPath);
            }
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
    }

    private void loadCategories() {
        try {
            if (Files.exists(CATEGORIES_PATH)) {
                List<Category> loaded = objectMapper.readValue(CATEGORIES_PATH.toFile(), new TypeReference<List<Category>>() {});
                categories.clear();
                for (Category c : loaded) {
                    categories.put(c.id(), c);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load categories", e);
        }
    }

    private void persistCategories() throws IOException {
        Files.createDirectories(CATEGORIES_PATH.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(CATEGORIES_PATH.toFile(), categories.values());
    }

    private void loadEntryCategories() {
        try {
            if (Files.exists(ENTRY_CATEGORIES_PATH)) {
                List<EntryCategory> loaded = objectMapper.readValue(ENTRY_CATEGORIES_PATH.toFile(), new TypeReference<List<EntryCategory>>() {});
                entryCategories.clear();
                for (EntryCategory ec : loaded) {
                    entryCategories.put(ec.id(), ec);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load entry-categories", e);
        }
    }

    private void persistEntryCategories() throws IOException {
        Files.createDirectories(ENTRY_CATEGORIES_PATH.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(ENTRY_CATEGORIES_PATH.toFile(), entryCategories.values());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add customer-service-agent/src/main/java/com/example/customerservice/service/CategoryService.java
git commit -m "feat(backend): add CategoryService with tree structure"
```

---

### Task 5: 创建 TagService

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/service/TagService.java`

- [ ] **Step 1: 创建 TagService.java**

```java
package com.example.customerservice.service;

import com.example.customerservice.dto.TagResponse;
import com.example.customerservice.entity.Tag;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@Service
public class TagService {

    private static final Logger logger = LoggerFactory.getLogger(TagService.class);
    private static final Path TAGS_PATH = Paths.get("data", "tags.json");
    private static final Path ENTRY_TAGS_PATH = Paths.get("data", "entry-tags.json");

    private final ObjectMapper objectMapper;
    private final Map<String, Tag> tags = new LinkedHashMap<>();
    private final Map<String, Set<String>> entryTags = new LinkedHashMap<>(); // entryId -> Set of tagIds

    public TagService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadTags();
        loadEntryTags();
    }

    public List<TagResponse> listTags() {
        return tags.values().stream()
            .map(t -> new TagResponse(t.id(), t.name(), t.createdAt()))
            .toList();
    }

    public TagResponse createTag(String name) throws IOException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("标签名称不能为空");
        }

        // Check for duplicate name
        for (Tag existing : tags.values()) {
            if (existing.name().equals(name)) {
                return new TagResponse(existing.id(), existing.name(), existing.createdAt());
            }
        }

        long now = Instant.now().toEpochMilli();
        String id = "tag-" + UUID.randomUUID().toString().substring(0, 8);
        Tag tag = new Tag(id, name.trim(), now);
        tags.put(id, tag);
        persistTags();

        return new TagResponse(id, name.trim(), now);
    }

    public void deleteTag(String id) throws IOException {
        Tag removed = tags.remove(id);
        if (removed == null) {
            throw new IllegalArgumentException("标签不存在: " + id);
        }

        // Remove from all entries
        for (Set<String> tagIds : entryTags.values()) {
            tagIds.remove(id);
        }

        persistTags();
        persistEntryTags();
    }

    public void addTagToEntry(String entryId, String tagId) throws IOException {
        if (!tags.containsKey(tagId)) {
            throw new IllegalArgumentException("标签不存在: " + tagId);
        }

        entryTags.computeIfAbsent(entryId, k -> new LinkedHashSet<>()).add(tagId);
        persistEntryTags();
    }

    public void removeTagFromEntry(String entryId, String tagId) throws IOException {
        Set<String> tagIds = entryTags.get(entryId);
        if (tagIds != null) {
            tagIds.remove(tagId);
            persistEntryTags();
        }
    }

    public List<String> getTagIdsForEntry(String entryId) {
        Set<String> tagIds = entryTags.get(entryId);
        return tagIds != null ? new ArrayList<>(tagIds) : List.of();
    }

    public List<String> getEntryIdsForTag(String tagId) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Set<String>> e : entryTags.entrySet()) {
            if (e.getValue().contains(tagId)) {
                result.add(e.getEntryKey());
            }
        }
        return result;
    }

    public List<TagResponse> getTagsForEntry(String entryId) {
        Set<String> tagIds = entryTags.get(entryId);
        if (tagIds == null) return List.of();

        return tagIds.stream()
            .map(tags::get)
            .filter(Objects::nonNull)
            .map(t -> new TagResponse(t.id(), t.name(), t.createdAt()))
            .toList();
    }

    private void loadTags() {
        try {
            if (Files.exists(TAGS_PATH)) {
                List<Tag> loaded = objectMapper.readValue(TAGS_PATH.toFile(), new TypeReference<List<Tag>>() {});
                tags.clear();
                for (Tag t : loaded) {
                    tags.put(t.id(), t);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load tags", e);
        }
    }

    private void persistTags() throws IOException {
        Files.createDirectories(TAGS_PATH.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(TAGS_PATH.toFile(), tags.values());
    }

    private void loadEntryTags() {
        try {
            if (Files.exists(ENTRY_TAGS_PATH)) {
                Map<String, List<String>> loaded = objectMapper.readValue(ENTRY_TAGS_PATH.toFile(), new TypeReference<Map<String, List<String>>>() {});
                entryTags.clear();
                for (Map.Entry<String, List<String>> e : loaded.entrySet()) {
                    entryTags.put(e.getKey(), new LinkedHashSet<>(e.getValue()));
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load entry-tags", e);
        }
    }

    private void persistEntryTags() throws IOException {
        Files.createDirectories(ENTRY_TAGS_PATH.getParent());
        Map<String, List<String>> toSave = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> e : entryTags.entrySet()) {
            toSave.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(ENTRY_TAGS_PATH.toFile(), toSave);
    }
}
```

- [ ] **Step 2: 创建 data/entry-tags.json**

```json
{}
```

- [ ] **Step 3: Commit**

```bash
git add customer-service-agent/src/main/java/com/example/customerservice/service/TagService.java
git add customer-service-agent/data/entry-tags.json
git commit -m "feat(backend): add TagService with entry-tag associations"
```

---

### Task 6: 创建 CategoryController 和 TagController

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/controller/CategoryController.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/controller/TagController.java`

- [ ] **Step 1: 创建 CategoryController.java**

```java
package com.example.customerservice.controller;

import com.example.customerservice.dto.CategoryResponse;
import com.example.customerservice.dto.CategoryTreeResponse;
import com.example.customerservice.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public CategoryTreeResponse getCategoryTree() {
        return categoryService.getCategoryTree();
    }

    @GetMapping("/list")
    public ResponseEntity<?> listCategories() {
        return ResponseEntity.ok(categoryService.listCategories());
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String parentId = request.get("parentId");
            CategoryResponse category = categoryService.createCategory(name, parentId);
            return ResponseEntity.status(HttpStatus.CREATED).body(category);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "创建分类失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            String newName = request.get("name");
            CategoryResponse category = categoryService.updateCategory(id, newName);
            return ResponseEntity.ok(category);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "更新分类失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable String id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(Map.of("message", "分类已删除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "删除分类失败: " + e.getMessage()));
        }
    }
}
```

- [ ] **Step 2: 创建 TagController.java**

```java
package com.example.customerservice.controller;

import com.example.customerservice.dto.TagResponse;
import com.example.customerservice.service.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public List<TagResponse> listTags() {
        return tagService.listTags();
    }

    @PostMapping
    public ResponseEntity<?> createTag(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            TagResponse tag = tagService.createTag(name);
            return ResponseEntity.status(HttpStatus.CREATED).body(tag);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "创建标签失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTag(@PathVariable String id) {
        try {
            tagService.deleteTag(id);
            return ResponseEntity.ok(Map.of("message", "标签已删除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "删除标签失败: " + e.getMessage()));
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add customer-service-agent/src/main/java/com/example/customerservice/controller/CategoryController.java
git add customer-service-agent/src/main/java/com/example/customerservice/controller/TagController.java
git commit -m "feat(backend): add CategoryController and TagController"
```

---

### Task 7: 扩展现有 KnowledgeEntryResponse 和 KnowledgeEntryCreateRequest

**Files:**
- Modify: `customer-service-agent/src/main/java/com/example/customerservice/dto/KnowledgeEntryResponse.java`
- Modify: `customer-service-agent/src/main/java/com/example/customerservice/dto/KnowledgeEntryCreateRequest.java`

- [ ] **Step 1: 扩展 KnowledgeEntryResponse.java**

在原有字段基础上新增 `categoryIds` 和 `tagIds`：

```java
// Add these fields:
private final List<String> categoryIds;
private final List<String> tagIds;

// Add to constructor parameters and assignment

// Add getters:
public List<String> getCategoryIds() { return categoryIds; }
public List<String> getTagIds() { return tagIds; }
```

- [ ] **Step 2: 扩展 KnowledgeEntryCreateRequest.java**

```java
// Add these fields:
private List<String> categoryIds;
private List<String> tags; // tag names

// Add getters and setters
public List<String> getCategoryIds() { return categoryIds; }
public void setCategoryIds(List<String> categoryIds) { this.categoryIds = categoryIds; }
public List<String> getTags() { return tags; }
public void setTags(List<String> tags) { this.tags = tags; }
```

- [ ] **Step 3: Commit**

```bash
git add customer-service-agent/src/main/java/com/example/customerservice/dto/KnowledgeEntryResponse.java
git add customer-service-agent/src/main/java/com/example/customerservice/dto/KnowledgeEntryCreateRequest.java
git commit -m "feat(backend): extend KnowledgeEntry DTOs with categoryIds and tagIds"
```

---

### Task 8: 创建 EntryGraphController（条目图谱 API）

**Files:**
- Create: `customer-service-agent/src/main/java/com/example/customerservice/dto/EntryGraphResponse.java`
- Create: `customer-service-agent/src/main/java/com/example/customerservice/controller/EntryGraphController.java`

**Note:** `GraphNodeResponse` and `GraphEdgeResponse` already exist at `com.example.customerservice.dto.*` - no need to create them.

- [ ] **Step 1: 创建 EntryGraphResponse.java**

```java
package com.example.customerservice.dto;

import java.util.List;

public class EntryGraphResponse {
    private final String entryId;
    private final String title;
    private final List<GraphNodeResponse> nodes;
    private final List<GraphEdgeResponse> edges;
    private final List<String> relatedEntries;

    public EntryGraphResponse(String entryId, String title, List<GraphNodeResponse> nodes, List<GraphEdgeResponse> edges, List<String> relatedEntries) {
        this.entryId = entryId;
        this.title = title;
        this.nodes = nodes;
        this.edges = edges;
        this.relatedEntries = relatedEntries;
    }

    public String getEntryId() { return entryId; }
    public String getTitle() { return title; }
    public List<GraphNodeResponse> getNodes() { return nodes; }
    public List<GraphEdgeResponse> getEdges() { return edges; }
    public List<String> getRelatedEntries() { return relatedEntries; }
}
```

- [ ] **Step 2: 创建 EntryGraphController.java**

```java
package com.example.customerservice.controller;

import com.example.customerservice.dto.EntryGraphResponse;
import com.example.customerservice.dto.GraphEdgeResponse;
import com.example.customerservice.dto.GraphNodeResponse;
import com.example.customerservice.service.KnowledgeBaseService;
import com.example.customerservice.service.retriever.GraphRAGRetriever;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Relationship;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/knowledge/entries")
public class EntryGraphController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final GraphRAGRetriever graphRAGRetriever;
    private final Driver driver;

    public EntryGraphController(KnowledgeBaseService knowledgeBaseService, GraphRAGRetriever graphRAGRetriever, Driver driver) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.graphRAGRetriever = graphRAGRetriever;
        this.driver = driver;
    }

    @GetMapping("/{entryId}/graph")
    public EntryGraphResponse getEntryGraph(@PathVariable String entryId) {
        // Get entry title
        String title = knowledgeBaseService.getEntryTitle(entryId);

        // Find related graph nodes based on entry title/content
        Set<String> matchedEntityIds = findRelatedEntities(title);

        // Build subgraph
        Map<String, Object> subgraph = buildSubgraph(matchedEntityIds, 2);

        // Find related entries
        List<String> relatedEntries = findRelatedEntries(entryId, matchedEntityIds);

        return new EntryGraphResponse(
            entryId,
            title,
            (List<GraphNodeResponse>) subgraph.get("nodes"),
            (List<GraphEdgeResponse>) subgraph.get("edges"),
            relatedEntries
        );
    }

    private Set<String> findRelatedEntities(String title) {
        Set<String> ids = new HashSet<>();
        try (Session session = driver.session()) {
            var result = session.run(
                "MATCH (n) WHERE n.name CONTAINS $keyword RETURN id(n) as id LIMIT 20",
                Map.of("keyword", title.length() > 10 ? title.substring(0, 10) : title)
            );
            for (Record record : result.list()) {
                ids.add(String.valueOf(record.get("id").asLong()));
            }
        }
        return ids;
    }

    private Map<String, Object> buildSubgraph(Set<String> entityIds, int hops) {
        Map<String, Object> subgraph = new HashMap<>();
        List<GraphNodeResponse> nodes = new ArrayList<>();
        List<GraphEdgeResponse> edges = new ArrayList<>();

        if (entityIds.isEmpty()) {
            subgraph.put("nodes", nodes);
            subgraph.put("edges", edges);
            return subgraph;
        }

        try (Session session = driver.session()) {
            String idsParam = entityIds.stream().collect(Collectors.joining(","));
            var result = session.run(
                "MATCH (n)-[r*1.." + hops + "]-(m) WHERE id(n) IN [" + idsParam + "] " +
                "WITH DISTINCT n, r, m " +
                "RETURN n, r, m"
            );

            Set<String> seenNodes = new HashSet<>();
            Set<String> seenEdges = new HashSet<>();

            for (Record record : result.list()) {
                var n = record.get("n").asNode();
                var m = record.get("m").asNode();
                var rels = record.get("r").asList();

                String nId = String.valueOf(n.id());
                if (!seenNodes.contains(nId)) {
                    seenNodes.add(nId);
                    nodes.add(new GraphNodeResponse(nId, n.labels().iterator().next(), n.get("name").asString(), n.asMap()));
                }

                String mId = String.valueOf(m.id());
                if (!seenNodes.contains(mId)) {
                    seenNodes.add(mId);
                    nodes.add(new GraphNodeResponse(mId, m.labels().iterator().next(), m.get("name").asString(), m.asMap()));
                }

                for (Object relObj : rels) {
                    Relationship rel = (Relationship) relObj;
                    String rId = String.valueOf(rel.id());
                    if (!seenEdges.contains(rId)) {
                        seenEdges.add(rId);
                        edges.add(new GraphEdgeResponse(rId, rel.startNodeElementId(), rel.endNodeElementId(), rel.type()));
                    }
                }
            }
        }

        subgraph.put("nodes", nodes);
        subgraph.put("edges", edges);
        return subgraph;
    }

    private List<String> findRelatedEntries(String entryId, Set<String> entityIds) {
        // Return empty for now - can be enhanced to find entries with similar entities
        return List.of();
    }
}
```

- [ ] **Step 3: 在 KnowledgeBaseService 中添加 getEntryTitle 方法**

在 KnowledgeBaseService.java 中添加：

```java
public String getEntryTitle(String entryId) {
    ManagedKnowledgeEntry entry = entries.get(entryId);
    return entry != null ? entry.title() : null;
}
```

- [ ] **Step 4: Commit**

```bash
git add customer-service-agent/src/main/java/com/example/customerservice/dto/EntryGraphResponse.java
git add customer-service-agent/src/main/java/com/example/customerservice/controller/EntryGraphController.java
git add customer-service-agent/src/main/java/com/example/customerservice/service/KnowledgeBaseService.java
git commit -m "feat(backend): add EntryGraphController for entry knowledge graph API"
```

---

## Phase 2: 前端组件

### Task 9: 创建前端 API 函数

**Files:**
- Modify: `frontend/src/api/index.js`

- [ ] **Step 1: 添加分类和标签 API 函数**

```javascript
// ==================== 知识库分类 API ====================

export async function getCategoryTree() {
  return api.get('/api/knowledge/categories')
}

export async function listCategories() {
  return api.get('/api/knowledge/categories/list')
}

export async function createCategory(name, parentId = null) {
  return api.post('/api/knowledge/categories', { name, parentId })
}

export async function updateCategory(id, name) {
  return api.put(`/api/knowledge/categories/${id}`, { name })
}

export async function deleteCategory(id) {
  return api.delete(`/api/knowledge/categories/${id}`)
}

// ==================== 知识库标签 API ====================

export async function getTags() {
  return api.get('/api/knowledge/tags')
}

export async function createTag(name) {
  return api.post('/api/knowledge/tags', { name })
}

export async function deleteTag(id) {
  return api.delete(`/api/knowledge/tags/${id}`)
}

// ==================== 条目图谱 API ====================

export async function getEntryGraph(entryId) {
  return api.get(`/api/knowledge/entries/${entryId}/graph`)
}

// ==================== 扩展现有条目 API ====================

export async function getKnowledgeEntries(params = {}) {
  return api.get('/api/knowledge/entries', { params })
}

export async function updateKnowledgeEntry(entryId, payload) {
  return api.put(`/api/knowledge/entries/${entryId}`, payload)
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/index.js
git commit -m "feat(frontend): add category, tag, and entry graph API functions"
```

---

### Task 10: 创建 CategoryTree 组件

**Files:**
- Create: `frontend/src/components/knowledge/CategoryTree.vue`

- [ ] **Step 1: 创建 CategoryTree.vue**

```vue
<template>
  <div class="category-tree">
    <div class="tree-header">
      <h4>分类目录</h4>
      <button class="add-btn" @click="$emit('add-category')">+ 新建</button>
    </div>
    <div class="tree-content">
      <div
        v-for="category in categories"
        :key="category.id"
        class="tree-node"
      >
        <div
          class="node-item"
          :class="{ active: selectedId === category.id }"
          :style="{ paddingLeft: (category.level * 16 + 8) + 'px' }"
          @click="selectCategory(category)"
        >
          <span
            v-if="category.children && category.children.length"
            class="expand-icon"
            @click.stop="toggleExpand(category.id)"
          >
            {{ expandedIds.has(category.id) ? '▼' : '▶' }}
          </span>
          <span v-else class="expand-icon-placeholder"></span>
          <span class="folder-icon">📁</span>
          <span class="node-name">{{ category.name }}</span>
          <span class="entry-count">({{ category.entryCount }})</span>
        </div>
        <div v-if="expandedIds.has(category.id) && category.children">
          <CategoryTreeNode
            :categories="category.children"
            :selectedId="selectedId"
            :expandedIds="expandedIds"
            :depth="depth + 1"
            @select="$emit('select', $event)"
            @toggle-expand="$emit('toggle-expand', $event)"
          />
        </div>
      </div>
      <div
        class="node-item uncategorized"
        :class="{ active: selectedId === null }"
        @click="$emit('select', null)"
      >
        <span class="expand-icon-placeholder"></span>
        <span class="folder-icon">📂</span>
        <span class="node-name">未分类</span>
        <span class="entry-count">({{ uncategorizedCount }})</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, props } from 'vue'

const props = defineProps({
  categories: { type: Array, default: () => [] },
  selectedId: { type: String, default: null },
  expandedIds: { type: Set, default: () => new Set() },
  uncategorizedCount: { type: Number, default: 0 }
})

const emit = defineEmits(['select', 'toggle-expand', 'add-category'])

function selectCategory(category) {
  emit('select', category.id)
}

function toggleExpand(id) {
  emit('toggle-expand', id)
}
</script>

<style scoped>
.category-tree {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ec;
}

.tree-header h4 {
  margin: 0;
  font-size: 14px;
  color: #344054;
}

.add-btn {
  background: #667eea;
  color: white;
  border: none;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}

.tree-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.node-item {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  cursor: pointer;
  border-radius: 6px;
  margin: 2px 8px;
  font-size: 13px;
}

.node-item:hover {
  background: #f4f5f7;
}

.node-item.active {
  background: #eef2ff;
  color: #4052b5;
}

.expand-icon {
  width: 16px;
  font-size: 10px;
  color: #667085;
  cursor: pointer;
}

.expand-icon-placeholder {
  width: 16px;
}

.folder-icon {
  margin: 0 6px;
}

.node-name {
  flex: 1;
}

.entry-count {
  color: #98a2b3;
  font-size: 11px;
  margin-left: 4px;
}

.uncategorized {
  color: #667085;
  margin-top: 8px;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/knowledge/CategoryTree.vue
git commit -m "feat(frontend): add CategoryTree component"
```

---

### Task 11: 创建 TagInput 组件

**Files:**
- Create: `frontend/src/components/knowledge/TagInput.vue`

- [ ] **Step 1: 创建 TagInput.vue**

```vue
<template>
  <div class="tag-input">
    <div class="tags">
      <span
        v-for="tag in tags"
        :key="tag.id"
        class="tag"
      >
        {{ tag.name }}
        <button class="remove-btn" @click="removeTag(tag.id)">×</button>
      </span>
      <input
        v-model="newTagName"
        class="tag-input-field"
        placeholder="输入标签后回车"
        @keydown.enter="addTag"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, props } from 'vue'

const props = defineProps({
  tags: { type: Array, default: () => [] }
})

const emit = defineEmits(['add', 'remove'])

const newTagName = ref('')

function addTag() {
  const name = newTagName.value.trim()
  if (name) {
    emit('add', name)
    newTagName.value = ''
  }
}

function removeTag(tagId) {
  emit('remove', tagId)
}
</script>

<style scoped>
.tag-input {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 8px 12px;
  background: white;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #f0fdf4;
  color: #16a34a;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.remove-btn {
  background: none;
  border: none;
  color: #16a34a;
  cursor: pointer;
  padding: 0;
  font-size: 14px;
  line-height: 1;
}

.remove-btn:hover {
  color: #dc2626;
}

.tag-input-field {
  border: none;
  outline: none;
  font-size: 12px;
  min-width: 100px;
  flex: 1;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/knowledge/TagInput.vue
git commit -m "feat(frontend): add TagInput component"
```

---

### Task 12: 创建 EntryDetailModal 组件

**Files:**
- Create: `frontend/src/components/knowledge/EntryDetailModal.vue`

- [ ] **Step 1: 创建 EntryDetailModal.vue**

```vue
<template>
  <div v-if="show" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <div>
          <input
            v-model="editData.title"
            class="title-input"
            placeholder="条目标题"
          />
        </div>
        <button class="close-btn" @click="$emit('close')">×</button>
      </div>

      <div class="modal-body">
        <div class="left-panel">
          <div class="section">
            <label>标签</label>
            <TagInput
              :tags="editData.tags"
              @add="handleAddTag"
              @remove="handleRemoveTag"
            />
          </div>

          <div class="section">
            <label>分类路径</label>
            <div class="category-paths">
              <span
                v-for="cat in editData.categories"
                :key="cat.id"
                class="category-tag"
              >
                {{ cat.path }}
                <button @click="removeCategory(cat.id)">×</button>
              </span>
              <button class="add-category-btn" @click="showCategorySelector = true">
                + 添加分类
              </button>
            </div>
          </div>

          <div class="section">
            <label>内容</label>
            <textarea
              v-model="editData.content"
              class="content-textarea"
              rows="12"
              placeholder="输入知识内容..."
            ></textarea>
          </div>

          <div class="actions">
            <button class="primary-btn" @click="saveEntry">保存修改</button>
            <button class="danger-btn" @click="deleteEntry">删除</button>
          </div>
        </div>

        <div class="right-panel">
          <label>关联知识图谱</label>
          <div class="graph-container">
            <div v-if="loadingGraph" class="loading">图谱加载中...</div>
            <div v-else-if="!graphData.nodes.length" class="empty-graph">
              <p>暂无关联图谱</p>
            </div>
            <div v-else class="graph-placeholder">
              <p>图谱展示区域</p>
              <p class="hint">节点: {{ graphData.nodes.length }}, 边: {{ graphData.edges.length }}</p>
            </div>
          </div>
          <div v-if="graphData.relatedEntries.length" class="related-entries">
            <label>相关条目</label>
            <div class="related-list">
              <span
                v-for="entry in graphData.relatedEntries"
                :key="entry"
                class="related-item"
              >
                {{ entry }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Category Selector Modal -->
    <div v-if="showCategorySelector" class="category-selector-overlay" @click.self="showCategorySelector = false">
      <div class="category-selector">
        <h4>选择分类</h4>
        <div class="category-list">
          <div
            v-for="cat in categoryTree"
            :key="cat.id"
            class="category-option"
            :style="{ paddingLeft: (cat.level * 16 + 12) + 'px' }"
            @click="addCategory(cat)"
          >
            {{ cat.name }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import TagInput from './TagInput.vue'
import { getEntryGraph, createTag, deleteTag, createCategory } from '@/api'

const props = defineProps({
  show: { type: Boolean, default: false },
  entry: { type: Object, default: null },
  categoryTree: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'save', 'delete'])

const loadingGraph = ref(false)
const showCategorySelector = ref(false)
const graphData = ref({ nodes: [], edges: [], relatedEntries: [] })

const editData = ref({
  title: '',
  content: '',
  tags: [],
  categories: []
})

watch(() => props.entry, (entry) => {
  if (entry) {
    editData.value = {
      title: entry.title || '',
      content: entry.content || '',
      tags: entry.tags || [],
      categories: entry.categories || []
    }
    loadGraph(entry.entryId)
  }
}, { immediate: true })

async function loadGraph(entryId) {
  if (!entryId) return
  loadingGraph.value = true
  try {
    const data = await getEntryGraph(entryId)
    graphData.value = data
  } catch (e) {
    console.error('Failed to load graph:', e)
    graphData.value = { nodes: [], edges: [], relatedEntries: [] }
  } finally {
    loadingGraph.value = false
  }
}

async function handleAddTag(tagName) {
  try {
    const tag = await createTag(tagName)
    if (!editData.value.tags.find(t => t.id === tag.id)) {
      editData.value.tags.push(tag)
    }
  } catch (e) {
    console.error('Failed to create tag:', e)
  }
}

async function handleRemoveTag(tagId) {
  editData.value.tags = editData.value.tags.filter(t => t.id !== tagId)
}

function addCategory(category) {
  if (!editData.value.categories.find(c => c.id === category.id)) {
    editData.value.categories.push(category)
  }
  showCategorySelector.value = false
}

function removeCategory(categoryId) {
  editData.value.categories = editData.value.categories.filter(c => c.id !== categoryId)
}

function saveEntry() {
  emit('save', {
    entryId: props.entry?.entryId,
    title: editData.value.title,
    content: editData.value.content,
    tagIds: editData.value.tags.map(t => t.id),
    categoryIds: editData.value.categories.map(c => c.id)
  })
}

function deleteEntry() {
  if (confirm('确定要删除这个知识条目吗？')) {
    emit('delete', props.entry?.entryId)
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 900px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e4e7ec;
}

.title-input {
  font-size: 20px;
  font-weight: 600;
  border: none;
  outline: none;
  width: 100%;
  color: #344054;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #667085;
}

.modal-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.left-panel {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
}

.right-panel {
  width: 350px;
  background: #f8fafc;
  padding: 20px;
  border-left: 1px solid #e4e7ec;
  overflow-y: auto;
}

.section {
  margin-bottom: 20px;
}

.section label {
  display: block;
  margin-bottom: 8px;
  color: #475467;
  font-size: 13px;
  font-weight: 500;
}

.category-paths {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.category-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #eef2ff;
  color: #4052b5;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.category-tag button {
  background: none;
  border: none;
  cursor: pointer;
  color: #4052b5;
  padding: 0;
}

.add-category-btn {
  background: none;
  border: 1px dashed #e4e7ec;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #667085;
  cursor: pointer;
}

.content-textarea {
  width: 100%;
  border: 2px solid #e4e7ec;
  border-radius: 8px;
  padding: 12px;
  font-size: 13px;
  resize: vertical;
  font-family: inherit;
}

.content-textarea:focus {
  border-color: #667eea;
  outline: none;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.primary-btn {
  background: #667eea;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.danger-btn {
  background: #fef3f2;
  color: #dc2626;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.graph-container {
  background: white;
  border: 1px solid #e4e7ec;
  border-radius: 12px;
  height: 250px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.loading, .empty-graph {
  color: #667085;
  font-size: 13px;
}

.hint {
  font-size: 12px;
  color: #98a2b3;
  margin-top: 4px;
}

.related-entries label {
  display: block;
  margin-bottom: 8px;
  color: #475467;
  font-size: 13px;
  font-weight: 500;
}

.related-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.related-item {
  background: #eef2ff;
  color: #667eea;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.category-selector-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
}

.category-selector {
  background: white;
  border-radius: 12px;
  padding: 20px;
  width: 300px;
  max-height: 400px;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.category-selector h4 {
  margin: 0 0 16px;
  color: #344054;
}

.category-option {
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 6px;
  font-size: 13px;
}

.category-option:hover {
  background: #f4f5f7;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/knowledge/EntryDetailModal.vue
git commit -m "feat(frontend): add EntryDetailModal with graph integration"
```

---

### Task 13: 重写 KnowledgeView.vue

**Files:**
- Modify: `frontend/src/views/KnowledgeView.vue`

- [ ] **Step 1: 重写 KnowledgeView.vue**

This is a complete rewrite of the existing component. See the full implementation in the actual file.

Key changes:
- Replace simple entry list with left category tree + right entry list layout
- Add EntryDetailModal for viewing/editing entries
- Add support for tags and categories in entry cards
- Wire up all new API calls

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/KnowledgeView.vue
git commit -m "feat(frontend): rewrite KnowledgeView with category tree and detail modal"
```

---

## 实施检查清单

完成所有任务后，验证以下功能：

- [ ] 后端：分类 CRUD API 正常工作
- [ ] 后端：标签 CRUD API 正常工作
- [ ] 后端：条目图谱 API 返回正确的图谱数据
- [ ] 前端：分类树正确显示层级结构
- [ ] 前端：点击分类过滤条目列表
- [ ] 前端：点击条目打开详情弹窗
- [ ] 前端：详情弹窗显示知识图谱
- [ ] 前端：可以编辑条目标题、内容、标签、分类
- [ ] 前端：新增条目可以选择分类和标签
