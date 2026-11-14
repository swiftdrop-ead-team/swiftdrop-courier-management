# SwiftDrop — Parcel Delivery & Order Intake Microservice (Member 3)

[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)]()
[![Database](https://img.shields.io/badge/Database-PostgreSQL%2016-blue.svg)]()
[![Messaging](https://img.shields.io/badge/Messaging-RabbitMQ-orange.svg)]()
[![Port](https://img.shields.io/badge/Port-8082-blueviolet.svg)]()

## 1. Executive Service Overview

The **Parcel Service** is the central order intake and consignment management engine for the **SwiftDrop** courier logistics platform. Assigned to **Member 3**, it handles parcel registration, address and volumetric dimension validation, unique tracking number generation (`SD-XXXX-XXXX-XX`), state machine lifecycle transitions, audit history tracking, and asynchronous domain event publishing across the RabbitMQ bus.

---

## 2. Technical Architecture & Technology Stack

- **Runtime & Language**: Java 21 LTS (with Java 17 compile compatibility).
- **Framework**: Spring Boot 3.4.3 (Spring Web, Spring Security, Spring Data JPA).
- **Persistence**: PostgreSQL 16 (Logical DB: `swiftdrop_parcel`), Flyway Migration Engine.
- **Messaging**: RabbitMQ Topic Exchange (`swiftdrop.parcel.exchange`).
- **Security**: JJWT (`0.12.6`), HMAC-SHA256 Stateless Token Verification, Role-Based Access Control (RBAC).
- **Testing**: JUnit 5, Mockito, Spring Boot Test, H2 In-Memory Database (PostgreSQL dialect mode).

---

## 3. Database Architecture & Schema Design

Managed automatically via Flyway migration scripts (`src/main/resources/db/migration/V1__init_parcel_schema.sql`).

### A. `parcels` Table
| Column | Type | Constraints / Description |
|:---|:---|:---|
| `id` | `UUID` | Primary Key, Auto-generated |
| `tracking_number` | `VARCHAR(30)` | Unique, Indexed (`SD-XXXX-XXXX-XX`) |
| `sender_id` | `UUID` | Indexed, Owner UUID |
| `sender_type` | `VARCHAR(20)` | `CUSTOMER` or `MERCHANT` |
| `sender_name` | `VARCHAR(150)` | Full name |
| `sender_phone` | `VARCHAR(30)` | Contact number |
| `sender_email` | `VARCHAR(150)` | Email address |
| `pickup_address` | `TEXT` | Street address |
| `pickup_city` | `VARCHAR(100)` | Origin city |
| `pickup_postal_code` | `VARCHAR(20)` | Origin postal code |
| `recipient_name` | `VARCHAR(150)` | Recipient name |
| `recipient_phone` | `VARCHAR(30)` | Recipient contact |
| `recipient_email` | `VARCHAR(150)` | Recipient email |
| `delivery_address` | `TEXT` | Destination street address |
| `delivery_city` | `VARCHAR(100)` | Destination city |
| `delivery_postal_code` | `VARCHAR(20)` | Destination postal code |
| `weight_kg` | `NUMERIC(6, 2)` | Actual physical weight |
| `length_cm`, `width_cm`, `height_cm` | `NUMERIC(6, 2)` | Volumetric dimensions |
| `description` | `VARCHAR(500)` | Cargo description |
| `is_fragile` | `BOOLEAN` | Fragile handling flag |
| `delivery_type` | `VARCHAR(30)` | `STANDARD`, `EXPRESS`, `SAME_DAY` |
| `status` | `VARCHAR(30)` | Current lifecycle state |
| `delivery_fee` | `NUMERIC(10, 2)` | Calculated delivery charge |
| `currency` | `VARCHAR(10)` | Default: `USD` |
| `assigned_courier_id` | `UUID` | Assigned courier (Indexed) |
| `failure_reason` | `VARCHAR(255)` | Reason note on failed drop |
| `special_instructions` | `TEXT` | Delivery notes |
| `created_at`, `updated_at` | `TIMESTAMP WITH TZ` | Audit timestamps |

### B. `parcel_status_history` Table
| Column | Type | Description |
|:---|:---|:---|
| `id` | `UUID` | Primary Key |
| `parcel_id` | `UUID` | Foreign Key referencing `parcels(id)` (CASCADE DELETE) |
| `previous_status` | `VARCHAR(30)` | Previous lifecycle state |
| `new_status` | `VARCHAR(30)` | New lifecycle state |
| `changed_by_user_id` | `UUID` | Operator / Actor UUID |
| `actor_role` | `VARCHAR(50)` | Role (`ROLE_CUSTOMER`, `ROLE_COURIER`, `ROLE_ADMIN`) |
| `location` | `VARCHAR(200)` | Physical checkpoint or hub |
| `notes` | `TEXT` | Status transition or proof notes |
| `timestamp` | `TIMESTAMP WITH TZ` | Immutable transition timestamp |

---

## 4. Consignment Lifecycle State Machine

Transitions between states are strictly enforced by `ParcelStatus.canTransitionTo()`:

```
[CREATED] ──> [PICKED_UP] ──> [RECEIVED_AT_HUB] ──> [SORTED] ──> [IN_TRANSIT] ──> [OUT_FOR_DELIVERY] ──> [DELIVERED]
    │              │                  │                                │                   │
    └──[CANCELLED] └───[CANCELLED]────┴────[CANCELLED]                 └───[FAILED] <──────┘
                                                                               │
                                                                               └── (Re-attempt / Return to Hub)
```

- **Cancellation Rules**: A parcel can only be cancelled while in `CREATED`, `PICKED_UP`, or `RECEIVED_AT_HUB` states. Once `IN_TRANSIT`, cancellation is rejected.
- **Terminal States**: `DELIVERED` and `CANCELLED` are immutable terminal states.
- **Role Enforcement**:
  - `CUSTOMER` / `MERCHANT`: May book, retrieve own parcels, and cancel eligible consignments.
  - `COURIER`: May update execution statuses (`PICKED_UP`, `IN_TRANSIT`, `OUT_FOR_DELIVERY`, `DELIVERED`, `FAILED`).
  - `ADMIN`: Full authority across all lifecycle transitions and system records.

---

## 5. REST API Specifications

Base URL: `http://localhost:8080/api/v1/parcels` (via Gateway) or `http://localhost:8082/api/v1/parcels` (Direct)

| Method | Endpoint | Access Role | Description |
|:---|:---|:---|:---|
| `POST` | `/api/v1/parcels` | `CUSTOMER`, `MERCHANT`, `ADMIN` | Register a new consignment. Returns `201 Created`. |
| `GET` | `/api/v1/parcels/tracking/{trackingNumber}` | **Public** | Public tracking checkpoint inquiry. Sanitizes private PII. |
| `GET` | `/api/v1/parcels/secure-tracking/{trackingNumber}` | Authenticated | Full tracking details including addresses and customer info. |
| `GET` | `/api/v1/parcels/{id}` | Authenticated | Retrieve parcel by UUID (Owner / Courier / Admin). |
| `GET` | `/api/v1/parcels/my-parcels` | `CUSTOMER`, `MERCHANT`, `ADMIN` | Retrieve paginated shipment history of authenticated user. |
| `GET` | `/api/v1/parcels` | `ADMIN` | Administrative query to view all platform parcels with filters. |
| `GET` | `/api/v1/parcels/sender/{senderId}` | Authenticated | Retrieve shipments for a specific sender ID. |
| `GET` | `/api/v1/parcels/courier/{courierId}` | `COURIER`, `ADMIN` | Retrieve shipments assigned to a specific courier. |
| `PATCH` | `/api/v1/parcels/{id}` | Owner / `ADMIN` | Update parcel details while status is `CREATED`. |
| `PATCH` | `/api/v1/parcels/{id}/status` | `COURIER`, `ADMIN` | Update delivery lifecycle status. |
| `POST` | `/api/v1/parcels/{id}/cancel` | Owner / `ADMIN` | Cancel a parcel booking. |

---

## 6. RabbitMQ Messaging Architecture

- **Exchange**: `swiftdrop.parcel.exchange` (`TopicExchange`, durable)
- **Routing Keys**:
  - `parcel.created`: Emitted when a new consignment is persisted.
  - `parcel.status_updated`: Emitted on every lifecycle transition for Tracking and Notification services.
  - `parcel.cancelled`: Emitted upon parcel cancellation.
- **Queues**:
  - `swiftdrop.tracking.parcel-updates` (bound via `parcel.#`)
  - `swiftdrop.notification.parcel-updates` (bound via `parcel.#`)

---

## 7. Environment Variables Reference

| Variable | Default Value | Description |
|:---|:---|:---|
| `PARCEL_SERVICE_PORT` | `8082` | HTTP Server port |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `POSTGRES_DB` | `swiftdrop_parcel` | PostgreSQL logical database |
| `POSTGRES_USER` | `swiftdrop_user` | PostgreSQL username |
| `POSTGRES_PASSWORD` | `swiftdrop_secure_pass_2026!` | PostgreSQL password |
| `JWT_SECRET` | `404E635266...` | HMAC-SHA256 shared secret |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ broker host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ broker port |
| `PRICING_SERVICE_URL` | `http://localhost:8085` | Member 6 Pricing Service URL |

---

## 8. Local Build & Test Instructions

### Running Tests Locally
```bash
cd services/parcel-service
./mvnw clean test
```

### Running Locally with PostgreSQL & RabbitMQ
```bash
# 1. Start database and message broker from repo root
docker compose up -d postgres

# 2. Start Parcel Service
cd services/parcel-service
./mvnw spring-boot:run
```

---

## 9. Proposed `docker-compose.yml` Registration

To integrate Parcel Service into the root `docker-compose.yml`, the team coordinator may add the following service definition:

```yaml
  # ==============================================================================
  # Parcel Microservice — Spring Boot 3.4.x / Java 21 (Port 8082)
  # ==============================================================================
  parcel-service:
    build:
      context: ./services/parcel-service
      dockerfile: Dockerfile
    container_name: swiftdrop-parcel-service
    restart: unless-stopped
    ports:
      - "${PARCEL_SERVICE_PORT:-8082}:8082"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/swiftdrop_parcel
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-swiftdrop_user}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-swiftdrop_secure_pass_2026!}
      SWIFTDROP_SECURITY_JWT_SECRET: ${JWT_SECRET:-404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      PRICING_SERVICE_URL: http://pricing-service:8085
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8082/actuator/health || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 25s
    networks:
      - swiftdrop-net
```
