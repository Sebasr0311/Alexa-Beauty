package com.alexabeauty.config.dto;

import java.time.Instant;

/**
 * Administrative configuration response including last modification timestamp.
 */
public record AdminSettingsResponse(String whatsappNumber, Instant updatedAt) {}
