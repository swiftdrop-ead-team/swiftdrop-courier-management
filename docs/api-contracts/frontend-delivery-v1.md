# Frontend delivery API handoff (v1)

These are **proposed contracts consumed by the completed frontend**, not a claim that the domain backends exist. Existing authentication and merchant approval endpoints remain unchanged. Domain owners should implement these contracts or adapt `frontend/src/features/delivery/service.ts` when their final schemas are agreed.

Base URL: `VITE_API_BASE_URL`, default `/api/v1`. Browser requests use the existing in-memory Bearer token, refresh-cookie interceptor, and a 15-second timeout. Public tracking uses separate anonymous requests without credentials. All dates/times exchanged with services must be ISO 8601; date-only pickup/assignment fields use `YYYY-MM-DD` in Asia/Colombo.

## Types

Authoritative frontend interfaces: `frontend/src/features/delivery/model.ts`.

```ts
type ParcelStatus = 'CREATED' | 'PICKED_UP' | 'IN_TRANSIT' |
  'OUT_FOR_DELIVERY' | 'DELIVERED' | 'FAILED' | 'CANCELLED';

interface Booking {
  senderName: string; senderPhone: string; pickupAddress: string;
  recipientName: string; recipientPhone: string; dropoffAddress: string;
  zoneId: string; description: string; weight: number;
  length: number; width: number; height: number; // kg and cm
  service: 'STANDARD' | 'EXPRESS'; pickupDate: string;
  instructions: string; cod: number; // LKR; separate from the delivery fee
}
interface Quote { id: string; amount: number; expiresAt: string }
interface HistoryEntry { status: ParcelStatus; timestamp: string; note: string }
interface ParcelRecord extends Booking {
  id: string; trackingNumber: string; ownerId: string;
  ownerRole: 'CUSTOMER' | 'MERCHANT'; status: ParcelStatus;
  amount: number; createdAt: string; courierId?: string; assignedDate?: string;
  recipientProof?: string; failureReason?: string; history: HistoryEntry[];
}
interface Courier { id: string; name: string; available: boolean; capacity: number }
interface Zone { id: string; name: string; base: number; perKg: number;
  expressMultiplier: number; active: boolean }
interface Profile { fullName: string; email: string; phone: string;
  businessName: string; address: string; emailNotifications: boolean }
interface SavedAddress { id: string; label: string; address: string }
interface Notice { id: string; ownerId: string; text: string;
  read: boolean; timestamp: string }
interface Integration { apiKey: string; hasKey?: boolean; clientId: string;
  webhookUrl: string; enabled: boolean }
interface WebhookAttempt { id: string; trackingNumber: string; status: ParcelStatus;
  timestamp: string; result: 'PENDING' | 'DELIVERED' | 'FAILED'; attempts: number }
```

## Endpoint contract

List responses are plain arrays. The current frontend requests the complete authorized set for client-side filtering, pagination, and aggregate reports. If server pagination is needed, agree its envelope and update the adapter/report APIs together. Errors use the existing `{ message, validationErrors? }` shape with meaningful HTTP status codes.

| Method and path | Request | Response | Authorized actor |
| --- | --- | --- | --- |
| GET `/parcels?scope=mine` | — | `ParcelRecord[]` owned by caller | Customer/approved merchant |
| GET `/parcels?scope=assigned` | — | `ParcelRecord[]` assigned to caller, including history | Courier |
| GET `/parcels?scope=all` | — | `ParcelRecord[]` | Admin |
| POST `/pricing/quotes` | `Booking` | `Quote` | Customer/approved merchant; merchant API key |
| POST `/parcels` | `Booking & {quoteId}` | `ParcelRecord` | Customer/approved merchant; merchant API key |
| PATCH `/parcels/{id}/cancel` | — | 200/204 | Owner/admin, only before assignment/pickup |
| PATCH `/parcels/{id}/status` | `{status, note, recipientName}` | 200/204 | Assigned courier |
| GET `/dispatch/couriers` | — | `Courier[]`; courier may receive only self | Admin/courier |
| PATCH `/dispatch/couriers/me/availability` | `{available:boolean}` | 200/204 | Courier |
| POST `/dispatch/assignments` | `{parcelId,courierId,assignedDate}` | 200/204 | Admin |
| GET `/pricing/zones` | — | `Zone[]` | Authenticated actors |
| PUT `/pricing/zones/{id}` | `Zone` | 200/204 | Admin |
| GET `/notifications` | — | `Notice[]` owned by caller | Customer/merchant/courier |
| PATCH `/notifications/{id}/read` | — | 200/204 | Notification owner |
| GET `/merchants/me/integration` | — | `Integration` | Approved merchant |
| POST `/merchants/me/integration/rotate` | — | `Integration` containing the new secret in `apiKey` | Approved merchant |
| PUT `/merchants/me/webhook` | `{webhookUrl,enabled}` | 200/204 | Approved merchant |
| GET `/merchants/me/webhook/attempts` | — | `WebhookAttempt[]` | Approved merchant |
| POST `/merchants/me/webhook/attempts/{id}/retry` | — | 200/204 | Attempt owner |
| GET `/auth/me/profile` | — | `Profile` | Authenticated actor |
| PUT `/auth/me/profile` | `Profile` | 200/204 | Profile owner |
| GET `/auth/me/addresses` | — | `SavedAddress[]` | Customer |
| PUT `/auth/me/addresses` | `{addresses:SavedAddress[]}` | 200/204 | Customer |
| PATCH `/admin/users/{id}` | `{roles:string[],status:'ACTIVE'|'INACTIVE'|'SUSPENDED'}` | 200/204 | Admin |
| POST `/pricing/quotes/bulk` | `{orders:Booking[]}` | `Quote[]`, matching input order | Approved merchant |
| POST `/parcels/bulk` | `{orders:(Booking & {quoteId})[]}` | 200/201 | Approved merchant |

`POST /parcels` sends `Idempotency-Key: <quoteId>`. Bulk create sends a stable UUID idempotency key for retries of the same selected batch. The server must atomically consume quotes and prevent duplicate bookings, including after a timeout. Bulk creation must be all-or-nothing and return row-indexed validation errors on failure. Limit batches to 100 orders and file uploads to 1 MB in the UI; enforce server limits independently.

Quote amounts are authoritative and bound to the complete booking payload. Reject changed/expired quotes. The preview formula is `max(actual kg, length*width*height/5000) * perKg + base`, multiplied by the express multiplier when selected. Coordinate this formula with the pricing owner; previews are illustrative, not production pricing authority.

The status endpoint must validate legal transitions and require recipient proof for delivery, a reason for failure, and appropriate COD handover handling. Courier identity comes from authenticated claims, not a client-supplied ID. Assignment must atomically verify availability/capacity and avoid double assignment. Owners/roles/scope must be enforced by the server even when a caller changes query parameters.

User administration must prevent self-lockout and removal of the last active administrator. Updating an email may require a verification flow in the identity service. API-key operations must require merchant approval. Store key hashes server-side; GET integration should return `apiKey: ''` and `hasKey: true` for an existing key. Only key generation returns the raw key, which the frontend holds in component memory for that view.

## Public tracking and events

GET `/tracking/{trackingNumber}` and SSE GET `/tracking/{trackingNumber}/events` are anonymous and must return only:

```json
{
  "trackingNumber": "SD-EXAMPLE-1001",
  "status": "IN_TRANSIT",
  "updatedAt": "2026-11-13T08:00:00Z",
  "history": [
    {"status":"CREATED","timestamp":"2026-11-13T04:00:00Z"},
    {"status":"PICKED_UP","timestamp":"2026-11-13T06:00:00Z"},
    {"status":"IN_TRANSIT","timestamp":"2026-11-13T08:00:00Z"}
  ]
}
```

Use default SSE `message` events (`data: <JSON payload>\n\n`), `Content-Type: text/event-stream`, and periodic heartbeat comments. The UI attempts SSE and independently polls every 10 seconds for recovery; terminal states stop further polling. No names, phone numbers, addresses, proof, or private notes are rendered on public tracking. Return 404 for unknown numbers. Configure allowed origins if the API is hosted separately. Gateway must preserve streaming; frontend Nginx disables API proxy buffering.

After a status transition, backend services publish the event, update tracking, persist notifications, send email/mock SMS, and enqueue merchant webhooks with signatures and retry/backoff. The UI displays the resulting records; **it does not replace the broker, notification consumer, or webhook worker**. HTTPS URL validation in the UI does not replace server-side SSRF validation.

## Integration checklist

1. Confirm DTOs, paths, status names, and response envelopes against the types above.
2. Implement server authorization/validation, not just frontend guards.
3. Return ISO timestamps and use the authenticated subject for resource ownership.
4. Test expired quotes, duplicate create retries, capacity conflicts, webhook failure/retry, and another courier's parcel.
5. Verify the complete live workflow with preview mode disabled. Existing domain services are placeholders in this checkout.
