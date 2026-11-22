# CloudPulse — API Reliability & Smart Health Validation Platform

[![Build & Verification](https://github.com/cloudpulse/cloudpulse/actions/workflows/ci.yml/badge.svg)](https://github.com/cloudpulse/cloudpulse/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Tests](https://img.shields.io/badge/Tests-167%20Passed%20(129%20Backend%20+%2038%20Frontend)-success.svg)](backend/src/test)

CloudPulse is a production-grade, full-stack API reliability, uptime monitoring, and smart health validation platform. It is engineered to solve a fundamental flaw in traditional monitoring tools: **traditional uptime monitors report `HTTP 200 OK` as healthy even when an API endpoint returns hidden application-level failures, depleted database pools, or invalid payloads.**

CloudPulse inspects actual HTTP response payloads using a high-performance **Smart API Validation Engine**, detects anomalies statistically, manages incidents with automatic recovery, sends transactional notifications with crash recovery, publishes tenant-isolated public status pages, and generates audit-ready PDF SLA compliance reports.

---

## 1. High-Level Architecture

CloudPulse follows a modular monolith design with strict layer separation, transaction boundaries, and tenant isolation:

```
                                  [ Client Browser ]
                     (React 18 / TypeScript / Tailwind CSS / Recharts)
                                          │
                        HTTP / REST APIs (JWT Bearer Token)
                                          │
                                          ▼
                             [ Spring Boot 3.3.5 API ]
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  Rate Limiting Filter  ──▶  Spring Security  ──▶  JWT AuthenticationFilter │
  └──────────────────────────────────────┬──────────────────────────────────────┘
                                         │
        ┌────────────────────────────────┼────────────────────────────────┐
        ▼                                ▼                                ▼
[ Monitor Management ]        [ Validation Engine ]            [ Anomaly Detection ]
- Multi-protocol (GET/POST)   - JSONPath Assertions            - Z-score (z >= 3.0)
- Custom headers & body       - SLA Latency Limits             - Failure Burst (3+)
- Tenant-scoped CRUD          - Syntax Validation              - Rolling Uptime Drop
        │                                │                                │
        ▼                                ▼                                ▼
[ ShedLock Scheduler ]        [ SSRF Protection Client ]       [ Transactional Outbox ]
- Distributed PostgreSQL Lock - RFC1918 Private IP Block       - At-least-once delivery
- Overlap Prevention          - AWS/GCP Metadata Block         - Atomic DB Leases
- Concurrency Throttle        - DNS Re-validation              - Worker Crash Recovery
        │                                │                                │
        └────────────────────────────────┼────────────────────────────────┘
                                         │
                                         ▼
                             [ PostgreSQL 16 Database ]
               Flyway Migrations (V1 __init -> V5 __create_anomalies)
```

---

## 2. Core Functional Modules

### 2.1 Smart API Validation Engine
- **JSONPath Value Assertions**: Compares actual vs expected values (e.g., `$.status == "success"`, `$.data.user != null`).
- **Hidden Outage Detection**: Identifies endpoints returning `HTTP 200 OK` with payload bodies such as `{"status":"error","message":"Database connection pool exhausted"}` and flags them as true outages.
- **SLA Latency Enforcement**: Verifies response latency against configured SLA thresholds in milliseconds.
- **JSON Syntax & Field Verification**: Guards against malformed payloads and missing contract fields.

### 2.2 Multi-Layer SSRF Defense-in-Depth
- **Pre-Connection Validation**: Enforces scheme whitelisting (`http`/`https`), strips credentials from URLs, and restricts dangerous port ranges.
- **DNS Resolution Check**: Prohibits loopback (`127.0.0.0/8`, `::1`), RFC 1918 private subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`), Link-Local addresses, and cloud metadata services (`169.254.169.254`, `metadata.google.internal`).
- **Redirect Re-Validation**: Intercepts 301/302/307/308 HTTP redirects and re-validates the destination URL to prevent intranet pivoting.
- **Payload & Connection Bounds**: Enforces maximum response body sizes (1 MB) and strict socket timeouts to prevent memory exhaustion and slowloris attacks.

### 2.3 Adaptive Theme System (Light, Dark, System)
- **Three-Mode Operation**: Seamless switching between Light, Dark, and OS-synchronized System modes.
- **Zero-FOUC Guarantee**: Inline DOM script initializes root classes before paint.
- **Dynamic Chart Palette**: Chart canvas and tooltip tokens adapt dynamically to contrast and theme state.

### 2.4 Phase 2.1 — Transactional Outbox Email Notifications
- **At-Least-Once Delivery**: Transactional Outbox pattern guarantees no alert is lost during network partitions or crashes.
- **Concurrency & Lease Ownership**: Atomic database row claiming (`lease_until`, `claimed_by`) prevents race conditions between parallel workers.
- **Worker Crash Recovery**: Background reaper recovers stale `IN_FLIGHT` leases automatically.
- **Mailpit Integration**: Local zero-cost SMTP development server with web interface on port `8025`.

### 2.5 Phase 2.2 — Public Status Pages
- **Privacy by Default**: Pages default to `is_published: false` until explicitly published.
- **Vanity URL Slug Generation**: Cryptographically secure random slugs with custom vanity support and collision retry.
- **Accurate Uptime Math**: Formula $Uptime = (N_{success} / N_{valid}) \times 100$, with distinct `NO_DATA` states when no checks exist.
- **Private Data Sanitization**: Public DTOs strictly redact internal URLs, headers, payloads, and validation rules.
- **30-Day Incident History**: Displays recent incident durations and root cause notes.

### 2.6 Phase 2.3 — PDF Monitoring & SLA Audit Reports
- **Powered by OpenPDF 2.0.3**: Pure Java document generation with zero external cloud dependencies.
- **Four Audit Templates**:
  1. *Portfolio Executive Summary*: Consolidated health overview of all monitors.
  2. *Single Monitor SLA Report*: Latency percentiles (P95, P99), uptime %, and check distributions.
  3. *Incident Audit Log*: Historical downtime duration and recovery post-mortems for compliance audits.
  4. *Performance & SLA SLA Report*: Latency distributions, throughput metrics, and SLA violations.
- **Instant Browser Download**: Streams `application/pdf` with compliant `Content-Disposition` headers.

### 2.7 Phase 2.4 — Advanced Analytics & Statistical Anomaly Detection
- **Z-Score Latency Spikes**: Detects statistically anomalous latency spikes ($Z \ge 3.0$ or $\ge 2.5\times$ baseline).
- **Failure Bursts**: Flags multiple consecutive check failures with severity grading (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`).
- **Uptime Degradation**: Identifies monitors where rolling 24-hour uptime drops below 95%.
- **Reliability Metrics**:
  - **MTTR (Mean Time to Recovery)**: Average incident duration from detection to recovery.
  - **MTBF (Mean Time Between Failures)**: Average operational uptime hours between incidents.
  - **P95 / P99 Tail Latencies**: High-percentile latency distributions.
  - **1-Click Acknowledge**: Operations dashboard with audit trail and unacknowledged alert counters.

---

## 3. Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.3.5, Spring Security, JWT (HMAC-SHA256), Spring Data JPA, Hibernate 6 |
| **PDF Engine** | OpenPDF 2.0.3 (LibrePDF) |
| **Scheduling** | ShedLock (PostgreSQL-backed distributed locking) |
| **Database** | PostgreSQL 16, Flyway Database Migrations (V1 through V5) |
| **Frontend** | React 18, TypeScript 5, Vite 5, Tailwind CSS, Recharts, Lucide Icons, Axios |
| **Email Server** | Mailpit (Local SMTP on port 1025, Web UI on port 8025) |
| **Containers** | Docker, Docker Compose, Multi-stage builds |
| **CI/CD** | GitHub Actions Pipeline (`.github/workflows/ci.yml`) |

---

## 4. REST API Endpoint Reference

### Authentication (`/api/auth`)
- `POST /api/auth/register` — Create a new user account.
- `POST /api/auth/login` — Authenticate and receive JWT access and refresh tokens.
- `POST /api/auth/refresh` — Rotate refresh token and obtain a new access token.
- `GET /api/auth/me` — Retrieve the currently authenticated user's profile.

### Monitors (`/api/monitors`)
- `GET /api/monitors` — List all monitors owned by the authenticated tenant.
- `POST /api/monitors` — Create a new monitor with validation rules (SSRF checked).
- `GET /api/monitors/{id}` — Get monitor details with latest check results and validation rules.
- `PUT /api/monitors/{id}` — Update monitor configuration (interval, timeout, validation rules).
- `DELETE /api/monitors/{id}` — Delete a monitor and its historical telemetry.
- `POST /api/monitors/{id}/check` — Trigger an immediate on-demand probe check.
- `POST /api/monitors/{id}/pause` — Pause monitor execution.
- `POST /api/monitors/{id}/resume` — Resume monitor execution.

### Public Status Pages (`/api/status-pages` & `/api/public/status`)
- `GET /api/status-pages` — List tenant's status pages.
- `POST /api/status-pages` — Create a new status page (private by default).
- `PUT /api/status-pages/{id}` — Update status page title, description, slug, or monitors.
- `PATCH /api/status-pages/{id}/publish` — Toggle status page publish state (`true`/`false`).
- `DELETE /api/status-pages/{id}` — Delete a status page.
- `GET /api/public/status/{slug}` — Anonymous public view of a published status page (private data stripped).

### PDF Monitoring Reports (`/api/reports`)
- `GET /api/reports/monitors/{id}/pdf` — Download Single Monitor SLA PDF Report.
- `GET /api/reports/summary/pdf` — Download Portfolio Executive Summary PDF Report.
- `GET /api/reports/incidents/pdf` — Download Incident & Downtime Audit Log PDF Report.
- `GET /api/reports/performance/pdf` — Download Latency & SLA Performance PDF Report.

### Statistical Anomalies & Analytics (`/api/anomalies` & `/api/analytics`)
- `GET /api/analytics/dashboard` — Overview metrics, trends, and recent incidents.
- `GET /api/analytics/advanced` — MTTR, MTBF, P95/P99 latencies, window uptimes (24h/7d/30d).
- `GET /api/anomalies` — Paginated list of detected anomalies for tenant.
- `GET /api/anomalies/monitors/{monitorId}` — Anomalies for a specific monitor.
- `POST /api/anomalies/{id}/acknowledge` — Mark an anomaly as acknowledged.
- `GET /api/anomalies/unacknowledged-count` — Count of unacknowledged anomalies.

### Notifications (`/api/notifications`)
- `GET /api/notifications/preferences` — Get email preferences and triggers.
- `PUT /api/notifications/preferences` — Update preferences and notification triggers.
- `GET /api/notifications/history` — Paginated delivery audit log (SENT, FAILED, SKIPPED).
- `POST /api/notifications/test` — Dispatch an immediate test email alert.

---

## 5. Database Schema & Migration History

CloudPulse strictly utilizes Flyway for schema versioning:

1. **`V1__init_schema.sql`**: Users, Monitors, Validation Rules, Check Results, Incidents, ShedLock tables.
2. **`V2__create_notifications_schema.sql`**: Notification Preferences, Triggers, and Notification History.
3. **`V3__harden_notifications_outbox.sql`**: Transactional outbox with `idempotency_key`, atomic lease claiming (`claimed_by`, `lease_until`), and delivery status.
4. **`V4__create_status_pages_schema.sql`**: Public status pages with unique vanity slugs, `is_published DEFAULT FALSE`, and monitor mapping table.
5. **`V5__create_anomalies_schema.sql`**: Anomaly detection schema with `anomaly_type`, `severity`, `observed_value`, `baseline_value`, and acknowledgment tracking.

---

## 6. Getting Started & Deployment

### Quickstart with Docker Compose (Recommended)

```bash
# 1. Clone repository
git clone https://github.com/cloudpulse/cloudpulse.git
cd cloudpulse

# 2. Configure environment variables (defaults work out of the box)
cp .env.example .env

# 3. Build and launch all 4 containers in the background
docker compose up -d --build

# 4. Verify running services
docker compose ps
```

### Active Service Ports
| Service | Internal Port | Host Port | Description |
| :--- | :--- | :--- | :--- |
| **Frontend** | `80` (Nginx) | `http://localhost:5174` | Web Application UI & Public Status Pages |
| **Backend** | `8081` | `http://localhost:8081` | REST APIs & Actuator Health Endpoint |
| **PostgreSQL**| `5432` | `localhost:5435` | Database with persistent volume storage |
| **Mailpit** | `1025` (SMTP) / `8025` (HTTP) | `http://localhost:8025` | Webmail interface for inspecting email alerts |

### Verification Endpoints
- **Application Dashboard**: [http://localhost:5174/dashboard](http://localhost:5174/dashboard)
- **Actuator Health Check**: [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)
- **Mailpit Email Inbox**: [http://localhost:8025](http://localhost:8025)

---

## 7. Verification & Automated Test Results

The CloudPulse platform is covered by comprehensive unit, integration, and security test suites across both backend and frontend:

### Backend Test Results (Maven / JUnit 5 / Mockito)
```bash
cd backend
mvn test
```
```
[INFO] Results:
[INFO] Tests run: 129, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- **SsrfProtectionServiceTest** (17 tests): Loopback, RFC1918, AWS metadata (`169.254.169.254`), GCP metadata, redirect pivoting, IPv6 unique local.
- **ValidationEngineTest** (6 tests): JSONPath assertions, SLA latency limits, HTTP 200 payload error detection, header matches.
- **IncidentServiceTest** (4 tests): 3-strike failure threshold escalation, duplicate alert suppression, auto-recovery downtime calculation.
- **NotificationModuleTest** (25 tests): Transactional outbox, atomic claiming, worker crash recovery, retry backoff, lease expiration.
- **StatusPageTest Suite** (18 tests): Slug generation, collision safety, uptime calculation, private data redacting, anonymous access.
- **ReportTest Suite** (13 tests): OpenPDF rendering, date-range bounds, P95/P99 latency math, tenant isolation.
- **AnomalyDetectionTest Suite** (12 tests): Z-score latency spike detection, failure bursts, rolling uptime degradation, MTTR/MTBF math, acknowledgment.
- **AuthControllerTest & SecurityTest** (34 tests): JWT rotation, RBAC, Rate limiting filter, tenant isolation.

### Frontend Test Results (Node Test Runner / React)
```bash
cd frontend
npm test
```
```
ℹ tests 38
ℹ suites 6
ℹ pass 38
ℹ fail 0
ℹ cancelled 0
ℹ skipped 0
```
- **Application Theme Suite** (8 tests): Light/Dark/System resolution, zero-FOUC script, localStorage persistence, chart color tokens.
- **Edit Monitor Suite** (8 tests): Client validation, JSON formatting, SLA rule types, header matching.
- **Notification Suite** (6 tests): Preference toggles, delivery badge formatting, audit pagination.
- **Status Pages Suite** (6 tests): Private-by-default, vanity slug regex, overall status computation, uptime denominator accuracy.
- **PDF Reports Suite** (5 tests): Preset date ranges, endpoint resolution, filename parsing from headers, SLA statistics math.
- **Advanced Analytics & Anomalies Suite** (5 tests): Z-score computation, severity mapping, MTTR math, MTBF math, category filtering.

---

## 8. Academic Demonstration & Submission Checklist

For university examiners, capstone reviewers, and technical interviewers, use this step-by-step walkthrough:

1. **System Health Verification**:
   - Open [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health) -> verify status `UP`.
   - Open [http://localhost:5174](http://localhost:5174) -> log in with `testuser@cloudpulse.io` / `Password123!`.
2. **Adaptive Theme Demonstration**:
   - Toggle between Light, Dark, and System in the top bar. Note zero-FOUC instant re-rendering and dynamic chart recoloring.
3. **Smart API Validation & Incident Escalation**:
   - Navigate to **New Monitor** -> configure an endpoint with Smart JSONPath Validation (e.g. `$.status == "success"` and latency `< 500ms`).
   - Trigger a simulated failure: witness consecutive failure counter increment.
   - On 3 consecutive failures: observe instant escalation to an **Incident** and outbox email alert sent to Mailpit ([http://localhost:8025](http://localhost:8025)).
4. **Public Status Page Showcase**:
   - Navigate to **Status Pages** -> click **New Status Page**.
   - Note private-by-default switch (`isPublished: false`).
   - Toggle to published, copy public vanity URL (e.g., `/status/acme-status`).
   - Open in an incognito window: observe public view displaying operational status, uptime (24h/7d/30d), and 30-day incident log with zero internal credentials leaked.
5. **Audit-Ready PDF Reports**:
   - Navigate to **Reports** -> select **Portfolio Summary** or **Single Monitor SLA Report**.
   - Choose a date range (e.g., Last 7 Days) -> click **Generate & Download PDF**.
   - Open downloaded PDF: observe header, executive summary, latency SLA percentiles (P95, P99), and incident resolution audit logs rendered via OpenPDF.
6. **Statistical Anomaly Detection & Reliability Analytics**:
   - Navigate to **Analytics** -> inspect MTTR, MTBF, and tail latencies.
   - Review the **Anomaly Detection Center**: observe detected latency spikes (Z-score &ge; 3.0) and failure bursts.
   - Click **Acknowledge** on an anomaly: observe instant state update and counter decrement.
7. **Automated Test Run**:
   - Execute `cd backend && mvn test` (129 tests passed).
   - Execute `cd frontend && npm test` (38 tests passed).

---

## 9. Zero-Budget & Security Compliance

CloudPulse is developed under strict zero-budget compliance (₹0 incurred). All infrastructure, databases, mail catchers, and monitoring probes execute locally inside optimized Docker containers. No paid cloud subscriptions or billable external APIs are utilized. Production cloud migration paths are documented in [AWS_DEPLOYMENT.md](file:///d:/Projects/CloudPulse/AWS_DEPLOYMENT.md).
