package com.alexabeauty.order.repository;

import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByReference(String reference);
    Optional<Order> findByTrackingTokenHash(String trackingTokenHash);
    List<Order> findByStatus(OrderStatus status);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    boolean existsByReference(String reference);

    @Query("SELECT o FROM Order o WHERE o.status IN :statuses AND o.expiresAt < :cutoff")
    List<Order> findByStatusInAndExpiresAtBefore(@Param("statuses") List<OrderStatus> statuses, @Param("cutoff") Instant cutoff);

    @Query("SELECT MAX(o.reference) FROM Order o WHERE o.reference LIKE :prefix")
    Optional<String> findMaxReferenceByPrefix(@Param("prefix") String prefix);

    @Query("SELECT o FROM Order o WHERE o.customerNotes LIKE %:idTag%")
    Optional<Order> findByIdempotencyTag(@Param("idTag") String idTag);
}
