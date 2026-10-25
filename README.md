# AdSentry

<p align="center">
  <img src="docs/adsentry-banner.png" alt="AdSentry — Advertising Campaign Analytics and Spend Optimization" width="100%">
</p>

<p align="center">
  <strong>Advertising Campaign Analytics &amp; Spend Optimization</strong>
</p>

<p align="center">
  A full-stack analytics platform that turns advertising campaign CSV data into actionable performance insights and rule-based recommendations.
</p>

<p align="center">
  <a href="https://www.oracle.com/java/technologies/downloads/">
    <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white" alt="Java 21">
  </a>
  <a href="https://spring.io/projects/spring-boot">
    <img src="https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 3.3.4">
  </a>
  <a href="https://react.dev/">
    <img src="https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black" alt="React 19">
  </a>
  <a href="https://www.postgresql.org/">
    <img src="https://img.shields.io/badge/PostgreSQL-Database-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL">
  </a>
  <a href="https://maven.apache.org/">
    <img src="https://img.shields.io/badge/Maven-Build-C71A36?logo=apachemaven&logoColor=white" alt="Maven">
  </a>
  <a href="https://vite.dev/">
    <img src="https://img.shields.io/badge/Vite-Frontend-646CFF?logo=vite&logoColor=white" alt="Vite">
  </a>
</p>

---

## Overview

AdSentry helps businesses analyze advertising campaign performance, identify potentially wasted spend, and monitor acquisition costs through a unified analytics dashboard.

Upload campaign reports, calculate key performance metrics, and receive actionable recommendations using a deterministic rule-based engine.

---

## Features

- **CSV Ingestion** — Import, validate, and normalize campaign reports across Google Ads and Meta Ads.
- **Campaign Analytics** — Analyze advertising spend, conversions, and cost per acquisition (CPA).
- **Rule-Based Recommendations** — Flag high-spend campaigns with zero known conversions and campaigns exceeding their CPA target.
- **Configurable Thresholds** — Adapt recommendation rules to campaign goals.
- **Recommendation Management** — Persist recommendations and recalculate them when underlying analysis changes.
- **CSV Export** — Export sanitized reports for further analysis.

---

## Rule-Based Recommendation Engine

AdSentry features a deterministic, transparent recommendation engine that surfaces optimization opportunities without making automated changes to live campaigns.

| Rule | Trigger Condition | Rationale & Evidence | Suggested Action | Triage Lifecycle |
| :--- | :--- | :--- | :--- | :--- |
| **Rule A: High Spend, Zero Conversions** | Aggregate Spend > configured threshold (e.g., ₹500) & Conversions == 0 | Spend is accumulating with no measurable return. Described transparently as *potentially at-risk spend*. | Inspect conversion tracking tags, verify landing page status, and check search query intent. | `NEW` ➔ `REVIEWED` ➔ `DISMISSED` |
| **Rule B: CPA Target Exceeded** | Conversions > 0 & Aggregate CPA > configured CPA target (e.g., ₹50) | Acquisition cost exceeds acceptable business targets for compatible conversion definitions. | Audit bid strategy, review search term negative keyword lists, and refine demographic targeting. | Preserved across recalculations if finding is unchanged |

### Lifecycle & Idempotency Safeguards
- **Stable Fingerprinting**: Recommendations use deterministic fingerprints (`ruleIdentifier` + `campaignIdentityKey` + `reportingPeriod`) to prevent duplicate findings across repeated runs.
- **Status Preservation**: Findings marked as `REVIEWED` or `DISMISSED` retain their state across re-imports unless underlying metrics materially change.
- **Conversion Compatibility**: Rules strictly validate conversion definitions (e.g., purchases vs. leads) to prevent flawed cross-action comparisons.

---

## Key Engineering Highlights

- **Decimal-Safe & Null-Safe Math**: All currency figures and ratios use decimal-safe calculations. CPA is strictly `null` (never a false ₹0) when conversions are zero or undefined.
- **Non-Destructive Batch Provenance**: Deleting an import batch restores the previous newest eligible version of any affected record rather than creating data gaps.
- **RFC-4180 Ingestion with Formula Guard**: Safe parser automatically sanitizes cell values against spreadsheet formula injection (`=`, `+`, `-`, `@`) and enforces unambiguous date parsing.
- **Period-over-Period Coverage Health**: Adjacent period comparisons evaluate calendar continuity and flag coverage gaps or partial datasets before presenting trend deltas.
- **Dual-Database Architecture**: Zero-config in-memory H2 database for rapid local development; cloud-ready PostgreSQL deployment powered by an automated `DATABASE_URL` credential processor.

---

## Tech Stack

| Component | Technology |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.3.4 |
| **Frontend** | React 19, Vite |
| **API** | REST |
| **Persistence** | Spring Data JPA, Hibernate |
| **Production database** | PostgreSQL |
| **Local database** | H2 |
| **Schema migrations** | Flyway |
| **Testing** | JUnit, Spring Boot Test, H2 |
| **Build tool** | Maven |

---

## Architecture

```text
React + Vite
     |
     | REST API
     v
Spring Boot Backend
     |
     +-- CSV Ingestion & Validation
     +-- Campaign Data Management
     +-- Analytics Engine
     +-- Recommendation Engine
     +-- Settings & CSV Export
     |
     v
PostgreSQL / Local H2
     |
     +-- Flyway Schema Migrations
```

---

## REST API Reference

### Imports
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/imports/preview` | Safe dry-run parse, column mapping, date validation, and duplicate preview |
| `POST` | `/api/imports` | Commit valid records with row-level error reporting |
| `GET` | `/api/imports` | List batch import history |
| `GET` | `/api/imports/{batchId}` | Inspect batch details and rejection summaries |
| `PUT` | `/api/imports/{batchId}/replace` | Atomically replace an existing batch |
| `DELETE` | `/api/imports/{batchId}` | Delete batch and restore previous record versions |

### Analytics
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/analytics/overview` | Overall spend, conversions, CPA, platform breakdown, and coverage warnings |
| `GET` | `/api/analytics/campaigns` | Aggregated per-campaign metrics and confidence scores |
| `GET` | `/api/analytics/comparison` | Equal-length adjacent period comparisons and percentage deltas |
| `GET` | `/api/analytics/coverage` | Total calendar vs. active day coverage breakdown and gap warnings |

### Recommendations & Settings
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/recommendations` | Filterable recommendations feed by status, priority, and platform |
| `PATCH` | `/api/recommendations/{id}/status` | Update triage state (`NEW`, `REVIEWED`, `DISMISSED`) |
| `POST` | `/api/analysis/run` | On-demand rule engine execution with optional threshold overrides |
| `GET` | `/api/settings` | Retrieve current default thresholds |
| `PUT` | `/api/settings` | Update saved default thresholds |

### Exports & Demo
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/exports/campaigns.csv` | Export sanitized campaign metrics (formula-injection protected) |
| `GET` | `/api/exports/recommendations.csv` | Export sanitized recommendations feed |
| `GET` | `/api/demo/overview` | Isolated sandbox demo metrics without touching database records |

---

## Run Locally

**Prerequisites:** JDK 21, Maven, Node.js, and npm.

### 1. Clone the repository

```bash
git clone https://github.com/thirulokeshh/AdSentry.git
cd AdSentry
```

### 2. Start the backend

Open a terminal in the `backend` directory.

**Windows CMD:**
```cmd
set SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

**macOS / Linux:**
```bash
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

The backend runs at `http://localhost:8080`.

### 3. Start the frontend

Open a second terminal in the `frontend` directory.

```bash
npm install
npm run dev
```

Open the local URL printed by Vite, normally `http://localhost:5173`.

### 4. Verify the backend

Visit `http://localhost:8080/actuator/health`.

A successful response should include:

```json
{ "status": "UP" }
```

> **Local database:** The local profile uses an in-memory H2 database. Data is lost when the backend stops. Production uses PostgreSQL.

---

## Run Tests

From the `backend` directory:

```bash
mvn test
```

---

## Repository

[GitHub — AdSentry](https://github.com/thirulokeshh/AdSentry)

---

<p align="center">
  Built with Java, Spring Boot, React, and PostgreSQL.
</p>
