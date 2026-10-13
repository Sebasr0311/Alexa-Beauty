package com.alexabeauty.inventory.service;

import com.alexabeauty.inventory.domain.MovementType;
import com.alexabeauty.inventory.dto.CreateStockReceiptRequest;
import com.alexabeauty.inventory.dto.InventoryMovementResponse;
import com.alexabeauty.inventory.dto.InventorySummaryResponse;
import com.alexabeauty.inventory.dto.StockAdjustmentRequest;
import com.alexabeauty.inventory.dto.StockAdjustmentResponse;
import com.alexabeauty.inventory.dto.StockReceiptResponse;
import com.alexabeauty.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface InventoryService {

    StockReceiptResponse createReceipt(CreateStockReceiptRequest request, String idempotencyKey, UserPrincipal actor);

    StockAdjustmentResponse adjustStock(StockAdjustmentRequest request, UserPrincipal actor);

    Page<InventoryMovementResponse> getKardexMovements(UUID variantId, MovementType type, Instant from, Instant to, Pageable pageable);

    Page<InventorySummaryResponse> getInventorySummary(String query, Boolean lowStockOnly, Pageable pageable);

    StockReceiptResponse getReceiptById(UUID id);

    Page<StockReceiptResponse> listReceipts(Pageable pageable);
}
