package com.alexabeauty.inventory.repository;

import com.alexabeauty.inventory.domain.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID>, JpaSpecificationExecutor<InventoryMovement> {
    List<InventoryMovement> findByVariantIdOrderByCreatedAtDesc(UUID variantId);
    Page<InventoryMovement> findByVariantIdOrderByCreatedAtDesc(UUID variantId, Pageable pageable);
}
