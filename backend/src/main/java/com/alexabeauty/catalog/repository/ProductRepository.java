package com.alexabeauty.catalog.repository;

import com.alexabeauty.catalog.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySku(String sku);
    boolean existsBySlug(String slug);
    boolean existsBySku(String sku);
    Page<Product> findByActiveTrue(Pageable pageable);
    Page<Product> findByCategoryIdAndActiveTrue(UUID categoryId, Pageable pageable);
    Page<Product> findByBrandIdAndActiveTrue(UUID brandId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT COUNT(*) > 0 FROM order_items oi JOIN product_variants pv ON oi.variant_id = pv.id WHERE pv.product_id = :productId",
        nativeQuery = true
    )
    boolean hasOrderItems(@org.springframework.data.repository.query.Param("productId") UUID productId);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT COUNT(*) > 0 FROM stock_receipt_items sri JOIN product_variants pv ON sri.variant_id = pv.id WHERE pv.product_id = :productId",
        nativeQuery = true
    )
    boolean hasStockReceipts(@org.springframework.data.repository.query.Param("productId") UUID productId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
        value = "DELETE FROM stock_reservations WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :productId)",
        nativeQuery = true
    )
    void deleteReservationsByProductId(@org.springframework.data.repository.query.Param("productId") UUID productId);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT COUNT(*) > 0 FROM inventory_movements im JOIN product_variants pv ON im.variant_id = pv.id WHERE pv.product_id = :productId",
        nativeQuery = true
    )
    boolean hasInventoryMovements(@org.springframework.data.repository.query.Param("productId") UUID productId);
}
