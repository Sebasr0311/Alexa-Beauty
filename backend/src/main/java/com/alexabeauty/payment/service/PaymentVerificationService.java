package com.alexabeauty.payment.service;

import com.alexabeauty.payment.dto.ConfirmPaymentRequest;
import com.alexabeauty.payment.dto.PaymentResponse;
import com.alexabeauty.security.UserPrincipal;

import java.util.UUID;

public interface PaymentVerificationService {

    /**
     * Verifies payment for an order, atomically consumes reserved inventory,
     * transitions status to PAGADO, and records movement ledger entries.
     * Idempotent against double confirmations.
     */
    PaymentResponse confirmPayment(UUID orderId, ConfirmPaymentRequest request, String idempotencyKey, UserPrincipal actor);
}
