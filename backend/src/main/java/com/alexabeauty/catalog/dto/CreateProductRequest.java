package com.alexabeauty.catalog.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank(message = "El nombre del producto es requerido")
        @Size(max = 200, message = "El nombre no puede superar los 200 caracteres")
        String name,

        @NotNull(message = "La marca es requerida")
        UUID brandId,

        @NotNull(message = "La categoría es requerida")
        UUID categoryId,

        String description,

        @Size(max = 50, message = "El SKU no puede superar los 50 caracteres")
        String sku,

        @NotNull(message = "El precio de venta es requerido")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio de venta no puede ser negativo")
        BigDecimal salePrice,

        @Min(value = 0, message = "El stock inicial no puede ser negativo")
        Integer stockOnHand,

        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        Integer minStock,

        @Valid
        List<ProductVariantRequest> variants,

        @JsonAlias("isFeatured")
        Boolean featured,

        Boolean active,

        @DecimalMin(value = "0.0", message = "El costo no puede ser negativo")
        BigDecimal referenceCost
) {
    public CreateProductRequest(String name, UUID brandId, UUID categoryId, String description, String sku,
            BigDecimal salePrice, Integer stockOnHand, Integer minStock, List<ProductVariantRequest> variants,
            Boolean featured, Boolean active) {
        this(name, brandId, categoryId, description, sku, salePrice, stockOnHand, minStock, variants, featured, active, null);
    }

    public boolean isFeaturedProduct() {
        return Boolean.TRUE.equals(featured);
    }

    public boolean isActiveProduct() {
        return active == null || active;
    }
}
