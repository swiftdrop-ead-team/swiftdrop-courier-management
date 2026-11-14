# Merchant workspace update

The active merchant route uses `features/delivery/DeliveryDashboard.tsx`. The older `pages/merchant/MerchantDashboard.tsx` is not routed.

## Merchant workflows

- Overview: pickup queue, in-transit/delivered/failed counts, recent shipments, and links to filtered work queues.
- Shipments: search, status and booking-date filters, details, cancellation before assignment, receipts, tracking links and CSV export.
- Booking: server-priced quotes, confirmation, requested pickup date, COD amount and saved pickup locations.
- Bulk import: CSV template, validation, quotes and per-row order creation with duplicate protection.
- Reports & COD: booking-date range, delivery outcomes, non-cancelled delivery fees, COD on delivered parcels and COD awaiting delivery. Export includes pickup date, COD and failure reason.
- Business & addresses: contact details, email preferences and up to 20 persisted pickup locations.
- Notifications and delivery history remain available.
- Store connections: optional API keys and signed webhooks. Kept because these are working backend capabilities; separated from daily shipping navigation.

## Backend contract and rollout

`POST /api/v1/parcels/estimate`, `POST /api/v1/parcels`, and `POST /api/v1/merchant/orders` accept optional `pickupDate` (`YYYY-MM-DD`) and `codAmount` (0–100000 LKR, at most two decimal places). Dates must be valid and today or later in Asia/Colombo. Omitted values default to today's pickup and zero COD on parcel creation. Authenticated parcel responses expose both values; public tracking does not expose COD.

Deploy the merchant and parcel services with the frontend. Parcel Flyway migration `V5__pickup_date_and_cod.sql` adds and backfills the columns. Existing rows retain zero COD. Pickup dates are requested dates, not reserved courier time slots.

COD reports are parcel-level operational amounts, not settlement or payout statements. Bank payouts, payment processing, return authorizations and refund accounting are not implemented by this change.

## Verification

Frontend workflow coverage includes merchant status links, report date validation, saved pickup locations and DTO mapping for pickup/COD. Backend coverage includes date/COD validation, parcel persistence and merchant-to-parcel forwarding, alongside existing approval, ownership, idempotency, cancellation and integration tests.

Validation on 2026-11-14: 73 frontend tests passed; 34 merchant-service and 38 parcel-service tests reported zero failures/errors. The production frontend build passed. Chrome checks covered overview, reports, shipments, booking, profile, store connections and a 390px mobile overview, with no runtime errors or page overflow. Browser checks used the existing local backend through a temporary development proxy; new pickup/COD backend behavior was verified in automated tests rather than deployed into that running stack.
