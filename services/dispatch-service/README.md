# Dispatch Service — SwiftDrop Logistics Platform

## Assigned Member: Member 5
**Branch**: `feature/dispatch-service`  
**Port**: `8084`  
**Database**: `swiftdrop_dispatch` (PostgreSQL 16)  
**Message Broker**: RabbitMQ (`dispatch.exchange`)  
**Technology Stack**: Java 21, Spring Boot 3.4.3, Spring Data JPA, Flyway, Springdoc OpenAPI, Docker  

---

## 1. Responsibilities
- Intelligent courier assignment based on vehicle capacity, proximity, and hub workload.
- Courier status and workload management (`AVAILABLE`, `ASSIGNED`, `DELIVERING`, `OFFLINE`).
- Active delivery balancing and capacity threshold validation to prevent courier overload.
- Proof of Delivery (POD) record capture (recipient name, signature, photo reference, notes).
- Event-driven notifications via RabbitMQ (`CourierAssignedEvent`, `DeliveryStatusUpdatedEvent`).

---

## 2. API Endpoints

### Couriers
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/dispatch/couriers` | Register a new courier |
| `GET` | `/api/v1/dispatch/couriers` | List all registered couriers |
| `GET` | `/api/v1/dispatch/couriers/available` | List couriers with remaining capacity |
| `GET` | `/api/v1/dispatch/couriers/workloads` | Workload analytics & capacity metrics |
| `GET` | `/api/v1/dispatch/couriers/{id}` | Get courier by ID |
| `PATCH` | `/api/v1/dispatch/couriers/{id}/status` | Update courier operational status |

### Dispatch Assignments & Proof of Delivery
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/dispatch/assignments` | Assign parcel to an available courier |
| `GET` | `/api/v1/dispatch/assignments` | List all dispatch assignments |
| `GET` | `/api/v1/dispatch/assignments/{id}` | Get assignment by ID |
| `GET` | `/api/v1/dispatch/assignments/tracking/{trackingNumber}` | Get assignment by tracking number |
| `GET` | `/api/v1/dispatch/assignments/courier/{courierId}` | Get all assignments for a courier |
| `GET` | `/api/v1/dispatch/assignments/courier/{courierId}/active` | Get active/in-progress assignments |
| `PATCH` | `/api/v1/dispatch/assignments/{id}/status` | Update delivery status (IN_TRANSIT, FAILED, etc.) |
| `POST` | `/api/v1/dispatch/assignments/{id}/proof` | Submit Proof of Delivery (POD) |
| `GET` | `/api/v1/dispatch/assignments/{id}/proof` | Get Proof of Delivery details |

---

## 3. Running & Testing Locally

### Run Unit and Integration Tests
```bash
./mvnw clean test
```

### Run Service
```bash
./mvnw spring-boot:run
```

### Interactive API Documentation (Swagger)
Once running, navigate to:
```
http://localhost:8084/swagger-ui.html
```
