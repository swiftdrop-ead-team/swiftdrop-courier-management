# SwiftDrop — Dispatch & Workload Management API Contract (v1)

**Service**: Dispatch Microservice (Member 5)  
**Base URL**: `http://localhost:8080/api/v1/dispatch` (via API Gateway) or `http://localhost:8084/api/v1/dispatch` (Direct Service)  
**Database**: `swiftdrop_dispatch` (PostgreSQL)  
**Message Broker**: RabbitMQ (`dispatch.exchange`)  
**Interactive Swagger UI**: `http://localhost:8084/swagger-ui.html`

---

## 1. Overview & Architecture Role
The Dispatch Microservice is responsible for:
1. Courier fleet registration, status monitoring, and operational availability.
2. Intelligent workload balancing preventing courier delivery overload.
3. Parcel assignment lifecycle (`ASSIGNED` ➔ `PICKED_UP` ➔ `IN_TRANSIT` ➔ `DELIVERED` / `FAILED`).
4. Proof of Delivery (POD) record capture (recipient name, signature, photo reference, notes).
5. Asynchronous event publication via RabbitMQ (`CourierAssignedEvent`, `DeliveryStatusUpdatedEvent`).

---

## 2. Courier Management Endpoints

### 2.1 Get All Couriers
- **Endpoint**: `GET /api/v1/dispatch/couriers`
- **Access**: Dispatcher / Admin
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": "courier-uom-001",
      "fullName": "Kasun Perera",
      "phoneNumber": "+94771234567",
      "vehicleType": "MOTORBIKE",
      "licensePlate": "WP BDF-1029",
      "assignedZone": "COLOMBO_CENTRAL",
      "status": "AVAILABLE",
      "maxActiveDeliveries": 8,
      "currentActiveDeliveries": 2,
      "availableForAssignment": true
    }
  ],
  "timestamp": "2026-11-03T20:50:00"
}
```

---

### 2.2 Get Available Couriers (With Capacity)
- **Endpoint**: `GET /api/v1/dispatch/couriers/available`
- **Access**: Dispatcher / Automated Dispatch Engine
- **Description**: Returns only couriers who are `AVAILABLE` and whose `currentActiveDeliveries < maxActiveDeliveries`, sorted by lowest load first.
- **Response**: `200 OK`

---

### 2.3 Get Courier Workloads & Utilization Metrics
- **Endpoint**: `GET /api/v1/dispatch/couriers/workloads`
- **Access**: Dispatcher / Admin Dashboard
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "courierId": 1,
      "courierName": "Kasun Perera",
      "phoneNumber": "+94771234567",
      "vehicleType": "MOTORBIKE",
      "assignedZone": "COLOMBO_CENTRAL",
      "status": "AVAILABLE",
      "activeDeliveries": 2,
      "maxDeliveries": 8,
      "utilizationPercentage": 25.0,
      "canAcceptMore": true
    }
  ],
  "timestamp": "2026-11-03T20:50:00"
}
```

---

### 2.4 Register New Courier
- **Endpoint**: `POST /api/v1/dispatch/couriers`
- **Access**: Admin / Dispatch Manager
- **Request Body**:
```json
{
  "userId": "courier-uom-006",
  "fullName": "Saman Kumara",
  "phoneNumber": "+94779988776",
  "vehicleType": "MOTORBIKE",
  "licensePlate": "WP BEF-2026",
  "assignedZone": "COLOMBO_CENTRAL",
  "maxActiveDeliveries": 5
}
```
- **Response**: `201 Created`

---

### 2.5 Update Courier Status
- **Endpoint**: `PATCH /api/v1/dispatch/couriers/{id}/status`
- **Access**: Courier / Dispatcher
- **Request Body**:
```json
{
  "status": "OFFLINE"
}
```
- **Response**: `200 OK`

---

## 3. Dispatch Assignment & Lifecycle Endpoints

### 3.1 Assign Parcel to Courier
- **Endpoint**: `POST /api/v1/dispatch/assignments`
- **Access**: Dispatcher
- **Request Body**:
```json
{
  "parcelId": "b18f3d64-4e2a-4a2e-8c31-92b45e998711",
  "courierId": 1,
  "trackingNumber": "SWIFT-COL-7890",
  "pickupAddress": "SwiftDrop Colombo Logistics Hub, Colombo 03",
  "deliveryAddress": "45 Galle Road, Dehiwala",
  "recipientName": "Nimal Fernando",
  "recipientPhone": "+94712345678",
  "notes": "Handle with care - fragile electronics"
}
```
- **Validation Rules**:
  - If Courier is `OFFLINE` ➔ Returns `400 Bad Request` (`CourierUnavailableException`).
  - If Courier reached max capacity ➔ Returns `409 Conflict` (`CourierOverloadException`).
  - If Parcel already actively assigned ➔ Returns `400 Bad Request` (`InvalidAssignmentException`).
- **Response**: `201 Created`
- **Side Effect**: Emits `CourierAssignedEvent` to RabbitMQ (`dispatch.exchange`).

---

### 3.2 Update Assignment Delivery Status
- **Endpoint**: `PATCH /api/v1/dispatch/assignments/{id}/status`
- **Access**: Courier
- **Request Body**:
```json
{
  "status": "IN_TRANSIT",
  "notes": "Collected package from hub, heading to destination"
}
```
- **Failure Status Example**:
```json
{
  "status": "FAILED",
  "failureReason": "Recipient unreachable after 3 call attempts and door locked"
}
```
- **Response**: `200 OK`
- **Side Effect**: Emits `DeliveryStatusUpdatedEvent` to RabbitMQ. If terminal (`DELIVERED`, `FAILED`, `CANCELLED`), decrements courier's active load.

---

### 3.3 Submit Proof of Delivery (POD)
- **Endpoint**: `POST /api/v1/dispatch/assignments/{id}/proof`
- **Access**: Courier
- **Request Body**:
```json
{
  "recipientName": "Nimal Fernando",
  "recipientNic": "199212345678",
  "signatureUrl": "https://s3.swiftdrop.lk/pod/sig-7890.png",
  "photoUrl": "https://s3.swiftdrop.lk/pod/photo-7890.jpg",
  "deliveryNotes": "Handed directly to recipient at residence"
}
```
- **Response**: `201 Created`
```json
{
  "success": true,
  "message": "Proof of delivery submitted successfully",
  "data": {
    "id": 1,
    "assignmentId": 100,
    "recipientName": "Nimal Fernando",
    "recipientNic": "199212345678",
    "signatureUrl": "https://s3.swiftdrop.lk/pod/sig-7890.png",
    "photoUrl": "https://s3.swiftdrop.lk/pod/photo-7890.jpg",
    "deliveryNotes": "Handed directly to recipient at residence",
    "deliveredAt": "2026-11-03T20:50:00"
  }
}
```

---

## 4. RabbitMQ Event Contracts

### 4.1 CourierAssignedEvent
- **Exchange**: `dispatch.exchange`
- **Routing Key**: `dispatch.courier.assigned`
- **Consumers**: Notification Service (dispatches SMS/Email to courier and sender).
```json
{
  "eventType": "COURIER_ASSIGNED",
  "assignmentId": 100,
  "parcelId": "b18f3d64-4e2a-4a2e-8c31-92b45e998711",
  "courierId": 1,
  "courierName": "Kasun Perera",
  "courierPhone": "+94771234567",
  "trackingNumber": "SWIFT-COL-7890",
  "pickupAddress": "SwiftDrop Colombo Logistics Hub, Colombo 03",
  "deliveryAddress": "45 Galle Road, Dehiwala",
  "recipientName": "Nimal Fernando",
  "recipientPhone": "+94712345678",
  "status": "ASSIGNED",
  "timestamp": "2026-11-03T20:50:00"
}
```

### 4.2 DeliveryStatusUpdatedEvent
- **Exchange**: `dispatch.exchange`
- **Routing Key**: `dispatch.status.updated`
- **Consumers**: Tracking Service (updates live cache), Merchant Service (fires webhooks), Notification Service.

