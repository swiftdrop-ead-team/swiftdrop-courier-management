# SwiftDrop — Parcel Delivery & Courier Management System
> **"Delivering trust, every mile."**

[![CI Pipeline](https://github.com/swiftdrop-ead-team/swiftdrop-courier-management/actions/workflows/ci.yml/badge.svg)](https://github.com/swiftdrop-ead-team/swiftdrop-courier-management/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-teal.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-navy.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)

An enterprise-grade, microservices-based logistics and courier platform developed for the **Enterprise Application Development (EAD)** group assignment. SwiftDrop orchestrates parcel intake, dynamic pricing algorithms, intelligent courier scheduling, real-time tracking, and administrative compliance verifications across 10 independent microservices.

---

## 1. High-Level Architecture

```
                      +-----------------------------+
                      |       Web / Mobile App      |
                      |   (React 18 / TypeScript)   |
                      +--------------+--------------+
                                     |
                                     | HTTPS / WSS / REST
                                     v
                      +-----------------------------+
                      |     Spring Cloud Gateway    |
                      |         (Port 8080)         |
                      |  - Edge Routing & CORS      |
                      |  - Correlation IDs          |
                      |  - Security Headers Filter  |
                      +--------------+--------------+
                                     |
            +------------------------+------------------------+
            |                        |                        |
            v                        v                        v
+-----------------------+ +---------------------+ +----------------------+
|   Identity Service    | |   Parcel Service    | |   Tracking Service   |
|      (Port 8081)      | |     (Port 8082)     | |     (Port 8086)      |
|  - JWT & Sessions     | |  - Order Lifecycle  | |  - Real-time GPS     |
|  - Google OAuth 2.0   | |  - Waybills & Label | |  - WebSockets/SSE    |
|  - RBAC & Approvals   | |  - Order Ingestion  | |  - Movement Audits   |
+-----------+-----------+ +----------+----------+ +----------+-----------+
            |                        |                       |
            v                        v                       v
+-----------------------+ +---------------------+ +----------------------+
|  PostgreSQL Database  | | PostgreSQL Database | | PostgreSQL Database  |
| (swiftdrop_identity)  | |  (swiftdrop_parcel) | | (swiftdrop_tracking) |
+-----------------------+ +---------------------+ +----------------------+
                                     |
                                     v
                      +-----------------------------+
                      |     RabbitMQ Event Bus      |
                      |  - parcel.status_updated    |
                      |  - courier.assigned         |
                      |  - notification.dispatch    |
                      +--------------+--------------+
                                     |
            +------------------------+------------------------+
            |                        |                        |
            v                        v                        v
+-----------------------+ +---------------------+ +----------------------+
|   Dispatch Service    | |    Pricing Service  | | Notification Service |
|      (Port 8084)      | |      (Port 8085)    | |      (Port 8087)     |
|  - Courier Scheduling | |  - Rate Engine      | |  - Multi-channel     |
|  - Route Optimization | |  - Zone Surcharges  | |    (Email/SMS/Push)  |
+-----------------------+ +---------------------+ +----------------------+
                                     |
                                     v
                      +-----------------------------+
                      |  AI Assistant & Analytics   |
                      |         (Port 8088)         |
                      |  - Python FastAPI & Gemini  |
                      |  - Predictive ETA Engine    |
                      +-----------------------------+
```

---

## 2. Monorepo Directory Structure

```text
swiftdrop-courier-management/
│
├── frontend/                     # React 18 / TypeScript / Vite / Tailwind UI (Port 3000)
│   ├── src/
│   │   ├── assets/               # Brand SVGs and illustrations
│   │   ├── components/
│   │   │   ├── ui/               # Logo, Navbar, Footer, UI components
│   │   │   └── layout/           # MainLayout & Role DashboardLayout
│   │   ├── pages/
│   │   │   ├── auth/             # Split-screen Login, Customer/Merchant Register, Reset
│   │   │   ├── customer/         # Customer delivery booking & tracking portal
│   │   │   ├── merchant/         # Merchant business profile & PENDING/APPROVED states
│   │   │   ├── courier/          # Courier status & dispatch waypoint console
│   │   │   └── admin/            # Administrator user auditing & merchant approvals
│   │   ├── routes/               # ProtectedRoute, RoleRoute, GuestRoute guards
│   │   ├── hooks/                # useAuth & context providers
│   │   ├── services/             # Axios API client & auto-refresh interceptors
│   │   └── types/                # TypeScript domain models
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
│
├── gateway/                      # Spring Cloud Gateway (Port 8080)
│   ├── src/                      # Correlation ID filter, Security Headers, Edge routing
│   ├── pom.xml
│   ├── Dockerfile
│   └── README.md
│
├── services/
│   ├── identity-service/         # Authentication & RBAC Microservice (Port 8081)
│   │   ├── src/main/java/        # BCrypt, JJWT 0.12.6, Google OAuth2, Spring Data JPA
│   │   ├── src/main/resources/   # Flyway V1 schema & V2 seeds, application.yml
│   │   ├── src/test/             # JUnit 5 & Mockito automated test suites
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── README.md
│   │
│   ├── parcel-service/           # Reserved (Member 3) — Order intake & parcel lifecycle
│   ├── merchant-service/         # Reserved (Member 4) — Bulk manifests & merchant profiles
│   ├── dispatch-service/         # Reserved (Member 5) — Route optimization & courier matching
│   ├── pricing-service/          # Reserved (Member 6) — Dynamic pricing calculation engine
│   ├── tracking-service/         # Reserved (Member 7) — GPS telemetry & WebSockets/SSE
│   ├── notification-service/     # Reserved (Member 8) — Email/SMS/Push RabbitMQ listeners
│   └── ai-assistant-service/     # Reserved (Member 9) — Python FastAPI & Google Gemini API
│
├── infrastructure/
│   ├── database/                 # init.sql creating per-service databases
│   └── README.md
│
├── docs/
│   ├── architecture/             # High-level architecture and sequence diagrams
│   ├── api-contracts/            # OpenAPI specifications and REST endpoints
│   ├── contribution-guide.md     # Git workflow, PR rules, and team boundaries
│   └── initial-codebase-report.md# Formal initial codebase report for grading
│
├── .github/
│   ├── workflows/ci.yml          # GitHub Actions CI for frontend and backend builds
│   └── pull_request_template.md  # Standardized PR checklist and member tagger
│
├── .gitignore
├── .env.example
├── docker-compose.yml
└── README.md
```

---

## 3. Team Ownership Matrix

Each of the 10 microservices is assigned to an individual team member with a dedicated Git feature branch:

| Member | Microservice / Domain | Git Feature Branch | Directory | Port | Status |
|:---|:---|:---|:---|:---|:---|
| **Member 1** | Frontend Architecture & UI/UX | `feature/frontend` | `frontend/` | `3000` | ✅ Active Milestone |
| **Member 2** | Identity Service & Auth | `feature/identity-service` | `services/identity-service/` | `8081` | ✅ Active Milestone |
| **Member 3** | Parcel Service | `feature/parcel-service` | `services/parcel-service/` | `8082` | ⏳ Phase 2 |
| **Member 4** | Merchant Service | `feature/merchant-service` | `services/merchant-service/` | `8083` | ⏳ Phase 2 |
| **Member 5** | Dispatch Service | `feature/dispatch-service` | `services/dispatch-service/` | `8084` | ⏳ Phase 2 |
| **Member 6** | Pricing Service | `feature/pricing-service` | `services/pricing-service/` | `8085` | ⏳ Phase 2 |
| **Member 7** | Tracking Service | `feature/tracking-service` | `services/tracking-service/` | `8086` | ⏳ Phase 2 |
| **Member 8** | Notification Service | `feature/notification-service` | `services/notification-service/` | `8087` | ⏳ Phase 2 |
| **Member 9** | AI Assistant (Gemini) | `feature/ai-assistant` | `services/ai-assistant-service/` | `8088` | ⏳ Phase 2 |
| **Member 10** | API Gateway & DevOps | `feature/gateway-devops` | `gateway/` | `8080` | ✅ Active Milestone |

---

## 4. Evaluator Test Accounts

The system seeds active test accounts into PostgreSQL for easy evaluation of all role-based experiences:

| Role | Email | Password | Landing Route | Features Demonstrated |
|:---|:---|:---|:---|:---|
| **Administrator** | `admin@swiftdrop.io` | `Admin@SwiftDrop2026!` | `/admin/dashboard` | User audit list, live Approve/Reject buttons for merchants |
| **Customer** | `customer@swiftdrop.io` | `Customer@123!` | `/customer/dashboard` | Parcel booking modal, tracking query, empty states |
| **Merchant (Approved)** | `merchant.approved@swiftdrop.io` | `Merchant@123!` | `/merchant/dashboard` | Active commercial partner badge, live API secret key |
| **Merchant (Pending)** | `merchant.pending@swiftdrop.io` | `Merchant@123!` | `/merchant/dashboard` | Clear PENDING review compliance banner |
| **Field Courier** | `courier@swiftdrop.io` | `Courier@123!` | `/courier/dashboard` | Online/Offline driver status toggle, route standby |

> [!TIP]
> At `/login`, click any of the **Assignment Evaluator Quick Credentials** buttons to instantly fill and test any of the 4 roles.

---

## 5. Quick Start Instructions

### Running with Docker Compose (Recommended)
```bash
# 1. Clone repository
git clone https://github.com/swiftdrop-ead-team/swiftdrop-courier-management.git
cd swiftdrop-courier-management

# 2. Configure environment
cp .env.example .env

# 3. Launch stack
docker compose up --build -d
```
Access points:
- **React Frontend**: `http://localhost:3000`
- **API Gateway**: `http://localhost:8080`
- **Identity Service**: `http://localhost:8081`
- **PostgreSQL**: `localhost:5432`

### Running Locally (Native Development)

#### 1. Identity Service
```bash
cd services/identity-service
./mvnw clean test
./mvnw spring-boot:run
```

#### 2. API Gateway
```bash
cd gateway
./mvnw clean test
./mvnw spring-boot:run
```

#### 3. React Frontend
```bash
cd frontend
npm install
npm test
npm run dev
```

---

## 6. Google OAuth 2.0 Setup

To enable live Google OAuth login:
1. Navigate to the [Google Cloud Console](https://console.cloud.google.com/apis/credentials).
2. Create an **OAuth 2.0 Client ID** (Application type: *Web application*).
3. Set **Authorized redirect URI** to:
   ```text
   http://localhost:8081/login/oauth2/code/google
   ```
4. Copy your Client ID and Client Secret into your `.env` file:
   ```env
   GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=your-google-client-secret
   ```

---

## 7. Submission Link

- **Formal Report**: [docs/initial-codebase-report.md](file:///Users/zinexai/Desktop/swiftdrop/docs/initial-codebase-report.md)
- **Monorepo Repository**: [https://github.com/swiftdrop-ead-team/swiftdrop-courier-management](https://github.com/swiftdrop-ead-team/swiftdrop-courier-management)
