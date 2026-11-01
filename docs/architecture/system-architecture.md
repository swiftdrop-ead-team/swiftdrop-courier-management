# SwiftDrop — System Architecture Documentation

## 1. High-Level Architecture Overview

SwiftDrop is designed as an Enterprise Parcel Delivery & Courier Management System built upon a resilient microservices architecture. It decouples the core domain into discrete, independently deployable services communicating via synchronous REST protocols at the edge and asynchronous event messaging internally.

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

## 2. Authentication & Authorization Security Model

### Dual-Token Architecture
1. **Access Token (JWT)**:
   - Stateless HMAC-SHA256 signed token.
   - Lifetime: 15 minutes.
   - Claims: `sub` (User ID), `email`, `roles` (`ROLE_CUSTOMER`, `ROLE_MERCHANT`, `ROLE_COURIER`, `ROLE_ADMIN`), `iss` (`swiftdrop-identity`), `exp`.
   - Sent via standard `Authorization: Bearer <token>` header or decrypted from secure HttpOnly cookie.

2. **Refresh Token (Opaque UUID / SHA256 hashed)**:
   - Lifetime: 7 days.
   - Stored in `refresh_sessions` table with client IP, user agent, expiration, and revocation status.
   - Delivered to the browser inside a secure, HttpOnly, SameSite=Lax cookie:
     ```http
     Set-Cookie: swiftdrop_refresh_token=<token>; Path=/api/v1/auth; HttpOnly; SameSite=Lax; Max-Age=604800
     ```

### Google OAuth 2.0 Flow
- Flow: Standard OpenID Connect / OAuth 2.0 Authorization Code flow.
- Gateway proxies `/oauth2/authorization/google` and `/login/oauth2/code/google` to Identity Service.
- Upon authorization from Google, user details are extracted:
  - If email exists: linked to `oauth_identities` table.
  - If new user: account auto-provisioned with `ROLE_CUSTOMER`.
- Identity Service generates JWT access token and refresh cookie, then redirects to frontend:
  `http://localhost:3000/auth/callback?token=<jwt>`

### Role-Based Access Control (RBAC)
- **CUSTOMER**: Can book parcels, track personal shipments, view order history.
- **MERCHANT**: Can upload bulk shipments, access merchant dashboard, view API credentials (only when approval status is `APPROVED`).
- **COURIER**: Can view assigned deliveries, update parcel delivery progress, report delivery exceptions.
- **ADMIN**: Platform administration, user management, pending merchant approvals (`APPROVE` / `REJECT`).
