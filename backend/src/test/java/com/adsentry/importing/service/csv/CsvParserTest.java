package com.adsentry.importing.service.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CsvParserTest {

    private final CsvParser parser = new CsvParser();

    @Test
    @DisplayName("Parse standard RFC-4180 CSV with quotes, commas, and escaped quotes")
    void parse_standardRfc4180Csv() throws IOException {
        String csv = """
                date,channel,campaign,spend
                2026-03-01,Google Ads,"Brand, Premium",1500.00
                2026-03-02,Google Ads,"Special ""Spring"" Offer",2200.50
                """;

        CsvTable table = parser.parse(csv);

        assertThat(table.getHeaders()).containsExactly("date", "channel", "campaign", "spend");
        assertThat(table.getRowCount()).isEqualTo(2);

        CsvRow row1 = table.getRows().get(0);
        assertThat(row1.getSourceRowNumber()).isEqualTo(2);
        assertThat(row1.get("campaign")).isEqualTo("Brand, Premium");
        assertThat(row1.get("spend")).isEqualTo("1500.00");

        CsvRow row2 = table.getRows().get(1);
        assertThat(row2.getSourceRowNumber()).isEqualTo(3);
        assertThat(row2.get("campaign")).isEqualTo("Special \"Spring\" Offer");
        assertThat(row2.get("spend")).isEqualTo("2200.50");
    }

    @Test
    @DisplayName("Parse CSV containing UTF-8 Byte Order Mark (BOM)")
    void parse_withUtf8Bom() throws IOException {
        String csv = "\uFEFFdate,spend\n2026-03-01,500.00";
        CsvTable table = parser.parse(csv);

        assertThat(table.getHeaders().get(0)).isEqualTo("date");
        assertThat(table.hasHeader("date")).isTrue();
        assertThat(table.getRowCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Empty or whitespace CSV returns empty table")
    void parse_emptyCsv() throws IOException {
        CsvTable table = parser.parse("   \n\n  ");
        assertThat(table.getRowCount()).isEqualTo(0);
        assertThat(table.getWarnings()).isNotEmpty();
    }
}
