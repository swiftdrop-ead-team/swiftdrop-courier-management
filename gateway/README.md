# API Gateway

## Assigned Member: Member 10
**Branch**: `feature/gateway-devops`  
**Port**: `8080`  
**Technology Stack**: Java 21, Spring Boot 3.4.3, Spring Cloud Gateway (2024.0.0), Spring WebFlux

---

## 1. Responsibilities
- Edge reverse proxy routing and traffic distribution for all microservices.
- Request correlation ID stamping (`X-Correlation-Id`) across the distributed system.
- Global Cross-Origin Resource Sharing (CORS) enforcement for React frontend (`http://localhost:3000`).
- Global security headers filter (`X-Content-Type-Options`, `X-Frame-Options`, `X-XSS-Protection`).
- Actuator health check aggregation (`/actuator/health`).

---

## 2. Route Mappings

| Service | Route Pattern | Target URI | Status |
|:---|:---|:---|:---|
| **Identity Service (Auth)** | `/api/v1/auth/**` | `http://identity-service:8081` | Active |
| **Identity Service (Admin)** | `/api/v1/admin/**` | `http://identity-service:8081` | Active |
| **Google OAuth2 Flow** | `/oauth2/**`, `/login/oauth2/**` | `http://identity-service:8081` | Active |
| **Parcel Service** | `/api/v1/parcels/**` | `http://parcel-service:8082` | Reserved |
| **Merchant Service** | `/api/v1/merchants/**` | `http://merchant-service:8083` | Reserved |
| **Dispatch Service** | `/api/v1/dispatch/**` | `http://dispatch-service:8084` | Reserved |
| **Pricing Service** | `/api/v1/pricing/**` | `http://pricing-service:8085` | Reserved |
| **Tracking Service** | `/api/v1/tracking/**` | `http://tracking-service:8086` | Reserved |
| **Notification Service** | `/api/v1/notifications/**` | `http://notification-service:8087` | Reserved |
| **AI Assistant Service** | `/api/v1/ai/**` | `http://ai-assistant-service:8088` | Reserved |

---

## 3. Local Execution & Testing

```bash
mvn test
mvn spring-boot:run
```
