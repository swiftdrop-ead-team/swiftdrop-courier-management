# Identity & Access Management Microservice

## Assigned Member: Member 2
**Branch**: `feature/identity-service`  
**Port**: `8081`  
**Database**: `swiftdrop_identity` (PostgreSQL)  
**Technology Stack**: Java 21, Spring Boot 3.4.3, Spring Security 6.x, Spring Data JPA, Flyway, JJWT (io.jsonwebtoken 0.12.6), OAuth2 Client (Google)

---

## 1. Responsibilities
- User registration (`CUSTOMER` and `MERCHANT`).
- Secure credential authentication and BCrypt password hashing.
- Google OAuth 2.0 authorization code flow integration.
- Dual-token session security (15-min JWT access token + 7-day HttpOnly refresh cookie with database rotation).
- Role-based access control (`ROLE_CUSTOMER`, `ROLE_MERCHANT`, `ROLE_COURIER`, `ROLE_ADMIN`).
- Merchant registration approval workflows (`PENDING`, `APPROVED`, `REJECTED`).
- Password reset issuance and verification with single-use expiring tokens.
- Brute-force login rate limiting.

---

## 2. API Endpoints

| Method | Endpoint | Access | Description |
|:---|:---|:---|:---|
| `POST` | `/api/v1/auth/register` | Public | Register new individual retail customer |
| `POST` | `/api/v1/auth/merchant/register` | Public | Register business merchant (sets `PENDING` approval) |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user, issue JWT & set HttpOnly refresh cookie |
| `POST` | `/api/v1/auth/refresh` | Public | Rotate refresh token and issue fresh JWT |
| `POST` | `/api/v1/auth/logout` | Authenticated | Revoke refresh session and clear cookie |
| `GET` | `/api/v1/auth/me` | Authenticated | Retrieve authenticated user profile and roles |
| `POST` | `/api/v1/auth/forgot-password` | Public | Request single-use password reset token |
| `POST` | `/api/v1/auth/reset-password` | Public | Complete password reset using token |
| `GET` | `/api/v1/admin/users` | `ROLE_ADMIN` | List all registered users |
| `GET` | `/api/v1/admin/merchants/pending` | `ROLE_ADMIN` | List merchants awaiting approval |
| `PATCH` | `/api/v1/admin/merchants/{id}/approve` | `ROLE_ADMIN` | Approve merchant registration |
| `PATCH` | `/api/v1/admin/merchants/{id}/reject` | `ROLE_ADMIN` | Reject merchant registration |

---

## 3. Seed Accounts for Local Testing

| Email | Password | Role | Notes |
|:---|:---|:---|:---|
| `admin@swiftdrop.io` | `Admin@SwiftDrop2026!` | `ADMIN` | System administrator |
| `customer@swiftdrop.io` | `Customer@123!` | `CUSTOMER` | Retail customer |
| `merchant.approved@swiftdrop.io` | `Merchant@123!` | `MERCHANT` | Approved commercial merchant |
| `merchant.pending@swiftdrop.io` | `Merchant@123!` | `MERCHANT` | Pending commercial merchant |
| `courier@swiftdrop.io` | `Courier@123!` | `COURIER` | Field courier |

---

## 4. Local Execution & Testing

Run unit & security tests:
```bash
mvn test
```

Start service locally:
```bash
mvn spring-boot:run
```
