package com.alexabeauty.config.service;

import com.alexabeauty.config.dto.AdminSettingsResponse;
import com.alexabeauty.config.dto.PublicStoreSettingsResponse;
import com.alexabeauty.security.UserPrincipal;

import java.util.Optional;

/**
 * Service for reading and updating store operational settings backed by {@code app_settings}.
 */
public interface BusinessSettingsService {

    String resolveRequiredWhatsAppNumber();

    Optional<String> findWhatsAppNumber();

    PublicStoreSettingsResponse getPublicSettings();

    AdminSettingsResponse getAdminSettings();

    AdminSettingsResponse updateWhatsAppNumber(String rawNumber, UserPrincipal actor);

    String normalizeAndValidateWhatsAppNumber(String rawNumber);
}
