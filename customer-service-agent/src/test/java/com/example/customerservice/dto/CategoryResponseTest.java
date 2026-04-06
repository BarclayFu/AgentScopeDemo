package com.example.customerservice.dto;

import com.example.customerservice.entity.Category;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryResponseTest {

    @Test
    void shouldCreateCategoryResponseWithAllFields() {
        long now = System.currentTimeMillis();
        CategoryResponse response = new CategoryResponse(
            "cat-001",
            "Electronics",
            null,
            "/Electronics",
            1,
            now,
            now
        );

        assertEquals("cat-001", response.getId());
        assertEquals("Electronics", response.getName());
        assertNull(response.getParentId());
        assertEquals("/Electronics", response.getPath());
        assertEquals(1, response.getLevel());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }

    @Test
    void shouldCreateCategoryTreeResponseWithNestedNodes() {
        long now = System.currentTimeMillis();

        CategoryTreeResponse.CategoryNode childNode = new CategoryTreeResponse.CategoryNode(
            "cat-002",
            "Smartphones",
            "cat-001",
            "/Electronics/Smartphones",
            2,
            5,
            List.of()
        );

        CategoryTreeResponse.CategoryNode parentNode = new CategoryTreeResponse.CategoryNode(
            "cat-001",
            "Electronics",
            null,
            "/Electronics",
            1,
            10,
            Arrays.asList(childNode)
        );

        CategoryTreeResponse treeResponse = new CategoryTreeResponse(List.of(parentNode));

        assertEquals(1, treeResponse.getCategories().size());

        CategoryTreeResponse.CategoryNode retrievedParent = treeResponse.getCategories().get(0);
        assertEquals("cat-001", retrievedParent.getId());
        assertEquals("Electronics", retrievedParent.getName());
        assertNull(retrievedParent.getParentId());
        assertEquals("/Electronics", retrievedParent.getPath());
        assertEquals(1, retrievedParent.getLevel());
        assertEquals(10, retrievedParent.getEntryCount());
        assertEquals(1, retrievedParent.getChildren().size());

        CategoryTreeResponse.CategoryNode retrievedChild = retrievedParent.getChildren().get(0);
        assertEquals("cat-002", retrievedChild.getId());
        assertEquals("Smartphones", retrievedChild.getName());
        assertEquals("cat-001", retrievedChild.getParentId());
        assertEquals("/Electronics/Smartphones", retrievedChild.getPath());
        assertEquals(2, retrievedChild.getLevel());
        assertEquals(5, retrievedChild.getEntryCount());
        assertTrue(retrievedChild.getChildren().isEmpty());
    }

    @Test
    void shouldCreateCategoryRecord() {
        long now = System.currentTimeMillis();
        Category category = new Category(
            "cat-001",
            "Electronics",
            null,
            "/Electronics",
            1,
            now,
            now
        );

        assertEquals("cat-001", category.id());
        assertEquals("Electronics", category.name());
        assertNull(category.parentId());
        assertEquals("/Electronics", category.path());
        assertEquals(1, category.level());
        assertEquals(now, category.createdAt());
        assertEquals(now, category.updatedAt());
    }
}