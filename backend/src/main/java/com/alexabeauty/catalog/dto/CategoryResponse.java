package com.alexabeauty.catalog.dto;

import com.alexabeauty.catalog.domain.Category;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        int sortOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static CategoryResponse from(Category category) {
        if (category == null) return null;
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getSortOrder(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
