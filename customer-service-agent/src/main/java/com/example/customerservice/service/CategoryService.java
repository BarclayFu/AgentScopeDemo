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