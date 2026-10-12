# AdSentry --- Master MVP README

**Purpose:** This is the implementation blueprint for Antigravity. Treat
it as the source of truth for the first AdSentry MVP. Build
incrementally, explain decisions in beginner-friendly language, test
each phase, and do not finalize the UI until the user approves the
Dribbble-reference adaptation.

## 1. Product overview

AdSentry helps small businesses understand advertising performance
across Google Ads and Meta Ads. Users import CSV reports; the
application validates and normalizes data, calculates campaign-level
metrics, compares compatible reporting periods, and produces
transparent, rule-based recommendations.

AdSentry identifies campaigns that may need attention. It must not claim
spend is definitely wasted and must never automatically change live
campaigns.

### MVP goals

-   Import Google Ads and Meta Ads CSV files.
-   Detect platform, map columns, preview validation and duplicate
    information, and ask for confirmation.
-   Import valid rows while reporting rejected row numbers and reasons.
-   Retain import history and support inspect, replace, and delete.
-   Normalize supported fields and calculate campaign-level metrics.
-   Start with two recommendation rules: (1) spend above a minimum
    threshold with zero known conversions; (2) CPA above target.
-   Compare equal-length adjacent reporting periods when coverage is
    sufficient.
-   Let users mark recommendations Reviewed or Dismissed.
-   Export campaign metrics and recommendations to CSV.
-   Provide isolated demo mode.
-   Deploy the frontend and backend.

### Out of scope

No live Google/Meta APIs, automatic campaign edits, billing, paid AI
APIs, microservices, Kafka, Redis, PDF exports, multi-user
permissions/login, or ad-level analysis as a primary workflow.
Additional rules such as CTR, frequency, deterioration, and positive
insights can be future work.

## 2. Confirmed stack

-   Frontend: React + Vite, hosted on Vercel.
-   Backend: Java + Spring Boot modular monolith, hosted on Render.
-   Database: PostgreSQL on Render.
-   Use REST APIs between frontend and backend.
-   Backend owns metric calculations and business rules; do not
    duplicate analysis logic in React.
-   Use database migrations (Flyway is suitable).
-   Keep secrets in environment variables, never in source control.

Suggested backend modules: `importing`, `normalization`, `campaign`,
`analytics`, `recommendation`, `settings`, `export`, `demo`, and
`common`. Keep controller, service, repository, DTO, entity, and mapping
responsibilities separate. Inspect the existing repository before
choosing exact packages or replacing files.

## 3. UI direction --- not finalized

The user has shared Dribbble dashboard screenshots as references. The
broad direction is a dark, card-based analytics experience: near-black
canvas, rounded cards, subtle borders, generous spacing, large metrics,
and blue/orange-red/green accents. Desktop-first but responsive.

Intended information architecture includes a combined platform overview,
dedicated Google Ads and Meta Ads views, import workflow/history,
recommendations, and threshold settings. These are not final screen
designs.

**Do not finalize navigation, layout, chart types, exact colors,
spacing, or detailed screen structure without user approval.** Inspect
the available reference screenshots, propose an adaptation for AdSentry,
and get approval before locking in the design. Do not substitute a
generic dashboard without review.

## 4. Import workflow

1.  User selects a CSV.
2.  Detect Google Ads or Meta Ads; show the detected platform and ask
    the user to confirm. If uncertain, request selection.
3.  Automatically map recognized columns.
4.  If a required field cannot be mapped, ask for manual mapping. Never
    silently treat budget as actual spend.
5.  Validate reporting dates, numbers, currency, identifiers, conversion
    definitions, and dimensions.
6.  Show a preview before import: platform, mapped columns, observed
    date range, accepted/rejected row estimates, duplicates/conflicts,
    currency, and coverage warnings.
7.  User confirms.
8.  Import valid rows and reject invalid rows. Show rejected source row
    numbers and reasons.
9.  Apply deduplication/version rules and retain source-batch
    provenance.
10. Recalculate affected analytics and recommendations.
11. Show an import summary: accepted, rejected, duplicate, updated
    records, date coverage, warnings, and errors.

### Partial imports and validation

-   Import valid rows even if other rows are invalid.
-   Require a valid reporting date on every imported record.
-   Store row-level errors with batch ID, source row number, field if
    known, error code, and human-readable reason.
-   Missing values are not zero. If a metric needed by a rule is
    unavailable, skip that rule and explain why.
-   Reject or flag rows whose identity cannot be established safely; do
    not speculatively merge them.
-   Support common date formats only when unambiguous. If a date such as
    `03/04/2026` is ambiguous, use a known source convention or ask the
    user---never silently guess.
-   INR only in the MVP. Reject/exclude unsupported currencies; never
    convert or relabel them as INR.
-   Preserve record dates; do not invent daily records from
    aggregate-only reports.
-   Partial coverage does not automatically block analysis, but must be
    disclosed and must not produce misleading comparisons.

## 5. CSV datasets and semantics

Public/Kaggle-style datasets found during planning are useful for
testing but are not verified native Google Ads or Meta Ads Manager
exports. Inspect the actual files and headers before implementing
mappings.

Known examples: - `GoogleAds_DataAnalytics_Sales_Uncleaned.csv`: about
2,600 rows; fields include `Ad_ID`, `Campaign_Name`, `Clicks`,
`Impressions`, `Cost`, `Leads`, `Conversions`, `Conversion Rate`,
`Sale_Amount`, `Ad_Date`, `Location`, `Device`, `Keyword`. Missing
values and inconsistent formats exist. Do not assume `Leads`,
`Conversions`, and `Sale_Amount` mean the same thing. -
`Facebook Ads.csv`: about 316 rows; fields include `Day`, `Impressions`,
`CPM (Cost per 1,000 Impressions)`, `Link Clicks`,
`CTR (Link Click-Through Rate)`, `CPC (Cost per Link Click)`,
`Amount Spent`, `Messaging Conversations Started`, and
`Checkouts Initiated`. Many fields are missing, and there is no campaign
ID/name in the described schema. Do not invent campaign-level
identity. - An event-based social ads dataset included `ad_events.csv`,
`ads.csv`, `campaigns.csv`, and `users.csv`. Event records are not
campaign summaries, and campaign budgets are not actual spend. Do not
calculate CPA or wasted spend from budget alone.

Preserve conversion semantics. A field called "Conversions" must not
automatically be assumed to mean purchase, lead, checkout, or messaging
conversation. Store conversion type/definition when available. If
unknown or incompatible, skip CPA comparisons and explain the
limitation.

## 6. Canonical data model

Inspect the repository and sample data first; refine physical schema as
needed while preserving these concepts.

### ImportBatch

`id`, platform, original filename, import timestamp, reporting start/end
when known, status, currency, row counts
(total/accepted/rejected/duplicate/updated), column mapping, validation
summary.

### ImportedRecord / source record version

Stable ID, source batch ID, platform, campaign ID when available,
original and normalized campaign name, reporting date, supported
dimensions (device/location/placement/keyword when present), spend,
impressions, clicks, conversion count and definition/type when known,
other metrics only when semantics are preserved, source row number, and
provenance/version metadata. Use nullable metrics for unknown values.

### Campaign identity

Prefer platform campaign IDs. If absent, use normalized campaign name as
a lower-confidence fallback, alongside platform/date/dimensions. Warn
that names may be ambiguous. Do not merge solely on platform + date.

### AppSettings

Saved global CPA target, minimum spend threshold, any configured
priority cutoffs, and updated timestamp. Defaults must be clearly
labelled examples, not industry benchmarks.

### Analysis metadata

Store latest analysis period, run timestamp, thresholds used (including
per-analysis overrides), coverage warnings, and applicable conversion
definitions. Full historical snapshots of every analysis run are not
required.

### Recommendation

ID, stable rule identifier, campaign reference/identity reliability,
period, priority (`HIGH`, `MEDIUM`, `LOW`), title/explanation,
evidence/supporting metrics, trigger condition, thresholds used,
suggested action, status (`NEW`, `REVIEWED`, `DISMISSED`), timestamps,
and stable fingerprint.

### ImportRowError

Batch ID, source row number, field if known, error code, and readable
message.

Use appropriate foreign keys, indexes, constraints, and migrations.

## 7. Identity, deduplication, replacement, deletion

### Record identity

Use the most specific available identity: - Platform. - Campaign ID when
available; otherwise normalized campaign name with lower confidence. -
Reporting date. - Meaningful dimensions needed to distinguish rows
(device/location/placement/etc. where the source splits metrics). -
Conversion definition/type or another meaningful dimension when
necessary.

Do not deduplicate by date alone. Do not merge rows with different
dimensions. If identity is ambiguous, warn or reject rather than
silently merge.

### Overlapping imports

-   For each matching record identity, the newest eligible import
    version wins, using import timestamp.
-   Keep source batch provenance for the winning version.
-   Never sum duplicate versions of the same record.
-   Select effective record versions first, then aggregate metrics.
-   When values conflict, use the newer import's values and record the
    source batch. This is the selected MVP policy; newer does not
    necessarily mean more accurate.
-   Report duplicates/updated records in the import summary.

### Replacement

Replace only the selected batch. Revalidate and process the replacement,
then recalculate affected metrics/comparisons/recommendations.
Replacement must be atomic: if it fails, do not leave a half-replaced
dataset.

### Deletion

Ask for confirmation. Delete the selected batch and its direct source
records, then recalculate affected analytics and recommendations. If the
deleted batch supplied the winning version for an identity, restore the
newest remaining eligible version; if none remains, remove that record
from effective analysis.

**Critical:** A single-row overwrite design is insufficient if deletion
must restore older versions. Retain per-batch source versions and
compute the effective winner at query/materialization time, or implement
another demonstrably correct approach. Test restoration explicitly.

## 8. Metric calculations

Aggregate effective records over the selected period first, then
calculate metrics centrally in the backend. Use decimal-safe types for
currency.

-   Spend = sum of eligible spend.
-   Impressions = sum of known impressions.
-   Clicks = sum of known clicks.
-   Conversions = sum only for compatible, known conversion definitions.
-   CTR = total clicks / total impressions × 100, if impressions are
    known and \> 0.
-   CPC = total spend / total clicks, if clicks are known and \> 0.
-   CPA = total spend / total compatible conversions, if conversions are
    known and \> 0.
-   Conversion rate = total compatible conversions / total clicks × 100,
    if clicks are known and \> 0.
-   CPM = total spend / total impressions × 1,000 when supported.

Do not use averages of row-level rates as substitutes for aggregate
metrics. Source-reported rates may be retained separately and labelled.

Semantics: - Missing/null means unknown/unavailable; zero means
explicitly reported zero. - CPA is undefined when conversions are zero,
not ₹0. - Rates are undefined if denominators are zero or unknown. - If
a rule's required field is unavailable, skip that rule and explain
why. - Never combine leads and purchases or compare their CPAs as if
equivalent. - Do not compare different currencies.

## 9. Analysis periods and coverage

-   Default to the latest available reporting period, with optional
    custom date selection.
-   Compare equal-length adjacent periods (e.g., selected 7 days versus
    the preceding 7 days).
-   Show actual observed coverage and warn about gaps or partial
    periods.
-   Do not interpret absent dates as zero activity.
-   Suppress or qualify comparisons if either period is materially
    incomplete or incompatible.
-   Do not create synthetic daily records from aggregate reports.

## 10. Recommendation engine

Implement a deterministic, extensible rule engine. Each rule should
declare required fields, conditions, priority criteria, explanation, and
suggested action.

### Rule A --- High spend with zero known conversions

Trigger only when: 1. Spend is known and valid for the selected period.
2. Conversion count is known and compatible. 3. Aggregate conversions
equal zero. 4. Aggregate spend is strictly greater than the configured
minimum-spend threshold. 5. Coverage is adequate to interpret the
period; otherwise qualify or skip the result if misleading.

Show spend, period, zero known conversions, threshold used, explanation,
and suggested investigation (e.g., check tracking, targeting, landing
page, or campaign objective). Describe the spend as **potentially
at-risk**, never confirmed wasted spend.

### Rule B --- CPA above target

Trigger only when spend and compatible conversions are known,
conversions \> 0, the conversion definition is compatible with the
target, currency is INR, and aggregate CPA exceeds the target. If
conversion definitions are ambiguous/incompatible, skip the rule and
explain why. If coverage makes the result unreliable, qualify or skip
it.

Show actual CPA, target, amount/percentage over target when calculable,
conversion definition, and period.

### Priorities

Use documented, rule-specific High/Medium/Low criteria with fixed
cutoffs: - Zero-conversion findings: priority based on spend bands
relative to the configured minimum or explicit spend cutoffs. - CPA
findings: priority based on fixed percentage bands above the target. -
Values are MVP configuration choices, not industry benchmarks. Document
and test them. - Priority is separate from Reviewed/Dismissed status.

### Recommendation lifecycle

-   Re-running analysis must not duplicate unchanged recommendations.
-   Use stable fingerprints based on rule, campaign identity, and
    relevant period/scope.
-   Preserve Reviewed/Dismissed when the underlying issue remains
    materially unchanged.
-   Reset to New when the issue materially changes.
-   Remove/mark obsolete findings when their rule no longer triggers;
    never leave stale findings presented as current.
-   Recalculate after imports, replacement, deletion, or relevant
    setting changes.

## 11. Threshold settings

-   Provide editable example defaults for minimum spend and global CPA
    target; label them as examples, not benchmarks.
-   Allow editing saved defaults in Settings.
-   Allow per-analysis overrides without silently changing saved
    defaults.
-   Record the values used for the latest analysis.
-   Validate thresholds as non-negative and INR-based.
-   Apply CPA targets only to compatible conversion definitions.
-   If a required threshold is missing/invalid, skip that rule and
    explain why rather than silently inventing a value.

## 12. Demo mode

Provide a separate, clearly labelled demo dataset and a way to
enter/exit demo mode. Demo records must never contaminate imported
records, import history, real campaign metrics, or real recommendations.
Use a dedicated demo service/API or equally isolated mechanism; do not
hardcode fake production metrics into React components.

## 13. Recommendation statuses

Users can set a recommendation to `REVIEWED` or `DISMISSED`; `NEW` is
the initial state. Status changes persist but do not alter calculated
metrics. Preserve status when the issue is materially unchanged; reset
it when the underlying finding materially changes. A dismissed item must
not hide a materially new issue.

## 14. CSV export

Support CSV exports for campaign metrics and recommendations, respecting
selected date/platform filters. Include clear headers, period, currency,
recommendation rule/priority/status/explanation/evidence where relevant.
Export unknown metrics as blank, not zero. Escape commas, quotes, and
line breaks, and mitigate spreadsheet formula injection for cells
starting with dangerous formula characters. PDF is out of scope.

## 15. Proposed REST API

Inspect existing code first. If no existing contract exists, use routes
similar to these and keep frontend/backend consistent.

### Imports

-   `POST /api/imports/preview` --- parse and preview without committing
    records; return detected platform/confidence, mappings, dates,
    validation, duplicate/conflict counts, and coverage.
-   `POST /api/imports` --- confirm/process import; return batch summary
    and row errors.
-   `GET /api/imports` --- list batches.
-   `GET /api/imports/{batchId}` --- inspect batch and summary.
-   `PUT /api/imports/{batchId}/replace` --- replace selected batch
    atomically.
-   `DELETE /api/imports/{batchId}` --- delete batch and recalculate.
-   `GET /api/imports/{batchId}/errors` --- row errors if not included
    in batch detail.

### Analytics

-   `GET /api/analytics/overview?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD&platform=...`
-   `GET /api/analytics/campaigns?startDate=...&endDate=...&platform=...`
-   `GET /api/analytics/comparison?startDate=...&endDate=...&platform=...`
-   `GET /api/analytics/coverage?startDate=...&endDate=...&platform=...`

### Recommendations and analysis

-   `GET /api/recommendations?startDate=...&endDate=...&platform=...&status=...&priority=...`
-   `PATCH /api/recommendations/{id}/status` --- update to Reviewed or
    Dismissed.
-   `POST /api/analysis/run` --- optional dates and threshold overrides;
    refresh latest results and return thresholds/coverage/warning
    summary.

Run analysis automatically after successful import and provide a manual
rerun action.

### Settings, export, demo

-   `GET /api/settings`
-   `PUT /api/settings`
-   `GET /api/exports/campaigns.csv?startDate=...&endDate=...&platform=...`
-   `GET /api/exports/recommendations.csv?startDate=...&endDate=...&platform=...`
-   `GET /api/demo/overview`
-   `GET /api/demo/campaigns`
-   `GET /api/demo/recommendations`

API requirements: - Use DTOs and request validation. - Use consistent
error response shapes with stable error codes and readable messages. -
Use appropriate HTTP status codes; validate date ranges. - Add
pagination where needed. - Do not expose stack traces, SQL details,
credentials, or internals. - Keep uploaded files temporary or safely
staged; enforce file-size and content validation.

## 16. Security and deployment

The initial app intentionally has one shared workspace and no login.
Display a clear warning that the workspace is shared and non-private. Do
not describe it as secure multi-user storage, and do not recommend
uploading sensitive business data. Before public use with real customer
data, add authentication, authorization, workspace isolation, rate
limiting, and a security review.

-   Configure CORS for the required Vercel origin(s).
-   Use HTTPS via hosting providers.
-   Use environment variables for database credentials and API base URL.
-   Suggested backend variables: `DATABASE_URL`, `DATABASE_USERNAME`,
    `DATABASE_PASSWORD`, `CORS_ALLOWED_ORIGINS`, and optional
    default-threshold variables.
-   Suggested frontend variable: `VITE_API_BASE_URL`.
-   Verify actual Render connection strings; do not guess them.
-   Never commit `.env` files or secrets.
-   Add health checks, structured logs, and avoid logging raw CSV
    contents.
-   Use migrations and transaction boundaries.
-   Validate file size, encoding, extension, content, and parsed
    structure.

## 17. Testing requirements

### Unit tests

Metric formulas and null/zero behavior; conversion compatibility; date
comparison and coverage; column mapping; identity normalization;
deduplication/winner selection; restoration after deletion; replacement
atomicity; both recommendation rules; priority cutoffs; recommendation
idempotency; status preservation/reset; CSV escaping/formula-injection
mitigation.

### Integration tests

Preview does not persist records; confirmed import and partial-import
summary; overlaps do not double-count; newest conflicting record wins;
deleting newest restores older version; replace/delete recalculate;
missing conversions do not trigger zero-conversion rule; incompatible
conversions skip CPA rule; demo data stays isolated; exports respect
filters.

### Frontend checks

Loading, empty, error and partial-success states; preview/confirmation;
manual mapping; coverage/identity warnings; import
history/inspect/replace/delete confirmation; thresholds and overrides;
recommendation statuses; CSV download; shared-workspace warning;
responsive behavior.

Never claim a test/build/deployment passed unless it was actually run
and passed.

## 18. Antigravity implementation phases

### Phase 0 --- Inspect

Inspect repository structure, Git state, dependencies, existing code,
and actual CSV files/headers/semantics. Report what exists and what the
data can genuinely support. Do not overwrite/delete existing work
without explanation.

### Phase 1 --- Backend foundation

Set up Spring Boot, PostgreSQL, migrations, configuration, validation,
API error handling, health endpoint, entities/repositories, and test
setup.

### Phase 2 --- Import pipeline (COMPLETED)

Implemented safe RFC-4180 CSV parsing, automatic/manual column mapping, budget protection, platform detection, unambiguous date parsing, INR currency enforcement with plain-number confirmation, preview without database writes, confirmed partial import with row-level error reporting, stable campaign identity separation from record deduplication keys, atomic replacement, and newest-version winner selection with automatic restoration upon batch deletion.

To run the reproducible PostgreSQL-backed integration test when a real PostgreSQL instance is available:
```powershell
mvn test -Dtest=PostgreSqlIntegrationTest -DPOSTGRES_TEST_URL="jdbc:postgresql://<host>:5432/<database>" -DPOSTGRES_TEST_USER="<user>" -DPOSTGRES_TEST_PASSWORD="<password>"
```
*(Note: Docker is not installed locally on this host environment, so automated Testcontainers execution is bypassed in favor of direct H2 PostgreSQL-compatibility mode for local testing and configurable `POSTGRES_TEST_URL` for CI/staging).*

### Phase 3 --- Analytics

Implement effective-record selection, metrics, latest-period selection,
custom dates, coverage, and adjacent-period comparisons. Add calculation
tests.

### Phase 4 --- Recommendations

Implement two rules, documented priorities, thresholds/overrides, stable
fingerprints, status lifecycle, and recalculation triggers. Test
duplicates and stale findings.

### Phase 5 --- UI and approval

Inspect Dribbble screenshots available in context. Propose screen
structure/adaptation for review before finalizing detailed visuals.
Implement approved design with React + Vite, real API calls, import
workflow, dashboard/platform views, history, recommendations, settings,
demo, exports, and all loading/error/empty/partial-data states.

### Phase 6 --- Integration and deployment

Run backend tests and frontend build; fix integration/CORS issues;
deploy PostgreSQL and backend on Render and frontend on Vercel;
configure environment variables and health checks; test the deployed
end-to-end flow with sample CSVs; document exactly what works and what
remains.

## 19. Working rules

-   Work one phase at a time; keep the project runnable after each phase
    where practical.
-   Explain important decisions in beginner-friendly language.
-   Summarize changed files, behavior, tests run/results, and next step
    after each phase.
-   Ask before major architecture changes, scope additions, destructive
    operations, or finalizing UI decisions.
-   Do not add live APIs, automatic campaign changes, paid AI APIs,
    billing, microservices, Kafka, or Redis.
-   Do not fabricate metrics, platform support, test results, or
    deployment success.
-   Keep calculations in the backend.
-   When a detail is unspecified, choose the simplest safe
    implementation consistent with this specification and document the
    assumption instead of repeatedly asking the user to decide small
    details.
-   Keep this README aligned with actual implementation.

## 20. Acceptance checklist

### Import/data

-   [x] Platform detection/confirmation works for supported formats.
-   [x] Automatic mapping and manual correction work.
-   [x] Preview shows mappings, dates, duplicates/conflicts, validation
    and coverage.
-   [x] Partial imports show row-level errors.
-   [x] Missing and zero remain distinct; INR-only behavior is enforced.
-   [x] Every imported record has a valid date.
-   [x] Import history can be inspected, replaced, and deleted.
-   [x] Overlapping records do not double-count.
-   [x] Deleting the winning batch restores the newest remaining
    version.

### Analytics/recommendations

-   [ ] Aggregate formulas are correct and tested.
-   [ ] Latest period and custom date range work.
-   [ ] Adjacent equal-length comparisons respect coverage.
-   [ ] Zero-conversion rule requires known zero conversions and spend
    above threshold.
-   [ ] CPA rule skips incompatible/ambiguous conversion definitions.
-   [ ] Priorities use documented cutoffs.
-   [ ] Recommendations explain evidence and next actions.
-   [ ] Recalculation prevents duplicate/stale recommendations.
-   [ ] Status persists when findings remain materially unchanged.

### Product/deployment

-   [ ] Demo mode is isolated.
-   [ ] CSV exports work and mitigate formula injection.
-   [ ] Shared/non-private warning is visible.
-   [ ] UI follows user-approved references and is responsive.
-   [ ] Backend tests pass and frontend builds successfully.
-   [ ] Render/Vercel deployment uses environment-based configuration.
-   [ ] No secrets are committed.
-   [ ] README matches actual implementation.

## Final instruction to Antigravity

**Start at Phase 0.** Inspect the repository and available CSV files,
report findings, and proceed in small, testable phases. Treat this
document as the agreed MVP blueprint. Do not finalize the UI until the
user has reviewed and approved the proposed adaptation of the Dribbble
references.
