# Fraud & AML Case Management Platform (Bank-Grade, Production-Style)

A production-style **Fraud & AML monitoring + case management** platform designed with **banking controls** in mind:
- **Real-time transaction ingestion** (Kafka/Redpanda)
- **Velocity rules** (Redis) for burst detection
- **Policy engine** (JSON-based) for AML/Fraud indicators
- **Whitelist / Exception management** to control false positives (hard/soft bypass)
- **Case management** with **evidence timeline**
- **Maker–Checker (4-eyes)** approvals for critical actions (block/unblock)
- **SAR/STR report generation** (versioned)
- **Observability** (Prometheus + Grafana) with custom metrics

> This project is intentionally built to reflect **banking-grade patterns**: auditability, traceability, controlled actions, and operational visibility.

---

## Architecture Overview

**Event Flow**
1. Transaction events are produced into Kafka topic: `transactions.created`
2. `TransactionConsumer` processes events:
   - **Whitelist check** (customer/iban/mcc/rule)  
   - **Velocity check** (Redis counters, time window)  
   - **Policy evaluation** (JSON rules)
3. If a rule triggers:
   - **FraudAlert** is created
   - **Case** is opened/reused
   - Alert is attached to the case and written into **Case Timeline**
4. **Actions** (block/unblock) are performed via **Maker–Checker** requests:
   - Maker creates request → Checker approves/rejects → system executes

**Core Services**
- `PolicyEngine` → JSON policy evaluation
- `VelocityService` → Redis-based burst detection
- `WhitelistService` → exception management
- `CaseService` → case lifecycle + evidence timeline
- `ActionRequestService` → maker-checker workflow
- `SarReportService` → SAR/STR report generation
- `MetricsService` → custom metrics + Prometheus endpoint

---

## Tech Stack

- **Java 17**, Spring Boot (Actuator + JPA)
- **PostgreSQL** (cases, alerts, whitelist, action_requests, reports, audit)
- **Redpanda / Kafka** (real-time event ingestion)
- **Redis** (velocity counters)
- **Prometheus + Grafana** (metrics + dashboards)
- Docker Compose for local environment parity

---

## Repository Layout (Typical)

.
├─ docker-compose.yml
├─ prometheus.yml
├─ grafana/
│ ├─ provisioning/
│ │ ├─ datasources/datasource.yml
│ │ └─ dashboards/dashboard.yml
│ └─ dashboards/fraud_aml_dashboard.json
├─ src/main/java/com/bank/fraud/
│ ├─ api/...
│ ├─ kafka/...
│ ├─ service/...
│ ├─ policy/...
│ └─ domain/...
└─ src/main/resources/
└─ application.yml

yaml
Kodu kopyala

---

## Quick Start (Local)

### Prerequisites
- Docker Desktop
- Java 17 (if running without Docker build)
- Maven/Gradle (depending on your build setup)

### Run Everything
```bash
docker compose up -d --build
Verify Services
API Health: http://localhost:9200/actuator/health

Prometheus: http://localhost:9090

Grafana: http://localhost:3000 (default: admin/admin)

API Endpoints (Core)
Cases
List cases:

GET /v1/cases?status=OPEN

GET /v1/cases?customerNo=CUST00042

Case timeline:

GET /v1/cases/{caseId}/timeline

Add analyst note:

POST /v1/cases/{caseId}/notes?note=...&actor=talha

Update status:

POST /v1/cases/{caseId}/status?status=INVESTIGATING&actor=talha

Whitelist / Exceptions (Admin)
List:

GET /v1/admin/whitelist

GET /v1/admin/whitelist?type=CUSTOMER

Upsert:

POST /v1/admin/whitelist/upsert?type=CUSTOMER&value=CUST00099&hardBypass=true&reason=VIP&createdBy=talha&ticketRef=INC-1001&expiresAt=2026-02-01T00:00:00Z

Delete:

DELETE /v1/admin/whitelist/{id}?actor=talha

Bypass modes

hardBypass=true: no alert/case/action

hardBypass=false: alert/case allowed, auto-block disabled

Maker–Checker Actions (4 eyes principle)
Create request (Maker):

POST /v1/actions/request?customerNo=CUST00042&actionType=BLOCK_CREDIT&reason=...&maker=talha&caseId=1

Approve and execute (Checker):

POST /v1/actions/{id}/approve?checker=supervisor&note=Approved

Reject (Checker):

POST /v1/actions/{id}/reject?checker=supervisor&note=Insufficient%20evidence

List:

GET /v1/actions?status=PENDING

SAR/STR Reports
Generate report:

POST /v1/reports/generate?caseId=1&type=SAR&actor=talha&narrative=...

List reports:

GET /v1/reports?caseId=1

Get report JSON:

GET /v1/reports/{id}/json

Metrics & Observability
Prometheus Metrics
Prometheus scrape endpoint:

GET /actuator/prometheus

Custom Metrics (Examples)
kafka_events_processed_total

fraud_alerts_total{type,severity}

fraud_cases_opened_total{priority}

credit_blocks_total{reason}

Grafana
Dashboard is provisioned automatically:

Folder: FraudAML

Dashboard: Fraud & AML - Ops Dashboard

Security Notes (Bank-Oriented)
This is a production-style demo. In real banking environments, you typically add:

Strong authentication/authorization (SSO/OAuth2, mTLS, RBAC)

Data masking for PII fields (IBAN, phone, name)

KMS-backed secret management (Vault, AWS KMS, HSM)

Full WORM-style audit logging (tamper-evident)

Stronger AML typologies + sanctions screening integrations
