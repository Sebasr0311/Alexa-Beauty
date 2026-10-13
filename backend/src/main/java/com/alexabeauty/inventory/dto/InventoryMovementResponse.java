package com.alexabeauty.inventory.dto;

import com.alexabeauty.inventory.domain.InventoryMovement;
import com.alexabeauty.inventory.domain.MovementType;

import java.time.Instant;
import java.util.UUID;

public record InventoryMovementResponse(
        UUID id,
        UUID variantId,
        String sku,
        String productName,
        String shadeName,
        MovementType movementType,
        int quantityDelta,
        int qtyBefore,
        int qtyAfter,
        String reference,
        String reason,
        String actorEmail,
        Instant createdAt
) {
    public static InventoryMovementResponse from(InventoryMovement movement) {
        if (movement == null) return null;
        String sku = movement.getVariant() != null ? movement.getVariant().getSku() : null;
        String productName = (movement.getVariant() != null && movement.getVariant().getProduct() != null)
                ? movement.getVariant().getProduct().getName() : null;
        String shadeName = movement.getVariant() != null ? movement.getVariant().getShadeName() : null;
        String actorEmail = movement.getUser() != null ? movement.getUser().getEmail() : null;

        return new InventoryMovementResponse(
                movement.getId(),
                movement.getVariant() != null ? movement.getVariant().getId() : null,
                sku,
                productName,
                shadeName,
                movement.getMovementType(),
                movement.getQuantityDelta(),
                movement.getQtyBefore(),
                movement.getQtyAfter(),
                movement.getReference(),
                movement.getReason(),
                actorEmail,
                movement.getCreatedAt()
        );
    }
}
