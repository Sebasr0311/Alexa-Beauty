package com.alexabeauty.inventory.dto;

import com.alexabeauty.inventory.domain.StockReceipt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record StockReceiptResponse(
        UUID id,
        String receiptNumber,
        String providerName,
        String invoiceNumber,
        int totalUnits,
        BigDecimal totalCost,
        String notes,
        String actorEmail,
        List<StockReceiptItemResponse> items,
        Instant createdAt
) {
    public static StockReceiptResponse from(StockReceipt receipt) {
        if (receipt == null) return null;
        List<StockReceiptItemResponse> items = receipt.getItems() != null
                ? receipt.getItems().stream().map(StockReceiptItemResponse::from).toList()
                : Collections.emptyList();
        String actorEmail = receipt.getUser() != null ? receipt.getUser().getEmail() : null;

        return new StockReceiptResponse(
                receipt.getId(),
                receipt.getReceiptNumber(),
                receipt.getProviderName(),
                receipt.getInvoiceNumber(),
                receipt.getTotalUnits(),
                receipt.getTotalCost(),
                receipt.getNotes(),
                actorEmail,
                items,
                receipt.getCreatedAt()
        );
    }
}
