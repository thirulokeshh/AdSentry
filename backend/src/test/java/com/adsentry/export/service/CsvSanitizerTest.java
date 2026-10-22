package com.adsentry.export.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CsvSanitizerTest {

    @Test
    @DisplayName("Null and empty inputs return empty strings")
    void testNullAndEmpty() {
        assertEquals("", CsvSanitizer.sanitize(null));
        assertEquals("", CsvSanitizer.sanitize(""));
        assertEquals("", CsvSanitizer.escapeField(null));
        assertEquals("", CsvSanitizer.escapeField(""));
    }

    @Test
    @DisplayName("Dangerous formula prefixes are prepended with a single quote")
    void testFormulaInjectionPrefixes() {
        assertEquals("'=1+2", CsvSanitizer.sanitize("=1+2"));
        assertEquals("'+cmd|' /C calc'!A0", CsvSanitizer.sanitize("+cmd|' /C calc'!A0"));
        assertEquals("'@SUM(A1:A10)", CsvSanitizer.sanitize("@SUM(A1:A10)"));
        assertEquals("'\tTabSeparated", CsvSanitizer.sanitize("\tTabSeparated"));
        assertEquals("'\rCarriageReturn", CsvSanitizer.sanitize("\rCarriageReturn"));
    }

    @Test
    @DisplayName("Standard negative numbers are not modified as formulas")
    void testStandardNegativeNumbers() {
        assertEquals("-15.50", CsvSanitizer.sanitize("-15.50"));
        assertEquals("-100", CsvSanitizer.sanitize("-100"));
    }

    @Test
    @DisplayName("Non-numeric strings starting with dash are sanitized")
    void testNonNumericDashPrefix() {
        assertEquals("'-calc()", CsvSanitizer.sanitize("-calc()"));
    }

    @Test
    @DisplayName("RFC-4180 quotes fields containing commas, quotes, or newlines")
    void testRfc4180Escaping() {
        // Commas
        assertEquals("\"Google Ads, Search\"", CsvSanitizer.escapeField("Google Ads, Search"));

        // Double quotes inside string are escaped to double-double-quotes
        assertEquals("\"He said \"\"Hello\"\"\"", CsvSanitizer.escapeField("He said \"Hello\""));

        // Newlines
        assertEquals("\"Line 1\nLine 2\"", CsvSanitizer.escapeField("Line 1\nLine 2"));

        // Formula injection combined with commas
        assertEquals("\"'=1+2, dangerous\"", CsvSanitizer.escapeField("=1+2, dangerous"));
    }
}
