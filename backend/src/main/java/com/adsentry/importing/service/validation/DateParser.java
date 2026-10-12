package com.adsentry.importing.service.validation;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates reporting dates and enforces the rule:
 * "Support common date formats only when unambiguous. If a date such as 03/04/2026 is ambiguous,
 * use a known source convention or ask the user—never silently guess."
 */
@Component
public class DateParser {

    private static final Pattern ISO_PATTERN = Pattern.compile("^(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})$");
    private static final Pattern SLASH_OR_DOT_PATTERN = Pattern.compile("^(\\d{1,2})[/.\\-](\\d{1,2})[/.\\-](\\d{4})$");

    public DateParseResult parseDate(String raw, String explicitFormat) {
        if (raw == null || raw.isBlank()) {
            return DateParseResult.invalid("", "Reporting date is missing or empty.");
        }
        String clean = raw.trim();

        // If explicit format provided by user/configuration
        if (explicitFormat != null && !explicitFormat.isBlank()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(explicitFormat.trim());
                LocalDate parsed = LocalDate.parse(clean, formatter);
                return DateParseResult.success(parsed);
            } catch (DateTimeParseException | IllegalArgumentException e) {
                return DateParseResult.invalid(clean, "Does not match specified date format '" + explicitFormat + "'");
            }
        }

        // Check ISO format YYYY-MM-DD or YYYY/MM/DD (unambiguous)
        Matcher isoMatcher = ISO_PATTERN.matcher(clean);
        if (isoMatcher.matches()) {
            try {
                int year = Integer.parseInt(isoMatcher.group(1));
                int month = Integer.parseInt(isoMatcher.group(2));
                int day = Integer.parseInt(isoMatcher.group(3));
                return DateParseResult.success(LocalDate.of(year, month, day));
            } catch (Exception e) {
                return DateParseResult.invalid(clean, e.getMessage());
            }
        }

        // Check D/M/YYYY or M/D/YYYY
        Matcher slashMatcher = SLASH_OR_DOT_PATTERN.matcher(clean);
        if (slashMatcher.matches()) {
            int part1 = Integer.parseInt(slashMatcher.group(1));
            int part2 = Integer.parseInt(slashMatcher.group(2));
            int year = Integer.parseInt(slashMatcher.group(3));

            // If both part1 and part2 are <= 12, the date is ambiguous!
            if (part1 <= 12 && part2 <= 12 && part1 != part2) {
                return DateParseResult.ambiguous(clean);
            }

            // If part1 > 12, part1 MUST be day, part2 MUST be month
            if (part1 > 12 && part2 <= 12) {
                try {
                    return DateParseResult.success(LocalDate.of(year, part2, part1));
                } catch (Exception e) {
                    return DateParseResult.invalid(clean, e.getMessage());
                }
            }

            // If part2 > 12, part1 MUST be month, part2 MUST be day
            if (part2 > 12 && part1 <= 12) {
                try {
                    return DateParseResult.success(LocalDate.of(year, part1, part2));
                } catch (Exception e) {
                    return DateParseResult.invalid(clean, e.getMessage());
                }
            }

            // If part1 == part2 <= 12, e.g. 05/05/2026, it's May 5th either way!
            if (part1 == part2 && part1 <= 12) {
                try {
                    return DateParseResult.success(LocalDate.of(year, part1, part2));
                } catch (Exception e) {
                    return DateParseResult.invalid(clean, e.getMessage());
                }
            }
        }

        return DateParseResult.invalid(clean, "Unrecognized date format. Supported: YYYY-MM-DD or explicit date format.");
    }
}
