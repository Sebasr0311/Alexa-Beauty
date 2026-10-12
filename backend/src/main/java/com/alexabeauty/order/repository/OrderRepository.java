package com.alexabeauty.order.repository;

import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
