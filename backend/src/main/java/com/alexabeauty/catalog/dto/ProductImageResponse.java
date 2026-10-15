package com.alexabeauty.catalog.dto;

import com.alexabeauty.catalog.domain.ProductImage;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductImageResponse(
        UUID id,
        UUID productId,
        UUID variantId,
        String storagePath,
        String url,
        String altText,
        int sortOrder,
        boolean isCover,
        Instant createdAt
) {
    public static ProductImageResponse from(ProductImage image, String publicUrl) {
        return from(image, publicUrl, false);
    }

    public static ProductImageResponse from(ProductImage image, String publicUrl, boolean publicView) {
        if (image == null) return null;
        return new ProductImageResponse(
                image.getId(),
                image.getProduct() != null ? image.getProduct().getId() : null,
                image.getVariant() != null ? image.getVariant().getId() : null,
                publicView ? null : image.getStoragePath(),
                publicUrl,
                image.getAltText(),
                image.getSortOrder(),
                image.isCover(),
                image.getCreatedAt()
        );
    }
}
