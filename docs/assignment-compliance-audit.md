- Frontend tests and production build are configured. The latest run passed 69 tests across 12 files; generated coverage is 44.65% overall and 78.83% across delivery-feature files. See [frontend verification](frontend-verification.md).
# Assignment Compliance Audit

Reviewed: 14 November 2026. Scope: current local checkout, routed frontend, Spring services, Compose stack, tests, smoke scripts, and submission documents. This is a source and test-evidence review, not a production certification.

## Summary

The core customer, merchant, courier, and dispatcher workflows are implemented against the service APIs and persisted data. The active role dashboards are not sample-only previews. The remaining material gaps are mainly assignment-submission artifacts and external production configuration: the required complete use-case and ER diagrams, two sequence diagrams, a named contribution sheet, a design PDF, Google OAuth credentials, and production email/secrets.

The older [initial requirements check](assignment-requirements-check.md) and [readiness review](project-readiness-review.md) are historical and contain findings that no longer match this checkout. The architecture document also needs a status and accuracy pass before submission.

Status key: **Implemented** = source and/or automated workflow evidence exists; **Conditional** = code exists but needs external configuration or a specific environment; **Gap** = requested submission artifact is absent or incomplete.

## Functional Requirements

| Requirement | Status | Current evidence and notes |
| --- | --- | --- |
| Customer signup, login, JWT sessions, Google login | Conditional | Identity service implements password auth, refresh sessions, role claims, and Google OAuth callback. Real Google client credentials and a provider-backed acceptance test are still required. |
| Customer booking, fee estimate, confirmation | Implemented | Frontend booking calls the parcel and pricing APIs. Quotes are persisted, short-lived, and consumed transactionally on confirmation. Same-day pickup is currently supported. |
| Parcel history, receipt, public mobile tracking | Implemented | Customer history/receipt and public tracking are live. Tracking uses SSE with polling recovery and exposes a restricted public projection. |
| Merchant registration and admin approval | Implemented | Identity owns registration decisions; Merchant Service synchronizes approval before enabling merchant operations. |
| Merchant API order creation | Implemented | Merchant API supports scoped API keys and JWT-authenticated orders, idempotency, and parcel-service integration. See `scripts/merchant_smoke_test.py`. |
| Merchant status webhooks with retry | Implemented | Signed webhook deliveries, persisted attempts, retry scheduling, and delivery history are implemented. The local webhook-receiver smoke path is opt-in because local callback URLs are blocked by default. |
| Merchant orders dashboard | Implemented | The live merchant dashboard uses backend shipment/order data and exposes shipment, tracking, API, notifications, and bulk-order workflows. |
| Courier assigned deliveries and status updates | Implemented | Dispatch assignments are scoped to the signed-in courier. Server-side stage validation, delivery proof by recipient name, and failed-delivery reasons are enforced. |
| Dispatcher assignment and workload monitoring | Implemented | Admin dispatch views use persisted courier and assignment records; capacity and ownership rules are enforced server-side. |
| Admin users, merchant approvals, pricing, and reports | Implemented | Identity admin APIs support user access and merchant review; pricing rules/zones and operational reports are available in the admin workspace. |

## System Requirements

| Requirement | Status | Current evidence and notes |
| --- | --- | --- |
| Responsive React frontend | Implemented | Customer, merchant, courier, admin, and public tracking screens are routed through shared role layouts. Existing browser checks cover desktop and 390px mobile widths. |
| Versioned APIs, validation, centralized errors | Implemented | Gateway routes `/api/v1`; services validate DTOs and return controlled error responses. |
| JWT and role-based/resource authorization | Implemented | Services validate signed tokens and enforce role and record ownership. `scripts/smoke_test.py` checks unauthorized access and courier/customer boundaries. |
| Real-time delivery updates | Implemented | Tracking consumes asynchronous state changes and serves SSE; the frontend recovers with polling. |
| Asynchronous status integration | Implemented | RabbitMQ carries parcel/dispatch status events to tracking, notifications, and merchant integration. Cancellation synchronization is covered by the smoke flow. |
| Notification consumer | Implemented | Notification service consumes events, scopes history/read state to the owner, and sends email. Compose uses Mailpit for local email capture; external SMTP needs credentials. |
| Containerized local stack | Implemented | Compose defines the frontend, gateway, all operational services, PostgreSQL, RabbitMQ, and Mailpit. The stack was running during this audit. |
| Personal-data/security practices | Implemented with deployment caveat | Passwords are BCrypt-hashed; request validation, authorization, privacy-filtered tracking, and controlled errors are present. Compose defaults are development-only; production requires managed secrets, TLS, and SMTP configuration. |

## Design and Submission Artifacts

| Requirement | Status | Gap / existing material |
| --- | --- | --- |
| Use-case diagram for all four roles | Gap | No complete use-case diagram was found. The Merchant API contract has a prose use-case description only. |
| Full ER diagram across users, roles, merchants, parcels, history, couriers, zones, pricing | Gap | The Merchant API contract contains a merchant-only ER diagram; it does not cover the full system model. |
| Layered architecture proposal with justification | Partial | `docs/architecture/system-architecture.md` exists but contains stale claims (GPS, waybills, multi-channel notifications, service details). It needs to be reconciled with the implemented services and include explicit design rationale. |
| Integration pattern rationale | Partial | REST and RabbitMQ usage are visible in contracts and service code, but there is no single accurate project-level rationale. |
| Sequence/activity: parcel creation | Implemented | `docs/frontend-assignment-handoff.md` includes a customer booking sequence. Confirm its DTO names against the final API contract before PDF export. |
| Sequence/activity: merchant order | Implemented | `docs/api-contracts/merchant-api-v1.md` documents the merchant order/event/webhook sequence. |
| Sequence/activity: courier status to notification | Gap | No complete sequence diagram was found for status update through broker to tracking/notification/webhook. |
| Sequence/activity: Google OAuth | Gap | OAuth is described in prose, but the required sequence diagram is absent. |
| Design-patterns/principles note | Partial | Repository, Strategy, and adapter patterns are present in code and some contracts; a concise system-level SOLID/DRY/patterns note is missing. |
| Design documentation PDF | Gap | The repository contains Markdown, not a submission-ready PDF. |
| Contribution sheet with actual names and responsibilities | Gap | `docs/contribution-guide.md` lists Member 1-10 roles but no names, IDs, or actual contribution evidence. |
| README setup instructions | Implemented with review needed | Root README and Compose setup exist. Recheck all current service/port claims and production-vs-local configuration before submission. |

## Testing and Demo Evidence

- Backend unit, integration, and authorization tests exist across identity, parcel, dispatch, pricing, tracking, notification, merchant, and gateway services. The current service test suites passed in the latest local validation.
- `scripts/smoke_test.py` exercises customer booking through estimate, confirmation, dispatch assignment, courier proof, tracking, notification history, cancellation, and password reset through the frontend proxy/gateway.
- `scripts/merchant_smoke_test.py` covers merchant approval synchronization, scoped key rotation/revocation, JWT/API-key order creation, idempotency, cancellation, and dashboard summary. The signed webhook receiver path requires the local webhook override.
- Frontend tests and production build are configured. Coverage reports are generated under `frontend/reports/coverage/`; update the submitted summary after the final test run rather than reusing the historical counts in `frontend-verification.md`.
- The required demo can cover customer booking, merchant orders, dispatch, courier proof, tracking, and notifications. Google login requires provider credentials; a delivered external email requires SMTP credentials (Mailpit is suitable for the local demo).

## Product Boundaries and Next Steps

The assignment does not require COD, GPS route optimization, scheduled pickup, Kubernetes, or the AI assistant. Current live booking supports same-day pickup and no COD; dispatch assigns immediately. Treat these as product boundaries, not assignment failures. Kubernetes and AI are bonus items and are not implemented as full deployable features.

Before submission:

1. Replace or correct the stale architecture claims and add the missing role use-case, full-system ER, courier-notification, and OAuth diagrams.
2. Add a system-level design-pattern/integration rationale and export the final design document to PDF.
3. Replace contribution placeholders with the team's actual names, IDs, responsibilities, and evidence.
4. Regenerate frontend coverage/test summaries after final UI changes; retain the backend Surefire reports and acceptance-test output.
5. Configure Google OAuth and external SMTP only when those live integrations will be demonstrated; keep deployment secrets out of source control.