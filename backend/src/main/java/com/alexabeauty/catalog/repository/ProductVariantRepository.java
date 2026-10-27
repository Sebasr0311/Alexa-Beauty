package com.alexabeauty.catalog.repository;

import com.alexabeauty.catalog.domain.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID>, JpaSpecificationExecutor<ProductVariant> {
    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByProductId(UUID productId);
    boolean existsBySku(String sku);
    @Query("SELECT pv.id FROM ProductVariant pv WHERE pv.product.id = :productId ORDER BY pv.id")
    List<UUID> findIdsByProductId(@Param("productId") UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id IN :ids ORDER BY pv.id ASC")
    List<ProductVariant> findByIdInOrderByIdForUpdate(@Param("ids") List<UUID> ids);

    @Query(value = "SELECT pv FROM ProductVariant pv " +
            "JOIN FETCH pv.product p " +
            "LEFT JOIN FETCH p.brand b " +
            "LEFT JOIN FETCH p.category c " +
            "WHERE p.deletedAt IS NULL AND (:query IS NULL OR :query = '' " +
            "       OR LOWER(pv.sku) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "       OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "       OR LOWER(pv.shadeName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND (:lowStockOnly = false OR (pv.stockOnHand - pv.stockReserved) <= pv.minStock)",
            countQuery = "SELECT COUNT(pv) FROM ProductVariant pv " +
            "JOIN pv.product p " +
            "WHERE p.deletedAt IS NULL AND (:query IS NULL OR :query = '' " +
            "       OR LOWER(pv.sku) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "       OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "       OR LOWER(pv.shadeName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND (:lowStockOnly = false OR (pv.stockOnHand - pv.stockReserved) <= pv.minStock)")
    Page<ProductVariant> findInventorySummary(
            @Param("query") String query,
            @Param("lowStockOnly") boolean lowStockOnly,
            Pageable pageable);

    @Query("SELECT pv FROM ProductVariant pv " +
           "JOIN FETCH pv.product p " +
           "LEFT JOIN FETCH p.brand b " +
           "LEFT JOIN FETCH p.category c " +
           "WHERE p.deletedAt IS NULL " +
           "ORDER BY p.name ASC, pv.sku ASC")
    List<ProductVariant> findAllForInventoryExport();
}
