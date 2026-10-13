package com.alexabeauty.reporting.dto;

import java.math.BigDecimal;

/**
 * Preview summary for an individual validated row.
 */
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
        boolean publicado
) {
}
