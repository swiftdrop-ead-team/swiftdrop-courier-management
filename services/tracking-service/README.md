# SwiftDrop Tracking Service

Tracking is an event-fed read model. It does not create parcels, change parcel
state, assign couriers, or access another service database.

## Local configuration

The service runs on port `8086` and uses the `swiftdrop_tracking` database.
Copy the repository `.env.example` to `.env`, then start PostgreSQL and
RabbitMQ with Docker Compose. The service consumes:

| Setting | Value |
|---|---|
| Exchange | `dispatch.exchange` |
| Queue | `swiftdrop.tracking.status-updated.queue` |
| Routing key | `dispatch.status.updated` |
| Event | `DeliveryStatusUpdatedEvent` |

The queue is durable and has its own rejected-message dead-letter exchange.
Invalid messages are rejected without requeue.

## API

`GET /api/v1/tracking/{trackingNumber}` is a limited public lookup.

`GET /api/v1/tracking/{trackingNumber}/history` requires a valid Identity
Service JWT and an authorized role.

`GET /api/v1/tracking/{trackingNumber}/stream` returns an SSE stream. It sends
an initial `snapshot`, then `status` events and periodic heartbeat comments.
The gateway already routes `/api/v1/tracking/**` to this service.

## Contract limitations

The current Dispatch event does not contain `eventId`, `version`, customer
ownership, or merchant ownership. Tracking derives a deterministic idempotency
key when `eventId` is absent and falls back to event time for ordering. This
prevents duplicate payloads and stale timestamps from overwriting the current
status, but it is not a substitute for authoritative versions.

Customer ownership and merchant authorization cannot be enforced from the
current event contract. Member 03/04 must publish an owner/merchant reference
or provide an authorization endpoint before those checks can be safely enabled.
No private addresses, phone numbers, courier details, or internal IDs are
returned by this service.
