# SwiftDrop — Parcel Delivery & Courier Management System
## Initial Codebase & Microservices Foundation Milestone Report
**Course**: Enterprise Application Development (EAD)  
**Team**: SwiftDrop Engineering Group (10 Members)  
**Date**: November 2026  

---

## Initial Codebase Repository

**Verified GitHub Repository URL**:  
🔗 **[https://github.com/swiftdrop-ead-team/swiftdrop-courier-management](https://github.com/swiftdrop-ead-team/swiftdrop-courier-management)**

> [!IMPORTANT]
> The complete working source code for the SwiftDrop initial milestone is committed and structured within the single shared monorepo linked above. All authentication flows, database migrations, Spring Cloud Gateway routes, role-based dashboards, and automated test suites are fully integrated.

---

## 1. Executive System Overview

**SwiftDrop** is an enterprise-grade logistics and parcel delivery management SaaS platform engineered with a microservices architecture. Designed to handle high-throughput delivery intake, dynamic pricing matrices, intelligent route dispatching, and real-time courier telemetry, the platform serves three primary stakeholder tiers:
1. **Retail Customers**: Quick one-click parcel booking, automated waybills, and real-time status tracking.
2. **Commercial Merchants**: High-volume B2B consignment intake, bulk manifest ingestion, automated API keys, and commercial billing.
3. **Field Couriers**: Dynamic route optimization, waypoint sequencing, turn-by-turn navigation, and proof-of-delivery capture.
4. **Platform Administrators**: Complete system oversight, user role administration, compliance audits, and real-time merchant approval workflows.

---

## 2. Microservices Architecture & System Design

The platform adheres to strict microservice isolation standards:
- **Independent Bounded Contexts**: Each microservice maintains its own dedicated logical database in compliance with the "Database-per-Service" pattern.
- **Edge Routing & Ingress**: All inbound client traffic flows through **Spring Cloud Gateway (Port 8080)**, which enforces edge security headers, global CORS policies, and request correlation tracking (`X-Correlation-Id`).
- **Identity & Access Management Boundary**: **Identity Service (Port 8081)** acts as the centralized authority for authentication, credential verification, RBAC, Google OAuth 2.0 federation, and session token lifecycle. Downstream services independently validate standard JWT claims.
- **Asynchronous Event Messaging (Future Phase)**: Microservices will exchange state change notifications asynchronously via RabbitMQ exchanges.

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

## 3. Technology Stack

### Frontend Application
- **Framework**: React 18 with TypeScript and Vite 6.
- **Styling**: Tailwind CSS tailored with the SwiftDrop palette (Midnight Navy `#10243A`, Premium Teal `#0D9488`, Soft Slate `#F8FAFC`).
- **Icons & Motion**: Lucide React.
- **Routing & State**: React Router 6, TanStack Query 5.
- **Form Management & Validation**: React Hook Form with Zod schemas.
- **API Client**: Axios with automatic token refresh response interceptors.

### Backend Microservices
- **Runtime & Language**: Java 21 LTS.
- **Framework**: Spring Boot 3.4.3.
- **Edge Gateway**: Spring Cloud Gateway 2024.0.0 (Reactive WebFlux).
- **Security**: Spring Security 6.x, Spring Security OAuth2 Client.
- **JWT Cryptography**: JJWT (`io.jsonwebtoken 0.12.6`, HMAC-SHA256).
- **Persistence**: Spring Data JPA, Hibernate, Flyway Migration Engine.
- **Database**: PostgreSQL 16.
- **Build & Packaging**: Apache Maven 3.9.9, Docker Multi-Stage Containerization.

---

## 4. Implemented Milestone Scope vs Planned Future Services

| Microservice / Component | Assigned Member | Implementation Status | Key Features Delivered in Milestone |
|:---|:---|:---|:---|
| **API Gateway** | Member 10 | ✅ **Completed** | Dynamic routing to Identity Service, Correlation IDs, CORS, Healthcheck |
| **Identity Service** | Member 2 | ✅ **Completed** | Full auth lifecycle, BCrypt hashing, JWT tokens, OAuth2, Flyway migrations |
| **React Frontend** | Member 1 | ✅ **Completed** | Split-screen login, customer/merchant forms, 4 starter dashboards |
| **PostgreSQL Persistence** | Member 10 / 2 | ✅ **Completed** | Flyway V1 schema, V2 role seeds, test account initialization |
| **Parcel Service** | Member 3 | ⏳ *Planned Phase 2* | Waybill lifecycle, parcel state machine, barcode/QR generation |
| **Merchant Service** | Member 4 | ⏳ *Planned Phase 2* | Bulk manifest uploads, store locations, merchant analytics |
| **Dispatch Service** | Member 5 | ⏳ *Planned Phase 2* | Route optimization, courier assignment algorithms |
| **Pricing Service** | Member 6 | ⏳ *Planned Phase 2* | Dynamic rate calculation matrix, zone surcharges |
| **Tracking Service** | Member 7 | ⏳ *Planned Phase 2* | GPS telemetry ingestion, WebSockets / SSE real-time streaming |
| **Notification Service** | Member 8 | ⏳ *Planned Phase 2* | Multi-channel SMS/Email delivery via RabbitMQ listeners |
| **AI Assistant & Analytics** | Member 9 | ⏳ *Planned Phase 2* | Python FastAPI with Google Gemini for intelligent delivery forecasting |

---

## 5. Security & Authentication Architecture

### Dual-Token Lifecycle
To protect against both Cross-Site Scripting (XSS) and Cross-Site Request Forgery (CSRF):
1. **Short-Lived Access Token (JWT)**:
   - Validity: 15 minutes.
   - Stored in memory on the frontend (never stored in `localStorage`).
   - Attached via standard `Authorization: Bearer <token>` header.
2. **Long-Lived Refresh Token**:
   - Validity: 7 days.
   - Stored in the PostgreSQL `refresh_sessions` table with SHA-256 hash.
   - Delivered to the browser via an **HttpOnly**, **SameSite=Lax** secure cookie (`swiftdrop_refresh_token`).
   - Automatically rotated upon refresh; revoked immediately upon logout.

### Google OAuth 2.0 Integration
- Configured with Spring Security OAuth2 Client authorization code flow.
- Configurable through environment variables `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`.
- When valid credentials are provided, user profiles are authenticated with Google, accounts are linked, and the user is redirected to the frontend dashboard.
- If external credentials are not yet supplied, the system gracefully handles the callback and presents clear configuration guidance.

### Role-Based Access Control (RBAC)
- `ROLE_CUSTOMER`: Access to `/customer/dashboard` for individual bookings and tracking.
- `ROLE_MERCHANT`: Access to `/merchant/dashboard`. New merchant signups are flagged as `PENDING` until an administrator approves them.
- `ROLE_COURIER`: Access to `/courier/dashboard` for assigned delivery manifests and route availability.
- `ROLE_ADMIN`: Access to `/admin/dashboard` for user auditing and pending merchant approval/rejection.

---

## 6. Pre-Configured Test Credentials

For university grading and evaluation, the following accounts are initialized:

| Role | Email Address | Password | Intended Test Workflow |
|:---|:---|:---|:---|
| **Administrator** | `admin@swiftdrop.io` | `Admin@SwiftDrop2026!` | Access `/admin/dashboard`, review users, approve/reject merchants |
| **Customer** | `customer@swiftdrop.io` | `Customer@123!` | Access `/customer/dashboard`, test booking modal, track consignment |
| **Merchant (Approved)** | `merchant.approved@swiftdrop.io` | `Merchant@123!` | Access `/merchant/dashboard`, view verified status & live API key |
| **Merchant (Pending)** | `merchant.pending@swiftdrop.io` | `Merchant@123!` | Access `/merchant/dashboard`, verify PENDING compliance alert |
| **Courier** | `courier@swiftdrop.io` | `Courier@123!` | Access `/courier/dashboard`, toggle online/offline availability |

> [!TIP]
> The login page at `/login` includes quick-fill buttons allowing evaluators to populate any of these test credentials with a single click.

---

## 7. Automated Test Results & Verification

All automated tests across backend and frontend build pipelines passed:

### Backend Test Results (JUnit 5 & Mockito)
- **Identity Service**: `8 passed, 0 failed, 0 skipped`
  - `JwtServiceTest.testGenerateAndValidateToken`: PASSED
  - `JwtServiceTest.testInvalidTokenReturnsFalse`: PASSED
  - `JwtServiceTest.testHashTokenProducesConsistentSha256`: PASSED
  - `AuthServiceTest.testRegisterCustomerSuccess`: PASSED
  - `AuthServiceTest.testRegisterDuplicateEmailThrowsConflict`: PASSED
  - `AuthServiceTest.testRegisterPasswordMismatchThrowsException`: PASSED
  - `AuthServiceTest.testLoginSuccess`: PASSED
  - `AuthServiceTest.testLoginInvalidPasswordFails`: PASSED
- **API Gateway**: `1 passed, 0 failed, 0 skipped`
  - `GatewayApplicationTests.contextLoads`: PASSED

### Frontend Test Results (Vitest & React Testing Library)
- **Frontend Suites**: `2 test files passed, 6 tests passed, 0 failed`
  - `LoginPage.test.tsx` (heading & inputs, Google OAuth button, quick-fill buttons, password toggle): PASSED
  - `RegisterPage.test.tsx` (customer inputs, terms and conditions checkbox): PASSED
- **Production Bundle**: `vite build` completed successfully with zero TypeScript compilation errors.

---

## 8. Local Startup Instructions

### Option A: Using Docker Compose (Full Containerized Stack)
```bash
# 1. Clone repository
git clone https://github.com/swiftdrop-ead-team/swiftdrop-courier-management.git
cd swiftdrop-courier-management

# 2. Copy environment template
cp .env.example .env

# 3. Build and launch all services
docker compose up --build -d
```
The services will be available at:
- **Frontend UI**: `http://localhost:3000`
- **API Gateway**: `http://localhost:8080`
- **Identity Service**: `http://localhost:8081`
- **PostgreSQL Database**: `localhost:5432`

### Option B: Running Locally with Maven & Node.js
1. **Start PostgreSQL**:
   ```bash
   docker compose up -d postgres
   ```
2. **Start Identity Service**:
   ```bash
   cd services/identity-service
   mvn spring-boot:run
   ```
3. **Start API Gateway**:
   ```bash
   cd gateway
   mvn spring-boot:run
   ```
4. **Start React Frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

---

## 9. Monorepo Git Strategy & GitHub Collaboration

- **Main Repository**: `https://github.com/swiftdrop-ead-team/swiftdrop-courier-management`
- **Branch Protection**: All development occurs on isolated feature branches (`feature/frontend`, `feature/identity-service`, `feature/gateway-devops`, etc.).
- **Code Reviews**: Every contribution requires a Pull Request with passing GitHub Actions CI workflows and peer review approval.
