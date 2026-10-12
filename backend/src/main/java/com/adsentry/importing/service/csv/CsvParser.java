package com.adsentry.importing.service.csv;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Robust RFC-4180 compliant CSV parser with support for:
 * - Escaped double quotes ("")
 * - Commas and newlines embedded within quoted fields
 * - UTF-8 Byte Order Mark (BOM) stripping
 * - Source line number tracking (1-indexed)
 */
@Component
public class CsvParser {

    public CsvTable parse(InputStream inputStream) throws IOException {
        return parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    public CsvTable parse(String csvContent) throws IOException {
        return parse(new StringReader(csvContent != null ? csvContent : ""));
    }

    public CsvTable parse(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        List<String> warnings = new ArrayList<>();

        List<List<String>> allRawRows = new ArrayList<>();
        List<Integer> rowLineNumbers = new ArrayList<>();

        StringBuilder currentCell = new StringBuilder();
        List<String> currentRow = new ArrayList<>();
        boolean inQuotes = false;
        int currentLine = 1;
        int rowStartLine = 1;
        boolean isFirstChar = true;

        int ch;
        while ((ch = bufferedReader.read()) != -1) {
            // Strip BOM if at the beginning of stream
            if (isFirstChar) {
                isFirstChar = false;
                if (ch == '\uFEFF') {
                    continue;
                }
            }

            char c = (char) ch;

            if (inQuotes) {
                if (c == '"') {
                    // Peek next character for escaped quote ""
                    bufferedReader.mark(1);
                    int next = bufferedReader.read();
                    if (next == '"') {
                        currentCell.append('"');
                    } else {
                        inQuotes = false;
                        if (next != -1) {
                            bufferedReader.reset();
                        }
                    }
                } else {
                    currentCell.append(c);
                    if (c == '\n') {
                        currentLine++;
                    }
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    currentRow.add(currentCell.toString().trim());
                    currentCell.setLength(0);
                } else if (c == '\r') {
                    // Check if followed by \n
                    bufferedReader.mark(1);
                    int next = bufferedReader.read();
                    if (next != '\n' && next != -1) {
                        bufferedReader.reset();
                    }
                    currentRow.add(currentCell.toString().trim());
                    currentCell.setLength(0);
                    if (!isEmptyRowList(currentRow)) {
                        allRawRows.add(new ArrayList<>(currentRow));
                        rowLineNumbers.add(rowStartLine);
                    }
                    currentRow.clear();
                    currentLine++;
                    rowStartLine = currentLine;
                } else if (c == '\n') {
                    currentRow.add(currentCell.toString().trim());
                    currentCell.setLength(0);
                    if (!isEmptyRowList(currentRow)) {
                        allRawRows.add(new ArrayList<>(currentRow));
                        rowLineNumbers.add(rowStartLine);
                    }
                    currentRow.clear();
                    currentLine++;
                    rowStartLine = currentLine;
                } else {
                    currentCell.append(c);
                }
            }
        }

        // Process final cell / row
        if (currentCell.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(currentCell.toString().trim());
            if (!isEmptyRowList(currentRow)) {
                allRawRows.add(new ArrayList<>(currentRow));
                rowLineNumbers.add(rowStartLine);
            }
        }

        if (allRawRows.isEmpty()) {
            return new CsvTable(List.of(), List.of(), List.of("CSV file is empty or contains only whitespace."), Map.of());
        }

        // First row is the header row
        List<String> rawHeaders = allRawRows.get(0);
        List<String> cleanedHeaders = new ArrayList<>();
        Map<String, Integer> headerIndexMap = new HashMap<>();

        for (int i = 0; i < rawHeaders.size(); i++) {
            String rawH = rawHeaders.get(i);
            String h = rawH != null ? rawH.trim() : "";
            cleanedHeaders.add(h);
            String lower = h.toLowerCase();
            if (!lower.isBlank() && !headerIndexMap.containsKey(lower)) {
                headerIndexMap.put(lower, i);
            }
        }

        List<CsvRow> csvRows = new ArrayList<>();
        int expectedColumns = cleanedHeaders.size();

        for (int r = 1; r < allRawRows.size(); r++) {
            List<String> rawRow = allRawRows.get(r);
            int lineNum = rowLineNumbers.get(r);

            if (rawRow.size() != expectedColumns) {
                warnings.add("Row " + lineNum + " column count (" + rawRow.size() + ") differs from header count (" + expectedColumns + ").");
            }

            Map<String, String> rowMap = new HashMap<>();
            for (int col = 0; col < cleanedHeaders.size(); col++) {
                String headerKey = cleanedHeaders.get(col).toLowerCase();
                String cellVal = (col < rawRow.size()) ? rawRow.get(col) : "";
                rowMap.put(headerKey, cellVal);
            }

            csvRows.add(new CsvRow(lineNum, rawRow, rowMap));
        }

        return new CsvTable(cleanedHeaders, csvRows, warnings, headerIndexMap);
    }

    private boolean isEmptyRowList(List<String> row) {
        if (row.isEmpty()) return true;
        return row.stream().allMatch(s -> s == null || s.trim().isEmpty());
    }
}
