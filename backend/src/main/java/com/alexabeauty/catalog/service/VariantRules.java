package com.alexabeauty.catalog.service;

import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.catalog.dto.ProductVariantRequest;
import com.alexabeauty.common.error.ValidationException;
import java.util.Locale;
import java.util.UUID;

public final class VariantRules {
    private VariantRules() {}
    public static String clean(String value) {
        if (value == null || value.isBlank()) return null;
        String result = value.strip().replaceAll("\\s+", " ");
        if (result.chars().anyMatch(c -> Character.isISOControl(c) || c == '<' || c == '>'))
            throw new ValidationException("Los campos de variante no admiten etiquetas ni caracteres de control.");
        return result;
    }
    public static String sku(String input) {
        String value = clean(input);
        if (value == null) return "ABV-" + UUID.randomUUID().toString().replace("-", "");
        if (!value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,49}"))
            throw new ValidationException("SKU: use letras, números, puntos, guiones o guiones bajos (máx. 50).");
        return value;
    }
    public static String key(String code, String name, String presentation) {
        String tone = clean(code) != null ? clean(code) : clean(name);
        return (tone == null ? "" : tone.toLowerCase(Locale.ROOT)) + "|" +
                (clean(presentation) == null ? "" : clean(presentation).toLowerCase(Locale.ROOT));
    }
    public static String key(ProductVariant v) { return key(v.getShadeCode(), v.getShadeName(), v.getPresentation()); }
    public static String key(ProductVariantRequest r) { return key(r.shadeCode(), r.shadeName(), r.presentation()); }
    public static void validateCommercialIdentity(String code, String name, String presentation) {
        if (clean(code) == null && clean(name) == null && clean(presentation) == null) {
            throw new ValidationException("Cada variante debe especificar al menos un identificador comercial: código de tono, nombre de tono o presentación.");
        }
    }
    public static void validateCommercialIdentity(ProductVariantRequest request) {
        if (request == null) throw new ValidationException("Los datos de la variante son obligatorios.");
        validateCommercialIdentity(request.shadeCode(), request.shadeName(), request.presentation());
    }
    public static void metadata(ProductVariant variant, ProductVariantRequest request) {
        String hex = clean(request.shadeHex());
        if (hex != null && !hex.matches("#[0-9a-fA-F]{6}")) throw new ValidationException("El color debe tener formato #RRGGBB.");
        variant.setShadeCode(clean(request.shadeCode()));
        variant.setShadeName(clean(request.shadeName()));
        variant.setPresentation(clean(request.presentation()));
        variant.setShadeHex(hex == null ? null : hex.toUpperCase(Locale.ROOT));
    }
}
