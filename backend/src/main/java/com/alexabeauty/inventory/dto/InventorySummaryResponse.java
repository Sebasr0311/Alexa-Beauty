package com.alexabeauty.inventory.dto;

import com.alexabeauty.catalog.domain.ProductImage;
import com.alexabeauty.catalog.domain.ProductVariant;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Function;

public record InventorySummaryResponse(
        UUID variantId,
        UUID productId,
        String sku,
        String productName,
        String brandName,
        String categoryName,
        String shadeName,
        String presentation,
        String shadeHex,
        BigDecimal salePrice,
        BigDecimal referenceCost,
        String imageUrl,
        int stockOnHand,
        int stockReserved,
        int availableStock,
        int minStock,
        boolean isLowStock,
        boolean active
) {
    public static InventorySummaryResponse from(ProductVariant variant) {
        return from(variant, Function.identity());
    }

    public static InventorySummaryResponse from(ProductVariant variant, Function<String, String> urlResolver) {
        if (variant == null) return null;
        int stockOnHand = variant.getStockOnHand();
        int stockReserved = variant.getStockReserved();
        int availableStock = stockOnHand - stockReserved;
        int minStock = variant.getMinStock();
        boolean isLowStock = availableStock <= minStock;

        UUID productId = variant.getProduct() != null ? variant.getProduct().getId() : null;
        String productName = variant.getProduct() != null ? variant.getProduct().getName() : null;
        String brandName = (variant.getProduct() != null && variant.getProduct().getBrand() != null)
                ? variant.getProduct().getBrand().getName() : null;
        String categoryName = (variant.getProduct() != null && variant.getProduct().getCategory() != null)
                ? variant.getProduct().getCategory().getName() : null;

        String imageUrl = null;
        if (variant.getProduct() != null && variant.getProduct().getImages() != null
                && !variant.getProduct().getImages().isEmpty()) {
            var images = variant.getProduct().getImages();
            Comparator<ProductImage> imgOrder = Comparator.comparing(ProductImage::isCover).reversed()
                    .thenComparingInt(ProductImage::getSortOrder);

            var variantImg = images.stream()
                    .filter(img -> img.getVariant() != null && variant.getId() != null
                            && variant.getId().equals(img.getVariant().getId()))
                    .sorted(imgOrder)
                    .findFirst();

            var chosen = variantImg.or(() -> images.stream()
                    .filter(img -> img.getVariant() == null)
                    .sorted(imgOrder)
                    .findFirst())
                    .or(() -> images.stream().sorted(imgOrder).findFirst());

            if (chosen.isPresent() && chosen.get().getStoragePath() != null) {
                String path = chosen.get().getStoragePath();
                imageUrl = urlResolver != null ? urlResolver.apply(path) : path;
            }
        }

        return new InventorySummaryResponse(
                variant.getId(),
                productId,
                variant.getSku(),
                productName,
                brandName,
                categoryName,
                variant.getShadeName(),
                variant.getPresentation(),
                variant.getShadeHex(),
                variant.getSalePrice(),
                variant.getReferenceCost(),
                imageUrl,
                stockOnHand,
                stockReserved,
                availableStock,
                minStock,
                isLowStock,
                variant.isActive()
        );
    }
}
