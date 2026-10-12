package com.adsentry.importing.service.csv;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a parsed row from a CSV document with its 1-indexed source row number.
 */
public class CsvRow {

    private final int sourceRowNumber;
    private final List<String> rawValues;
    private final Map<String, String> valuesByHeader;

    public CsvRow(int sourceRowNumber, List<String> rawValues, Map<String, String> valuesByHeader) {
        this.sourceRowNumber = sourceRowNumber;
        this.rawValues = rawValues != null ? Collections.unmodifiableList(rawValues) : Collections.emptyList();
        this.valuesByHeader = valuesByHeader != null ? Collections.unmodifiableMap(valuesByHeader) : Collections.emptyMap();
    }

    public int getSourceRowNumber() {
        return sourceRowNumber;
    }

    public List<String> getRawValues() {
        return rawValues;
    }

    public String get(String header) {
        if (header == null) {
            return null;
        }
        return valuesByHeader.get(header.trim().toLowerCase());
    }

    public String getByIndex(int index) {
        if (index >= 0 && index < rawValues.size()) {
            return rawValues.get(index);
        }
        return null;
    }

    public boolean isEmptyRow() {
        return rawValues.isEmpty() || rawValues.stream().allMatch(s -> s == null || s.trim().isEmpty());
    }

    public int getColumnCount() {
        return rawValues.size();
    }
}
