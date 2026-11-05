# SwiftDrop — Notification Service (Member 8)

> **Enterprise Application Development (EAD)**  
> **Assigned Member**: Member 8  
> **Git Feature Branch**: `feature/notification-service`  
> **Service Port**: `8087`  
> **Database**: `swiftdrop_notification` (PostgreSQL)  
> **Message Broker**: RabbitMQ (`dispatch.exchange`)  

---

## 1. Overview & Architecture

The **Notification Service** is responsible for delivering asynchronous, multi-channel notifications (HTML emails) across all 4 system roles (*Customer, Merchant, Courier, Administrator*).

It operates completely decoupled from order and dispatch lifecycles by listening to domain events emitted over **RabbitMQ**.

```
   [ Parcel / Dispatch Service ]
                 │
                 │ Asynchronous Event (dispatch.status.updated, dispatch.courier.assigned)
                 ▼
         [ RabbitMQ Broker ]
                 │
                 ▼
     [ Notification Service (Port 8087) ]
      ├── NotificationEventListener (@RabbitListener)
      ├── Idempotency & Deduplication Engine (Event ID + Milestone Check)
      ├── EmailTemplateService (Responsive Branded HTML)
      ├── EmailSenderService (SMTP + Mock Fallback)
      └── NotificationLogRepository (PostgreSQL: swiftdrop_notification)
```

---

## 2. Core Features & Rubric Alignment

1. **Observer Pattern / Event Consumption**:
   * Consumes `dispatch.status.updated` and `dispatch.courier.assigned` from RabbitMQ.
2. **Milestone Filtering (Spam Prevention)**:
   * Internal logistical hub scans (`IN_TRANSIT`, `PICKED_UP`) are filtered out to protect customer inboxes from spam.
   * Direct notifications are dispatched only for high-value actionable milestones:
     * `ORDER_CONFIRMED`
     * `COURIER_ASSIGNED`
     * `OUT_FOR_DELIVERY`
     * `DELIVERED`
     * `FAILED`
3. **Double-Layer Idempotency (Deduplication)**:
   * **Layer 1**: Discards duplicate incoming events by `eventId`.
   * **Layer 2**: Prevents duplicate milestone emails for the same parcel and recipient.
4. **Resilient Mail Delivery**:
   * Works with live SMTP (Gmail, Mailtrap, SendGrid).
   * Automatically falls back to a simulated mock logger in local evaluator environments so demos never crash due to missing credentials.
5. **Database Audit History**:
   * Tracks full delivery lifecycle (`SENT`, `FAILED`, `PENDING`), retry counts, and error details in PostgreSQL.

---

## 3. Design Patterns Applied

* **Observer Pattern**: Asynchronously consumes events emitted across the RabbitMQ message broker.
* **Strategy Pattern**: Decoupled notification sending strategy (`EmailSenderService`), easily extensible to SMS providers (Twilio).
* **Repository Pattern**: Spring Data JPA repository for clean data access and idempotency queries.
* **Envelope Pattern**: All REST API responses wrapped in standardized `ApiResponse<T>`.

---

## 4. REST Endpoints & API Contract

Base URL: `http://localhost:8087/api/v1/notifications`  
Swagger UI: `http://localhost:8087/swagger-ui.html`  
Actuator Health: `http://localhost:8087/actuator/health`  

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/notifications/history` | Retrieve all notification audit logs (optional `?parcelId=...` filter) |
| `GET` | `/api/v1/notifications/{id}` | Retrieve specific notification log by primary key ID |
| `POST` | `/api/v1/notifications/send` | Manually dispatch a notification for evaluator live demos |

---

## 5. Sample Payloads

### Trigger Manual Notification (POST `/api/v1/notifications/send`)
```json
{
  "parcelId": "PRC-2026-9901",
  "trackingNumber": "SWIFT-COL-9901",
  "recipientEmail": "customer@swiftdrop.io",
  "recipientName": "Alice Fernando",
  "notificationType": "OUT_FOR_DELIVERY",
  "customMessage": "Driver Dave is 10 minutes away."
}
```

### Successful Response (201 Created)
```json
{
  "success": true,
  "message": "Notification dispatched successfully",
  "data": {
    "id": 3,
    "parcelId": "PRC-2026-9901",
    "trackingNumber": "SWIFT-COL-9901",
    "recipientEmail": "customer@swiftdrop.io",
    "recipientName": "Alice Fernando",
    "notificationType": "OUT_FOR_DELIVERY",
    "channel": "EMAIL",
    "subject": "SwiftDrop Notification: OUT FOR DELIVERY - #SWIFT-COL-9901",
    "deliveryStatus": "SENT",
    "retryCount": 0,
    "createdAt": "2026-11-04T22:25:00",
    "sentAt": "2026-11-04T22:25:01"
  },
  "timestamp": "2026-11-04T22:25:01"
}
```

---

## 6. How to Run & Test

### Running with Docker Compose (Stack)
```bash
docker compose up --build notification-service
```

### Running Locally
```bash
# 1. Start dependencies
docker compose up -d postgres rabbitmq

# 2. Run service
cd services/notification-service
.\mvnw.cmd spring-boot:run
```

### Running Automated Tests
```bash
.\mvnw.cmd test
```
* Runs unit and event integration tests using an in-memory H2 database with zero external dependencies required.
