package com.adsentry.importing.service.csv;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * In-memory representation of a parsed CSV file.
 */
public class CsvTable {

    private final List<String> headers;
    private final List<CsvRow> rows;
    private final List<String> warnings;
    private final Map<String, Integer> headerIndexMap;

    public CsvTable(List<String> headers,
                    List<CsvRow> rows,
                    List<String> warnings,
                    Map<String, Integer> headerIndexMap) {
        this.headers = headers != null ? Collections.unmodifiableList(headers) : Collections.emptyList();
        this.rows = rows != null ? Collections.unmodifiableList(rows) : Collections.emptyList();
        this.warnings = warnings != null ? Collections.unmodifiableList(warnings) : Collections.emptyList();
        this.headerIndexMap = headerIndexMap != null ? Collections.unmodifiableMap(headerIndexMap) : Collections.emptyMap();
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<CsvRow> getRows() {
        return rows;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean hasHeader(String header) {
        if (header == null) return false;
        return headerIndexMap.containsKey(header.trim().toLowerCase());
    }

    public Integer getHeaderIndex(String header) {
        if (header == null) return null;
        return headerIndexMap.get(header.trim().toLowerCase());
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getHeaderCount() {
        return headers.size();
    }
}
