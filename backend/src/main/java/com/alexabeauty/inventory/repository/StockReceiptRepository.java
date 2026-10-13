package com.alexabeauty.inventory.repository;

import com.alexabeauty.inventory.domain.StockReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockReceiptRepository extends JpaRepository<StockReceipt, UUID> {
    Optional<StockReceipt> findByReceiptNumber(String receiptNumber);
    boolean existsByReceiptNumber(String receiptNumber);
    Page<StockReceipt> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
