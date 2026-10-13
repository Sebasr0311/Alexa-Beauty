package com.alexabeauty.payment.repository;

import com.alexabeauty.payment.domain.Payment;
import com.alexabeauty.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByOrderId(UUID orderId);
    List<Payment> findByStatus(PaymentStatus status);
    Optional<Payment> findFirstByOrderIdAndStatus(UUID orderId, PaymentStatus status);
}
