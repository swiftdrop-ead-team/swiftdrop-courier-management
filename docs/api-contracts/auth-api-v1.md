# SwiftDrop — Identity & Authentication API Contract (v1)

Base URL: `http://localhost:8080/api/v1` (via Gateway) or `http://localhost:8081/api/v1` (Direct)

---

## 1. Authentication Endpoints

### 1.1 Customer Registration
- **Endpoint**: `POST /api/v1/auth/register`
- **Access**: Public
- **Request Body**:
```json
{
  "fullName": "Jane Doe",
  "email": "jane.doe@example.com",
  "phoneNumber": "+1234567890",
  "password": "Password123!",
  "confirmPassword": "Password123!",
  "acceptTerms": true
}
```
- **Response**: `201 Created`
```json
{
  "success": true,
  "message": "Customer registered successfully.",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "c1f7b0e2-7634-4a25-95a2-3f14bb7e4811",
    "fullName": "Jane Doe",
    "email": "jane.doe@example.com",
    "roles": ["CUSTOMER"],
    "status": "ACTIVE"
  }
}
```

---

### 1.2 Merchant Registration
- **Endpoint**: `POST /api/v1/auth/merchant/register`
- **Access**: Public
- **Request Body**:
```json
{
  "businessName": "Apex Retail Solutions",
  "contactPerson": "Robert Smith",
  "email": "robert@apexretail.com",
  "businessAddress": "456 Market St, Suite 200, San Francisco, CA",
  "phoneNumber": "+1987654321",
  "password": "SecurePassword123!",
  "confirmPassword": "SecurePassword123!"
}
```
- **Response**: `201 Created`
```json
{
  "success": true,
  "message": "Merchant registration received. Account is pending administrator approval.",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "e4f3a1b2-1234-4b89-a654-7f14bb7e9900",
    "fullName": "Robert Smith",
    "email": "robert@apexretail.com",
    "roles": ["MERCHANT"],
    "merchantApprovalStatus": "PENDING"
  }
}
```

---

### 1.3 User Login
- **Endpoint**: `POST /api/v1/auth/login`
- **Access**: Public
- **Request Body**:
```json
{
  "email": "admin@swiftdrop.io",
  "password": "Admin@SwiftDrop2026!",
  "rememberMe": true
}
```
- **Response**: `200 OK`
- **Cookie**: `Set-Cookie: swiftdrop_refresh_token=...; HttpOnly; SameSite=Lax; Path=/api/v1/auth`
```json
{
  "success": true,
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "a0000000-0000-0000-0000-000000000001",
    "fullName": "System Administrator",
    "email": "admin@swiftdrop.io",
    "roles": ["ADMIN"],
    "status": "ACTIVE"
  }
}
```

---

### 1.4 Get Current User Profile
- **Endpoint**: `GET /api/v1/auth/me`
- **Access**: Authenticated (`CUSTOMER`, `MERCHANT`, `COURIER`, `ADMIN`)
- **Headers**: `Authorization: Bearer <token>` or valid cookie
- **Response**: `200 OK`
```json
{
  "id": "a0000000-0000-0000-0000-000000000001",
  "fullName": "System Administrator",
  "email": "admin@swiftdrop.io",
  "roles": ["ADMIN"],
  "phoneNumber": "+18005550199",
  "status": "ACTIVE",
  "createdAt": "2026-11-01T00:00:00Z"
}
```

---

### 1.5 Token Refresh
- **Endpoint**: `POST /api/v1/auth/refresh`
- **Access**: Public (presents refresh cookie or JSON body `{ "refreshToken": "..." }`)
- **Response**: `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900
}
```

---

### 1.6 User Logout
- **Endpoint**: `POST /api/v1/auth/logout`
- **Access**: Authenticated
- **Response**: `200 OK` (clears refresh cookie, revokes refresh session)
```json
{
  "success": true,
  "message": "Logged out successfully."
}
```

---

### 1.7 Password Reset Request
- **Endpoint**: `POST /api/v1/auth/forgot-password`
- **Access**: Public
- **Request Body**:
```json
{
  "email": "user@example.com"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "If this email exists in our system, a password reset link has been dispatched."
}
```

---

### 1.8 Password Reset Confirmation
- **Endpoint**: `POST /api/v1/auth/reset-password`
- **Access**: Public
- **Request Body**:
```json
{
  "token": "reset-uuid-token",
  "newPassword": "NewStrongPassword123!",
  "confirmPassword": "NewStrongPassword123!"
}
```
- **Response**: `200 OK`

---

## 2. Admin Management Endpoints

### 2.1 List All Users
- **Endpoint**: `GET /api/v1/admin/users`
- **Access**: `ROLE_ADMIN`
- **Response**: `200 OK`

### 2.2 List Pending Merchants
- **Endpoint**: `GET /api/v1/admin/merchants/pending`
- **Access**: `ROLE_ADMIN`
- **Response**: `200 OK`
```json
[
  {
    "id": "e4f3a1b2-1234-4b89-a654-7f14bb7e9900",
    "businessName": "Apex Retail Solutions",
    "contactPerson": "Robert Smith",
    "email": "robert@apexretail.com",
    "phoneNumber": "+1987654321",
    "businessAddress": "456 Market St, Suite 200, San Francisco, CA",
    "approvalStatus": "PENDING",
    "registeredAt": "2026-11-01T07:15:00Z"
  }
]
```

### 2.3 Approve Merchant
- **Endpoint**: `PATCH /api/v1/admin/merchants/{id}/approve`
- **Access**: `ROLE_ADMIN`
- **Request Body**:
```json
{
  "notes": "Verified business documentation and tax ID."
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Merchant has been approved successfully."
}
```

### 2.4 Reject Merchant
- **Endpoint**: `PATCH /api/v1/admin/merchants/{id}/reject`
- **Access**: `ROLE_ADMIN`
- **Request Body**:
```json
{
  "notes": "Invalid business registration number."
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Merchant registration rejected."
}
```
