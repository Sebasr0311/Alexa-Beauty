package com.alexabeauty.order.repository;

import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    Optional<Order> findByReference(String reference);
    Optional<Order> findByTrackingTokenHash(String trackingTokenHash);
    List<Order> findByStatus(OrderStatus status);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    boolean existsByReference(String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT o FROM Order o WHERE o.status IN :statuses AND o.expiresAt < :cutoff")
    List<Order> findByStatusInAndExpiresAtBefore(@Param("statuses") List<OrderStatus> statuses, @Param("cutoff") Instant cutoff);

    @Query("SELECT MAX(o.reference) FROM Order o WHERE o.reference LIKE :prefix")
    Optional<String> findMaxReferenceByPrefix(@Param("prefix") String prefix);

    @Query("SELECT o FROM Order o WHERE o.customerNotes LIKE %:idTag%")
    Optional<Order> findByIdempotencyTag(@Param("idTag") String idTag);

    @Query(value = "SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(o.reference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(o.customerPhone) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:from IS NULL OR o.createdAt >= :from) AND " +
           "(:to IS NULL OR o.createdAt <= :to)",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(o.reference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(o.customerPhone) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:from IS NULL OR o.createdAt >= :from) AND " +
           "(:to IS NULL OR o.createdAt <= :to)")
    Page<Order> findFilteredOrders(
            @Param("status") OrderStatus status,
            @Param("search") String search,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
