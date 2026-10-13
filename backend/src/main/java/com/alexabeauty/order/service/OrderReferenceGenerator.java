package com.alexabeauty.order.service;

import com.alexabeauty.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique, sequential public order references in format AB-yyyy-NNNNNN (e.g. AB-2026-000101).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReferenceGenerator {

    private final OrderRepository orderRepository;
    private final AtomicInteger currentYear = new AtomicInteger(0);
    private final AtomicLong currentCounter = new AtomicLong(0);

    public synchronized String generateReference() {
        int year = LocalDate.now(ZoneId.of("America/Bogota")).getYear();
        if (currentYear.get() != year) {
            currentYear.set(year);
            String prefix = "AB-" + year + "-";
            long maxExisting = orderRepository.findMaxReferenceByPrefix(prefix + "%")
                    .map(maxRef -> {
                        try {
                            String numPart = maxRef.substring(prefix.length());
                            return Long.parseLong(numPart);
                        } catch (Exception e) {
                            return 0L;
                        }
                    })
                    .orElse(0L);
            currentCounter.set(maxExisting);
        }

        long nextNumber = currentCounter.incrementAndGet();
        String candidate = String.format("AB-%04d-%06d", year, nextNumber);
        while (orderRepository.existsByReference(candidate)) {
            nextNumber = currentCounter.incrementAndGet();
            candidate = String.format("AB-%04d-%06d", year, nextNumber);
        }
        return candidate;
    }
}
