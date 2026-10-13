package com.alexabeauty.reporting.excel;

/**
 * Formula Sanitizer to protect against Excel / CSV formula injection (OWASP P07 matrix).
 * Prepends a single quote (') to cells starting with dangerous characters (=, +, -, @, \t, \r)
 * preventing Excel from evaluating them as executable expressions or system commands.
 */
public final class ExcelSanitizer {

    private static final char[] DANGEROUS_CHARS = {'=', '+', '-', '@', '\t', '\r'};

    private ExcelSanitizer() {
    }

    /**
     * Determines whether the given text begins with formula injection prefix characters.
     *
     * @param text input string
     * @return true if text starts with dangerous formula tokens
     */
    public static boolean isDangerous(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char first = text.charAt(0);
        for (char c : DANGEROUS_CHARS) {
            if (first == c) {
                return true;
            }
        }
        String trimmed = text.stripLeading();
        if (!trimmed.isEmpty()) {
            char trimmedFirst = trimmed.charAt(0);
            return trimmedFirst == '=' || trimmedFirst == '+' || trimmedFirst == '-' || trimmedFirst == '@';
        }
        return false;
    }

    /**
     * Sanitizes potentially malicious formula expressions by prepending a single quote.
     *
     * @param text cell text content
     * @return sanitized string safe for Excel export
     */
    public static String sanitize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (isDangerous(text)) {
            return "'" + text;
        }
        return text;
    }
}
