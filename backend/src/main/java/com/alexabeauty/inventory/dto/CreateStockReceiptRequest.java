package com.alexabeauty.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateStockReceiptRequest(
        String receiptNumber,
        String providerName,
        String invoiceNumber,
        String notes,

        @NotEmpty(message = "La recepción debe incluir al menos un ítem")
        @Valid
        List<StockReceiptItemRequest> items
) {
}
