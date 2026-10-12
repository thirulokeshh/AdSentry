package com.adsentry.importing.service.currency;

import com.adsentry.importing.service.csv.CsvRow;
import com.adsentry.importing.service.csv.CsvTable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service for currency detection, validation, and numeric monetary extraction.
 *
 * Rules:
 * 1. INR is the supported MVP product currency.
 * 2. Literal ₹ is NOT required on every row; plain numbers with reliable confirmation or explicit
 *    configuration are supported.
 * 3. Unsupported currencies (USD, EUR, GBP, etc.) are strictly rejected.
 * 4. Conflicting or ambiguous currency signals must be flagged and rejected.
 */
@Service
public class CurrencyService {

    public static final String SUPPORTED_CURRENCY = "INR";

    private static final Pattern USD_PATTERN = Pattern.compile("(?i)(\\$|\\busd\\b)");
    private static final Pattern EUR_PATTERN = Pattern.compile("(?i)(€|\\beur\\b)");
    private static final Pattern GBP_PATTERN = Pattern.compile("(?i)(£|\\bgbp\\b)");
    private static final Pattern JPY_PATTERN = Pattern.compile("(?i)(¥|\\bjpy\\b)");

    private static final Pattern INR_PATTERN = Pattern.compile("(?i)(₹|\\binr\\b|\\brs\\.|\\brs\\b)");

    public CurrencyDetectionResult detectCurrency(CsvTable table, String spendColumn, String explicitCurrency) {
        List<String> warnings = new ArrayList<>();
        Set<String> detectedCurrencies = new HashSet<>();

        // 1. If explicit currency provided by user/config
        if (explicitCurrency != null && !explicitCurrency.isBlank()) {
            String norm = explicitCurrency.trim().toUpperCase();
            if (!SUPPORTED_CURRENCY.equals(norm)) {
                return new CurrencyDetectionResult(
                        norm,
                        false,
                        false,
                        false,
                        "EXPLICIT_CONFIG",
                        List.of("User specified unsupported currency: " + norm),
                        "Unsupported currency: '" + norm + "'. AdSentry MVP only supports INR."
                );
            }
        }

        // 2. Scan headers for currency indicators
        for (String header : table.getHeaders()) {
            if (USD_PATTERN.matcher(header).find()) detectedCurrencies.add("USD");
            if (EUR_PATTERN.matcher(header).find()) detectedCurrencies.add("EUR");
            if (GBP_PATTERN.matcher(header).find()) detectedCurrencies.add("GBP");
            if (JPY_PATTERN.matcher(header).find()) detectedCurrencies.add("JPY");
            if (INR_PATTERN.matcher(header).find()) detectedCurrencies.add("INR");
        }

        // 3. Scan sample rows in spend column if available
        if (spendColumn != null) {
            int scanLimit = Math.min(table.getRowCount(), 100);
            for (int i = 0; i < scanLimit; i++) {
                CsvRow row = table.getRows().get(i);
                String val = row.get(spendColumn);
                if (val != null && !val.isBlank()) {
                    if (USD_PATTERN.matcher(val).find()) detectedCurrencies.add("USD");
                    if (EUR_PATTERN.matcher(val).find()) detectedCurrencies.add("EUR");
                    if (GBP_PATTERN.matcher(val).find()) detectedCurrencies.add("GBP");
                    if (JPY_PATTERN.matcher(val).find()) detectedCurrencies.add("JPY");
                    if (INR_PATTERN.matcher(val).find()) detectedCurrencies.add("INR");
                }
            }
        }

        // Check for conflicting currencies
        if (detectedCurrencies.size() > 1) {
            return new CurrencyDetectionResult(
                    "AMBIGUOUS",
                    false,
                    false,
                    true,
                    "CONTENT_SCAN",
                    List.of("Multiple conflicting currency symbols detected in file: " + detectedCurrencies),
                    "Conflicting currencies detected in file: " + detectedCurrencies + ". Cannot safely compare mixed currencies."
            );
        }

        if (detectedCurrencies.contains("USD") || detectedCurrencies.contains("EUR")
                || detectedCurrencies.contains("GBP") || detectedCurrencies.contains("JPY")) {
            String unsupp = detectedCurrencies.iterator().next();
            return new CurrencyDetectionResult(
                    unsupp,
                    false,
                    false,
                    false,
                    "CONTENT_SCAN",
                    List.of("Unsupported foreign currency symbol detected: " + unsupp),
                    "Unsupported currency detected: '" + unsupp + "'. AdSentry MVP only supports INR (₹)."
            );
        }

        if (detectedCurrencies.contains("INR")) {
            return new CurrencyDetectionResult(
                    "INR",
                    true,
                    true,
                    false,
                    "CONTENT_SCAN",
                    warnings,
                    null
            );
        }

        // Plain numbers without symbols:
        boolean isConfirmed = (explicitCurrency != null && SUPPORTED_CURRENCY.equalsIgnoreCase(explicitCurrency.trim()));
        if (!isConfirmed) {
            warnings.add("No currency symbol found in headers or values. Numbers will be treated as INR upon confirmation.");
        }

        return new CurrencyDetectionResult(
                "INR",
                true,
                isConfirmed,
                false,
                isConfirmed ? "EXPLICIT_CONFIG" : "DEFAULT_UNCONFIRMED",
                warnings,
                null
        );
    }

    /**
     * Cleans and parses a monetary string into BigDecimal.
     * Returns null if missing / empty / N/A / dash.
     */
    public BigDecimal parseMonetaryValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String clean = raw.trim();
        if ("-".equals(clean) || "n/a".equalsIgnoreCase(clean) || "null".equalsIgnoreCase(clean)) {
            return null;
        }

        // Remove INR symbols, currency codes, commas, and whitespace
        clean = INR_PATTERN.matcher(clean).replaceAll("");
        clean = clean.replace(",", "").replace(" ", "").trim();

        if (clean.isBlank()) {
            return null;
        }

        return new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
    }
}
