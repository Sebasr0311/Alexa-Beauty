package com.alexabeauty.config.service;

import com.alexabeauty.common.audit.AuditLog;
import com.alexabeauty.common.audit.AuditLogRepository;
import com.alexabeauty.common.error.ValidationException;
import com.alexabeauty.config.domain.AppSetting;
import com.alexabeauty.config.dto.AdminSettingsResponse;
import com.alexabeauty.config.dto.PublicStoreSettingsResponse;
import com.alexabeauty.config.repository.AppSettingRepository;
import com.alexabeauty.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Resolves and updates dynamic store settings, treating {@code app_settings} as the primary source of truth.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessSettingsServiceImpl implements BusinessSettingsService {

    public static final String WHATSAPP_NUMBER_KEY = "whatsapp_number";
    private static final Pattern WHATSAPP_PATTERN = Pattern.compile("^[1-9][0-9]{9,14}$");
    private static final Pattern COLOMBIA_MOBILE_PATTERN = Pattern.compile("^3[0-9]{9}$");

    private final AppSettingRepository appSettingRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${app.whatsapp-number:}")
    private String fallbackBusinessPhone;

    @Override
    @Transactional(readOnly = true)
    public String resolveRequiredWhatsAppNumber() {
        return findWhatsAppNumber()
                .orElseThrow(() -> new ValidationException("El contacto WhatsApp del negocio no está configurado."));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findWhatsAppNumber() {
        Optional<String> fromSetting = appSettingRepository.findBySettingKey(WHATSAPP_NUMBER_KEY)
                .map(AppSetting::getSettingValue)
                .map(String::trim)
                .filter(value -> WHATSAPP_PATTERN.matcher(value).matches());

        if (fromSetting.isPresent()) {
            return fromSetting;
        }

        if (fallbackBusinessPhone != null) {
            String trimmedFallback = fallbackBusinessPhone.trim();
            if (WHATSAPP_PATTERN.matcher(trimmedFallback).matches()) {
                return Optional.of(trimmedFallback);
            }
        }

        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicStoreSettingsResponse getPublicSettings() {
        return new PublicStoreSettingsResponse(findWhatsAppNumber().orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSettingsResponse getAdminSettings() {
        Optional<AppSetting> setting = appSettingRepository.findBySettingKey(WHATSAPP_NUMBER_KEY);
        return new AdminSettingsResponse(
                findWhatsAppNumber().orElse(null),
                setting.map(AppSetting::getUpdatedAt).orElse(null)
        );
    }

    @Override
    @Transactional
    public AdminSettingsResponse updateWhatsAppNumber(String rawNumber, UserPrincipal actor) {
        String normalized = normalizeAndValidateWhatsAppNumber(rawNumber);

        AppSetting setting = appSettingRepository.findBySettingKey(WHATSAPP_NUMBER_KEY)
                .orElseGet(() -> AppSetting.builder()
                        .settingKey(WHATSAPP_NUMBER_KEY)
                        .description("Número oficial de WhatsApp para pedidos")
                        .build());

        String previousValue = setting.getSettingValue();
        setting.setSettingValue(normalized);
        setting.setUpdatedAt(Instant.now());
        AppSetting saved = appSettingRepository.save(setting);

        String actorEmail = actor != null ? actor.getUsername() : "sistema";
        AuditLog auditLog = AuditLog.builder()
                .actorEmail(actorEmail)
                .action("UPDATE_WHATSAPP_NUMBER")
                .entityType("APP_SETTING")
                .entityId(saved.getId().toString())
                .beforeSnapshot(previousValue)
                .afterSnapshot(normalized)
                .build();
        auditLogRepository.save(auditLog);

        log.info("Updated store WhatsApp number by actor='{}': '{}' -> '{}'", actorEmail, previousValue, normalized);

        return new AdminSettingsResponse(saved.getSettingValue(), saved.getUpdatedAt());
    }

    @Override
    public String normalizeAndValidateWhatsAppNumber(String rawNumber) {
        if (rawNumber == null || rawNumber.isBlank()) {
            throw new ValidationException("El número de WhatsApp es requerido.");
        }

        String digits = rawNumber.replaceAll("[^0-9]", "");
        if (COLOMBIA_MOBILE_PATTERN.matcher(digits).matches()) {
            digits = "57" + digits;
        }

        if (!WHATSAPP_PATTERN.matcher(digits).matches()) {
            throw new ValidationException(
                    "Ingresa un número de WhatsApp válido (10 dígitos en Colombia o código de país + número, entre 10 y 15 dígitos)."
            );
        }

        return digits;
    }
}
