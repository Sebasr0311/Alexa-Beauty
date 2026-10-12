package com.alexabeauty.catalog.dto;

import com.alexabeauty.catalog.domain.Brand;

import java.time.Instant;
import java.util.UUID;

public record BrandResponse(
        UUID id,
        String name,
        String slug,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static BrandResponse from(Brand brand) {
        if (brand == null) return null;
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getSlug(),
                brand.isActive(),
                brand.getCreatedAt(),
                brand.getUpdatedAt()
        );
    }
}
