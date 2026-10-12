package com.alexabeauty.inventory.repository;

import com.alexabeauty.inventory.domain.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
    List<InventoryMovement> findByVariantIdOrderByCreatedAtDesc(UUID variantId);
}
