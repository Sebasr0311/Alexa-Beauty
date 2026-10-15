package com.alexabeauty.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductVariantRequest(
        @Size(max = 50, message = "El SKU no puede superar los 50 caracteres")
        String sku,

        @Size(max = 100, message = "El tono no puede superar los 100 caracteres")
        String shadeName,

        @Size(max = 100, message = "La presentación no puede superar los 100 caracteres")
        String presentation,

        @DecimalMin(value = "0.0", inclusive = true, message = "El precio de venta no puede ser negativo")
        BigDecimal salePrice,

        @DecimalMin(value = "0.0", inclusive = true, message = "El costo de referencia no puede ser negativo")
        BigDecimal referenceCost,

        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        Integer minStock,

        @Min(value = 0, message = "El stock en mano no puede ser negativo")
        Integer stockOnHand,

        Boolean active,
        @Size(max = 50) String shadeCode,
        @jakarta.validation.constraints.Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "El color debe tener formato #RRGGBB")
        String shadeHex,
        Boolean clearReferenceCost
) {
    public ProductVariantRequest(String sku, String shadeName, String presentation, BigDecimal salePrice,
            BigDecimal referenceCost, Integer minStock, Integer stockOnHand, Boolean active, String shadeCode, String shadeHex) {
        this(sku, shadeName, presentation, salePrice, referenceCost, minStock, stockOnHand, active, shadeCode, shadeHex, null);
    }

    public ProductVariantRequest(String sku, String shadeName, String presentation, BigDecimal salePrice,
            BigDecimal referenceCost, Integer minStock, Integer stockOnHand, Boolean active) {
        this(sku, shadeName, presentation, salePrice, referenceCost, minStock, stockOnHand, active, null, null, null);
    }
}
