# Frontend/backend delivery integration

The live adapter is `frontend/src/features/delivery/service.ts`. It consumes the actual backend DTOs; preview data never replaces failed live requests. All paths below are relative to `/api/v1` through the gateway.

| Workflow | Actual endpoint / behavior |
| --- | --- |
| Account profile | GET/PUT `/auth/profile`; email remains read-only |
| Saved addresses | GET/PUT `/auth/addresses` (at most 20 unique entries) |
| User access | PATCH `/admin/users/{id}` with short role names and status; self-demotion/deactivation blocked |
| Pricing | POST `/pricing/calculate`; admin GET `/pricing/rules`, PUT `/pricing/rules/{tier}` |
| Customer/merchant deliveries | GET `/parcels/my-parcels?page=N&size=100`; collect all pages |
| Admin deliveries | GET `/parcels`; join dispatch assignments for workload/state |
| Fee estimate | POST `/parcels/estimate` using `CreateParcelRequest`; returns `{id,amount,currency,expiresAt}` |
| Confirm booking | POST `/parcels` with the same payload and `estimateId`; estimate is owner/payload-bound, valid five minutes, and single-use |
| Cancel booking | POST `/parcels/{id}/cancel` with `{reason}` |
| Courier identity | GET `/dispatch/couriers/user/{identityUserId}`; maps identity UUID to numeric dispatch ID |
| Assigned deliveries | GET `/dispatch/assignments/courier/{dispatchId}` plus authorized parcel detail requests |
| Availability | PATCH `/dispatch/couriers/{dispatchId}/status` with AVAILABLE/OFFLINE |
| Register courier | POST `/dispatch/couriers` from the admin dispatch screen using an existing courier identity account |
| Assign | POST `/dispatch/assignments`; includes numeric courier ID, parcel/tracking IDs, contact addresses, and booking-owner notification email |
| Courier progress | PATCH `/dispatch/assignments/{id}/status` with `{status,notes,failureReason}` |
| Proof / delivered | POST `/dispatch/assignments/{id}/proof` with `{recipientName,deliveryNotes}` |
| Notification logs | GET `/notifications/history`; backend filters to the authenticated account email (admins can audit all) |
| Read state | PATCH `/notifications/{id}/read`; owner/admin only |
| Email preferences | GET/PUT `/notifications/preferences` with `{enabled}`; keyed to the authenticated email |
| Public tracking | GET `/tracking/{number}`; fallback to `/parcels/tracking/{number}` before dispatch has created a tracking read model |
| Public SSE | GET `/tracking/{number}/stream`; listen for `snapshot` and `status` events |

## Mapping and integration details

Parcel backend names such as `deliveryAddress`, `weightKg`, `deliveryFee`, `senderId`, and `statusHistory` are mapped to the existing dashboard fields. Dispatch and notification services return `{success,data,message}` envelopes. Parcel list endpoints return `{content,totalPages,...}` pages. Dispatch timestamps have no offset; the adapter interprets them in Asia/Colombo. New estimates and parcels use LKR. Historical parcel currency is preserved in estimates/receipts.

Live booking accepts a requested pickupDate (YYYY-MM-DD, today or later in Asia/Colombo) and codAmount (0?100000 LKR, up to two decimal places). Both fields persist in the parcel service and are returned by authenticated parcel endpoints. Existing requests may omit them for same-day pickup and zero COD. COD reporting reflects parcel amounts, not settlements or confirmed payouts. The estimate endpoint calls the pricing service using the caller's signed token. Estimates are persisted in PostgreSQL with an owner and payload fingerprint. Confirmation locks and consumes the estimate in the same transaction as booking creation; failed transactions do not consume it. Pricing unavailability produces a service error rather than a fabricated fallback fee.

Dispatch events now include the courier's identity UUID. The parcel service consumes assignment/status events from `dispatch.exchange`, persists courier ownership and status history, and ignores duplicate/stale timestamps. Notification status events include the booking owner's email and a generated event ID. Tracking also consumes parcel booking/cancellation events; dispatch consumes cancellations to release courier workload. RabbitMQ must be running for the cross-service synchronization to occur. Durable outbox delivery remains future backend work.

Tracking DTOs use `currentStatus`, `lastUpdatedAt`, and `timeline`. These are normalized to the UI timeline; private notes are excluded from public rendering. Polling every ten seconds recovers from stream disconnects. Terminal delivery states stop polling.

## Scope and authorization

Merchant credentials/webhooks and bulk orders are excluded until Merchant Service is merged. AI Assistant is also excluded. Existing identity registration and merchant approvals continue to work. Parcel booking and estimates verify the current approval decision with Identity; pending merchants receive HTTP 403 even with a valid merchant token.

Dispatch, notification and pricing verify signed JWTs. Courier directory/registration and dispatch assignment creation require ADMIN; a courier can access/update only their own profile and assignments. Parcel access checks enforce assignment ownership, and delivered status requires the dispatch proof endpoint. Public tracking excludes private notes in the frontend. Notification history, preferences and read-state operations are scoped to the authenticated recipient.

SMTP delivery uses Mailpit locally. Production email requires SMTP credentials. Google social login requires valid external credentials. Durable event outbox/replay and immediate cross-service access-token revocation remain production-hardening work.
