# AdSentry — Phase 0 Inspection & Feasibility Report

**Date:** 2026-10-11  
**Project:** AdSentry (Ad Spend & Performance Guardian)  
**Status:** Phase 0 Complete — Ready for Phase 1 (Backend Foundation)

---

## 1. System Environment & Toolchain Inspection

| Tool / Runtime | Detected Version | Availability & Notes |
| :--- | :--- | :--- |
| **Java JDK** | OpenJDK 21.0.12.1 (Temurin-21 LTS) | Installed on PATH; ready for Spring Boot 3.x |
| **Java Compiler** | javac 21.0.12.1 | Verified |
| **Apache Maven** | 3.10.0-rc-1 | Installed on PATH; ready for dependency management and builds |
| **Node.js** | v24.18.0 | Verified |
| **npm / npx** | 11.16.0 | Available (`npm.cmd` / `npx.cmd` used in PowerShell environment) |
| **Git** | 2.55.0.windows.3 | Verified; repository initialized at scratch workspace |
| **PostgreSQL** | Cloud-managed for Render; no local service on PATH | Production will use Render PostgreSQL (`DATABASE_URL`). For local development and CI testing, we will configure dual profile support: PostgreSQL dialect for production, and an embedded/H2 PostgreSQL-compatibility test profile so local builds and unit/integration tests run reliably without requiring a local Postgres daemon. |

---

## 2. Repository & Workspace State

- **Active Workspace:** `C:\Users\Thiru\.gemini\antigravity-ide\scratch\adsentry`
- **Git Status:** Initialized empty repository on `master` branch.
- **Project Structure:**
  - `README.md` (AdSentry Master Specification copied from Downloads)
  - `sample-marketing.csv` (Inspected marketing dataset)
- **Previous Codebase:** No existing backend or frontend code existed in this location. Clean slate for modular monolith architecture.

---

## 3. CSV Dataset & Data Semantics Inspection

### 3.1 Discovered Dataset: `sample-marketing.csv`
- **Total Records:** 260 rows (+1 header row)
- **Observed Date Span:** `2026-01-01` to `2026-05-01` (4 months, daily granularity, ISO format `YYYY-MM-DD`)
- **Headers:** `date, channel, campaign, impressions, clicks, spend, conversions, revenue`
- **Platform Breakdown (`channel`):**
  - **Google Ads:** 34 records
  - **Meta Ads:** 26 records
  - **Other channels present:** Organic (41), Email (42), TikTok (41), LinkedIn (49), Affiliate (27)
- **Observed Metrics:**
  - `spend`: ₹34.87 to ₹14,614.29 (Average: ₹2,816.42)
  - `impressions`: 662 to 89,302
  - `clicks`: 11 to 4,352
  - `conversions`: 0 to 552
  - `revenue`: ₹0.00 to ₹59,150.84

### 3.2 Semantic Capabilities & Data Limitations
1. **Platform / Channel Filtering:**
   - Supported natively: The dataset contains both `Google Ads` and `Meta Ads` records alongside other channels.
   - The import pipeline will support filtering or isolating Google Ads and Meta Ads, while flagging unmapped or unsupported channels during preview.
2. **Campaign Identity:**
   - The dataset provides `campaign` names (e.g. `Brand-4`, `Spring-1`, `Launch-7`, `Retargeting-1`), but does **not** include platform-native Campaign IDs (e.g. numeric Google `campaign_id` or Meta `campaign_id`).
   - *Impact on Canonical Model:* As specified in Section 6 & 7 of the Master README, identity will fall back to `Normalized Campaign Name + Platform + Date` with an explicit "Lower-Confidence Identity (Name-based)" warning in preview and analytics.
3. **Zero-Conversion Detection (Rule A Validation):**
   - Verified present: E.g., `2026-02-12, Google Ads, Launch-7, spend: 34.87, conversions: 0`.
   - The dataset genuinely supports Rule A testing (High spend with zero conversions) once spend exceeds the configured minimum spend threshold.
4. **CPA Target Exceeded (Rule B Validation):**
   - Several campaigns exhibit high spend relative to low conversions (e.g., `2026-03-10, Google Ads, Retargeting-1, spend: ₹1,501.55, conversions: 9 => CPA = ₹166.84`).
   - *Conversion Definition Limitation:* The dataset uses a generic column `conversions` without specifying conversion action (Purchase vs Lead vs App Install). Per Section 5 of the specification, the import preview will label this conversion definition as `Unspecified / Standard`, and allow the user to confirm whether to evaluate it against the target CPA.
5. **Currency:**
   - Assumed INR (`₹`) in line with AdSentry MVP specifications. Any non-INR files encountered during runtime will be rejected during preview.

### 3.3 Synthetic & Fixture Datasets Planned for Phase 2
Because `GoogleAds_DataAnalytics_Sales_Uncleaned.csv` and `Facebook Ads.csv` mentioned in planning were not found in local directories, we will create dedicated test fixtures in `src/test/resources/fixtures/`:
1. `google_ads_export_sample.csv` (Native Google Ads Manager headers, INR currency, edge cases).
2. `meta_ads_export_sample.csv` (Native Meta Ads Manager headers, INR currency, edge cases).
3. `ambiguous_currency_invalid.csv` (Non-INR rejection test).
4. `duplicate_overlapping_records.csv` (Deduplication and conflict winner verification test).

---

## 4. Proposed Dribbble UI Adaptation

The user provided a reference screenshot featuring a sleek, dark-mode bento-grid dashboard. Here is our proposed adaptation for AdSentry:

```
+-------------------------------------------------------------------------------------------------------+
|  🛡️ AdSentry   [ Platform: All Ads ▾ ]  [ Date: Last 30 Days ▾ ]   [ ⚙️ Settings ]  [ 📤 Import CSV ]   |
+-------------------------------------------------------------------------------------------------------+
|                                                                                                       |
|  +---------------------------+  +--------------------------+  +-------------------------------------+ |
|  | TOTAL AT-RISK SPEND       |  | ACTIVE RECOMMENDATIONS   |  | TOP WASTE / HIGH-CPA ALERT          | |
|  | ₹18,420.50                |  | 5 Actionable Alerts      |  | 🚨 Google Ads — Launch-7            | |
|  | 4 Campaigns at zero conv  |  | 3 High • 2 Medium        |  | Spent ₹9,414 with 0 conversions     | |
|  | [ View Zero-Conv Items ↗ ] |  | [ Review Engine Rules ↗ ]|  | [ Take Action / Dismiss ↗ ]         | |
|  +---------------------------+  +--------------------------+  +-------------------------------------+ |
|                                                                                                       |
|  +---------------------------------------------------------+  +-------------------------------------+ |
|  | SPEND VS CONVERSION EFFICIENCY (LINE/AREA CHART)        |  | PLATFORM SHARE (BENTO BARS)         | |
|  | Modern wave chart with day-by-day spend and CPA trends  |  | 🟢 Google Ads: 58% (₹84k)           | |
|  | Tooltip pills displaying exact metrics                  |  | 🔵 Meta Ads:   42% (₹61k)           | |
|  +---------------------------------------------------------+  +-------------------------------------+ |
|                                                                                                       |
|  +--------------------------------------------------------------------------------------------------+ |
|  | ACTIONABLE RECOMMENDATIONS FEED (Cards with High/Medium/Low badges, Evidence & Status Toggle)    | |
|  +--------------------------------------------------------------------------------------------------+ |
|  | CAMPAIGN PERFORMANCE AUDIT TABLE (Sortable, INR Formatted, Clean Status Pills, Export CSV)      | |
|  +--------------------------------------------------------------------------------------------------+ |
+-------------------------------------------------------------------------------------------------------+
```

### Visual Characteristics Adapted from Reference:
1. **Palette:**
   - Primary Background: Near-black `#0E0F12` / `#131418`
   - Card Surfaces: Deep charcoal `#181920` with subtle border `rgba(255, 255, 255, 0.08)`
   - Standout Alert Card: Vibrant deep orange/red gradient `#FF451A` to `#E03710` for high-severity at-risk spend
   - Accents: Mint green `#10B981` (healthy / positive delta), electric blue `#3B82F6` (Meta Ads), amber `#F59E0B` (warning)
2. **Typography & Layout:**
   - Bento-box grid with `20px` border-radius and generous padding
   - High-impact metrics (large bold numbers, tabular figures for currency)
   - Micro-action buttons with rounded circular arrow icons (`↗`)

---

## 5. Next Step: Phase 1 — Backend Foundation

Upon review, we will proceed immediately to **Phase 1**:
1. Initialize Spring Boot 3 application with Java 21 and Maven (`adsentry-backend`).
2. Configure modules: `common`, `importing`, `normalization`, `campaign`, `analytics`, `recommendation`, `settings`, `export`, `demo`.
3. Set up database entities and Flyway migrations for `ImportBatch`, `ImportedRecord`, `AppSettings`, `Recommendation`, and `ImportRowError`.
4. Implement global exception handling, RFC-7807 problem responses, health check endpoint, and unit/integration test baseline.
