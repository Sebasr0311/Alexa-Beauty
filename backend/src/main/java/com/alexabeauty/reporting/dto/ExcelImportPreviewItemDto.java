package com.alexabeauty.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Preview summary for an individual validated row (supports V1 and V2 fields).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExcelImportPreviewItemDto(
        int rowNumber,
        String nombre,
        String marca,
        int stockInicial,
        BigDecimal precioVentaCop,
        String sku,
        String categoria,
        String tono,
        String presentacion,
        BigDecimal costoUnitarioCop,
        int stockMinimo,
        String descripcion,
        boolean publicado,
        String productCode,
        String shadeCode,
        String shadeName,
        String shadeHex
) {
    public ExcelImportPreviewItemDto(
            int rowNumber,
            String nombre,
            String marca,
            int stockInicial,
            BigDecimal precioVentaCop,
            String sku,
            String categoria,
            String tono,
            String presentacion,
            BigDecimal costoUnitarioCop,
            int stockMinimo,
            String descripcion,
            boolean publicado
    ) {
        this(rowNumber, nombre, marca, stockInicial, precioVentaCop, sku, categoria, tono, presentacion, costoUnitarioCop, stockMinimo, descripcion, publicado, null, null, tono, null);
    }
}
