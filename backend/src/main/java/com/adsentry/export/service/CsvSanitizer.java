package com.adsentry.export.service;

import java.util.Set;

/**
 * Utility for formatting RFC-4180 compliant CSV fields with mitigation
 * against spreadsheet formula injection (CSV Excel Macro Injection).
 */
public final class CsvSanitizer {

    private static final Set<Character> DANGEROUS_PREFIXES = Set.of('=', '+', '-', '@', '\t', '\r');

    private CsvSanitizer() {
    }

    /**
     * Sanitizes a string value to prevent spreadsheet formula injection.
     * If the text begins with dangerous prefix characters, prepends a single quote (').
     */
    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        char firstChar = value.charAt(0);
        if (DANGEROUS_PREFIXES.contains(firstChar)) {
            // Check if it's a plain standard negative number (e.g. -12.34 or -5)
            if (firstChar == '-' && isNumeric(value)) {
                return value;
            }
            return "'" + value;
        }
        return value;
    }

    /**
     * Escapes a value according to RFC-4180 rules.
     * Encloses the value in quotes if it contains commas, double quotes, newlines, or semicolons.
     * Internal quotes are escaped as double-quotes ("").
     */
    public static String escapeField(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        String sanitized = sanitize(rawValue);
        boolean needsQuotes = sanitized.contains(",") ||
                sanitized.contains("\"") ||
                sanitized.contains("\n") ||
                sanitized.contains("\r") ||
                sanitized.startsWith(" ") ||
                sanitized.endsWith(" ");

        if (needsQuotes) {
            return "\"" + sanitized.replace("\"", "\"\"") + "\"";
        }
        return sanitized;
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
