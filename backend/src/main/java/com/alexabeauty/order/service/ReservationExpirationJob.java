package com.alexabeauty.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job running every 60 seconds to release stock reservations
 * for orders whose expiration window has passed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpirationJob {

    private final OrderCheckoutService orderCheckoutService;

    @Scheduled(fixedDelay = 60000)
    public int expireReservations() {
        int count = orderCheckoutService.expireOverdueReservations();
        if (count > 0) {
            log.info("ReservationExpirationJob: Released reserved stock for {} expired orders.", count);
        }
        return count;
    }
}
