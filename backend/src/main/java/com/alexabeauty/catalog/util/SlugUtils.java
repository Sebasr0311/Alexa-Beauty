package com.alexabeauty.catalog.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTI_DASH = Pattern.compile("-+");

    private SlugUtils() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "item";
        }
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = MULTI_DASH.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase(Locale.ROOT);
        if (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        if (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        return slug.isEmpty() ? "item" : slug;
    }

    public static String generateUniqueSlug(String baseName, Predicate<String> slugExistsPredicate) {
        String baseSlug = toSlug(baseName);
        if (!slugExistsPredicate.test(baseSlug)) {
            return baseSlug;
        }
        int suffix = 2;
        while (true) {
            String candidate = baseSlug + "-" + suffix;
            if (!slugExistsPredicate.test(candidate)) {
                return candidate;
            }
            suffix++;
        }
    }
}
