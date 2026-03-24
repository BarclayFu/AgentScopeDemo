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

        // Check for duplicate name - return existing if found
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