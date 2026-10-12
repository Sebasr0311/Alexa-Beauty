package com.alexabeauty.catalog.dto;

import com.alexabeauty.catalog.domain.ProductImage;

import java.time.Instant;
import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        UUID productId,
        String storagePath,
        String url,
        String altText,
        int sortOrder,
        boolean isCover,
        Instant createdAt
) {
    public static ProductImageResponse from(ProductImage image, String publicUrl) {
        if (image == null) return null;
        return new ProductImageResponse(
                image.getId(),
                image.getProduct() != null ? image.getProduct().getId() : null,
                image.getStoragePath(),
                publicUrl,
                image.getAltText(),
                image.getSortOrder(),
                image.isCover(),
                image.getCreatedAt()
        );
    }
}
