# SwiftDrop readiness review

> Historical review before the integration fixes. See [merged-services-status.md](merged-services-status.md) for the updated implementation and verification.

Reviewed 14 November 2026 against the current working tree, including uncommitted work.

## Assessment

The frontend has broad screen coverage and a working production build. It is substantially implemented for a demonstration of the core delivery workflow. The full product is not complete: several screens offer preview-only behavior, backend authorization needs work, and important integrations are unfinished. A reliable completion percentage cannot be inferred from screen count.

This is a source and automated-check review, not a browser visual/accessibility audit or a live end-to-end certification. Existing application changes were preserved.

## Feature coverage

| Area | Current implementation | Remaining work |
| --- | --- | --- |
| Authentication | Login, customer/merchant registration, role guards, refresh handling, OAuth callback and reset screens; identity backend exists | Verify real Google OAuth; deliver reset links by email rather than logging tokens |
| Customer | Overview, individual booking, fee confirmation, parcel history, cancellation, tracking, receipts | Profile/address persistence; scheduled pickup and COD if required |
| Merchant | Approval workflow and individual shipment operations use backend APIs | Merchant service is a README placeholder; bulk creation, credentials, webhooks, billing and business profile operations remain unfinished |
| Courier | Assignment loading, availability, delivery status and recipient proof are wired to dispatch | Enforce ownership and lifecycle rules on the server; verify live workflow and clarify whether real GPS/navigation is required |
| Admin | Identity user list and merchant review; delivery and dispatch adapters; derived reports | User access mutations and pricing management remain unavailable live; reports are based on loaded records rather than a dedicated analytics service |
| Tracking | Read model, SSE, polling recovery and public tracking UI | Consume booking/cancellation events too; currently tracking listens to dispatch status updates only |
| Pricing | Calculation strategies, quote/discount/zone services and tests exist | Only health REST controller exists. Expose actual calculation/quote endpoints and align request/response contracts |
| Notifications | Dispatch listeners, history UI/API and email sender | Authentication/ownership, genuine delivery failure handling, reset emails, unread state; SMS/push if in scope |
| AI | Service README describes intended work | No application implementation; decide whether required for the submission |

## Priority 0: fix before declaring the core system ready

1. **Dispatch Flyway startup conflict.** `services/dispatch-service/src/main/resources/db/migration/` contains both `V2__seed_courier_data.sql` and `V2__notification_recipient.sql`. Flyway is enabled; duplicate versions prevent normal migration validation/startup. Give the new migration a unique version after checking which migrations existing databases have applied. Do not rename already applied migrations blindly.
2. **Dispatch and notification APIs lack authorization.** Their POMs do not include Spring Security, and controllers expose assignments, courier updates, notification history and manual sending without principal checks. Gateway filters add headers/correlation IDs, not authentication. Add JWT verification, role rules and resource ownership at the service boundary. Client filtering does not protect direct API calls.
3. **Courier parcel access is broader than assignment ownership.** `ParcelServiceImpl.validateParcelAccess` returns for any courier even when the courier ID does not match. Restrict normal delivery access to assigned parcels; define separate, explicit hub-intake permissions if needed.
4. **Dispatch lifecycle validation is incomplete.** `DispatchAssignmentServiceImpl.updateAssignmentStatus` accepts the requested enum without checking allowed transitions and can mark DELIVERED without proof. Proof submission can also complete a terminal assignment. Enforce transitions, proof requirements, idempotency and workload consistency server-side; cover concurrent assignment/capacity updates.

## Priority 1: complete the core integrations

1. **Pricing currently falls back.** Parcel's `FallbackPricingClientService` calls `POST /api/v1/pricing/calculate` and reads top-level `totalFee`, but pricing exposes only `/health`. Booking may still succeed using a local USD calculator; that does not verify pricing-service integration. Implement and test the endpoint, including fee currency, delivery tiers and quote expiry/ownership. Confirm the intended currency with project requirements.
2. **Tracking does not start at booking.** Parcel publishes events to `swiftdrop.parcel.exchange`; tracking consumes `dispatch.status.updated` from `dispatch.exchange`. New and cancelled bookings therefore lack that event-fed public history until supported dispatch events arrive. Add compatible event consumers and test booking, cancellation, assignment and status history together.
3. **Password recovery stops at logs.** `AuthService.forgotPassword` saves a reset token and logs its value; it does not send a reset link. Integrate email delivery and remove token logging.
4. **Email success is misleading.** Compose defaults notification mock mode to true. `EmailSenderService` returns true even after real SMTP failure. Keep simulation explicitly identified in development; record failures and retry appropriately in live operation. Pass the required SMTP configuration through deployment configuration.
5. **Run a real cross-role acceptance test.** Register/login customer -> request estimate -> confirm booking -> admin assignment -> courier pickup/transit/out-for-delivery/proof -> customer/public tracking -> notification history. Also verify cancellation, failed delivery, unavailable services, expired session and forbidden access from another account. Test persistence after restarting services.

Also reconcile cancellation with dispatch: parcel cancellation publishes a parcel event, but dispatch has no corresponding cancellation listener. An assigned booking cancelled before pickup can leave an active assignment and occupied courier capacity. Test and implement this path explicitly. Parcel estimates are currently stored in one process's memory and consumed before booking persistence; consider shared storage and retry/idempotency semantics before scaling or handling database failures.

## Priority 2: finish the remaining visible features

The unavailable methods are explicit in `frontend/src/features/delivery/service.ts`: zone administration, profile editing, saved addresses, notification read status, merchant credentials/key rotation, webhooks/history/retry. `DeliveryDashboard.tsx` also gates live bulk upload behind an explanation. Implement backend endpoints and adapters for required features, or clearly remove them from the agreed delivery scope.

Booking currently supports today's pickup and zero COD; dispatch supports assignment for today. These constraints are enforced by the adapter. Extend the database, DTOs, validation and workflow together if scheduled pickups, scheduling, COD collection or SAME_DAY selection are requirements.

Merchant and AI service directories contain only READMEs and are absent from Compose, although gateway routes exist. Their presence in the architecture does not mean they are implemented.

## Quality and deployment follow-up

- Expand `.github/workflows/ci.yml` backend test coverage beyond identity and gateway to parcel, dispatch, pricing, tracking and notifications. Docker image builds alone do not establish migration or runtime health.
- Add live API integration checks and a browser end-to-end smoke test. Current frontend tests primarily use previews/mocks; passing them does not prove broker/database interoperability.
- Review desktop/mobile layouts, keyboard navigation, focus, screen-reader labels and loading/error states in a real browser.
- Reduce frontend payload: the build emits approximately 1.12 MB main JavaScript before gzip and four role images of roughly 2 MB each. Split feature routes and compress/resize illustrations.
- Update root README ownership/status tables: several implemented services are still labelled reserved. Document the actual supported workflow and disabled features consistently.
- Vite configuration is build-time. Compose's frontend runtime `VITE_API_BASE_URL` does not change a prebuilt bundle; the existing same-origin Nginx proxy is the relevant default. Validate the intended deployment origin, CORS, OAuth redirects and non-development credentials.
- Verify migrations against both a clean database and an existing volume without deleting existing data.

## Verification

- Frontend: `npm.cmd test -- --reporter=dot` passed **64 tests across 11 files**.
- Frontend: `npm.cmd run build` passed TypeScript compilation and Vite production bundling; Vite reported a large JavaScript chunk warning.
- Test output includes React Router/defaultProps warnings and jsdom network errors despite passing assertions; isolate auth tests from real requests and clean these up.
- Backend: offline Maven tests passed for identity (8), parcel (33), dispatch (19), pricing (216), tracking (3), and notification (7). These are service test results, not evidence of a functioning full stack. Dispatch test configuration disables Flyway, so it does not detect the duplicate migration version.
- Gateway: offline Maven tests passed.
- Docker runtime inspection could not connect to the Docker engine because access to the Windows Docker pipe was denied. No live container or browser workflow was verified.

## Recommended order

Fix migration and server permissions first. Then complete pricing, tracking event ingestion, password recovery and notification delivery. Prove the core workflow against running services. Finish the required merchant/account/admin features next, then improve performance, documentation and submission evidence.
