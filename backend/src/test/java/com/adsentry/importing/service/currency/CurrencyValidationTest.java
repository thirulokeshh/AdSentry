package com.adsentry.importing.service.currency;

import com.adsentry.importing.service.csv.CsvParser;
import com.adsentry.importing.service.csv.CsvTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyValidationTest {

    private final CsvParser parser = new CsvParser();
    private final CurrencyService currencyService = new CurrencyService();

    @Test
    @DisplayName("Literal rupee symbol (₹) is recognized as supported INR currency")
    void detectCurrency_literalRupeeSymbol() throws IOException {
        String csv = """
                date,campaign,spend
                2026-03-01,Brand-1,₹ 1500.00
                2026-03-02,Brand-1,₹ 2200.50
                """;
        CsvTable table = parser.parse(csv);

        CurrencyDetectionResult result = currencyService.detectCurrency(table, "spend", null);

        assertThat(result.isSupported()).isTrue();
        assertThat(result.isConfirmed()).isTrue();
        assertThat(result.getDetectedCurrency()).isEqualTo("INR");
    }

    @Test
    @DisplayName("Plain numbers without currency symbols supported with confirmation")
    void detectCurrency_plainNumbersWithConfirmation() throws IOException {
        String csv = """
                date,campaign,spend
                2026-03-01,Brand-1,1500.00
                2026-03-02,Brand-1,2200.50
                """;
        CsvTable table = parser.parse(csv);

        // When user confirms INR explicitly
        CurrencyDetectionResult confirmedResult = currencyService.detectCurrency(table, "spend", "INR");
        assertThat(confirmedResult.isSupported()).isTrue();
        assertThat(confirmedResult.isConfirmed()).isTrue();
        assertThat(confirmedResult.getDetectedCurrency()).isEqualTo("INR");

        // When unconfirmed
        CurrencyDetectionResult unconfirmedResult = currencyService.detectCurrency(table, "spend", null);
        assertThat(unconfirmedResult.isSupported()).isTrue();
        assertThat(unconfirmedResult.isConfirmed()).isFalse();
        assertThat(unconfirmedResult.getWarnings()).isNotEmpty();
    }

    @Test
    @DisplayName("Unsupported foreign currencies (USD, EUR, GBP) are strictly rejected")
    void detectCurrency_unsupportedCurrenciesRejected() throws IOException {
        String csvUsdHeader = "date,campaign,Cost (USD)\n2026-03-01,Brand-1,500.00";
        CsvTable table1 = parser.parse(csvUsdHeader);
        CurrencyDetectionResult result1 = currencyService.detectCurrency(table1, "Cost (USD)", null);
        assertThat(result1.isSupported()).isFalse();
        assertThat(result1.getErrorMessage()).containsIgnoringCase("Unsupported currency detected: 'USD'");

        String csvDollarSign = "date,campaign,spend\n2026-03-01,Brand-1,$ 500.00";
        CsvTable table2 = parser.parse(csvDollarSign);
        CurrencyDetectionResult result2 = currencyService.detectCurrency(table2, "spend", null);
        assertThat(result2.isSupported()).isFalse();
        assertThat(result2.getErrorMessage()).containsIgnoringCase("Unsupported currency detected: 'USD'");
    }

    @Test
    @DisplayName("Currency ambiguity: Conflicting currencies within the same file are rejected")
    void detectCurrency_conflictingCurrenciesWithinFile() throws IOException {
        String csvMixed = """
                date,campaign,spend
                2026-03-01,Brand-1,₹ 1000.00
                2026-03-02,Brand-1,$ 200.00
                """;
        CsvTable table = parser.parse(csvMixed);
        CurrencyDetectionResult result = currencyService.detectCurrency(table, "spend", null);

        assertThat(result.isAmbiguous()).isTrue();
        assertThat(result.isSupported()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("Conflicting currencies detected");
    }

    @Test
    @DisplayName("parseMonetaryValue removes symbols and commas, and preserves null for missing values")
    void parseMonetaryValue_formatting() {
        assertThat(currencyService.parseMonetaryValue("₹ 1,501.55")).isEqualByComparingTo(new BigDecimal("1501.55"));
        assertThat(currencyService.parseMonetaryValue("Rs. 2,000")).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(currencyService.parseMonetaryValue("500.50")).isEqualByComparingTo(new BigDecimal("500.50"));

        // Missing values are null
        assertThat(currencyService.parseMonetaryValue("")).isNull();
        assertThat(currencyService.parseMonetaryValue("   ")).isNull();
        assertThat(currencyService.parseMonetaryValue("-")).isNull();
        assertThat(currencyService.parseMonetaryValue("N/A")).isNull();
    }
}
