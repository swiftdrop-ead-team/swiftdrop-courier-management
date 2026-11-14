# Merged-service integration status

> Remote status correction: Hiruni's [Merchant Service PR #8](https://github.com/swiftdrop-ead-team/swiftdrop-courier-management/pull/8) was merged into GitHub `main` on 13 November 2026. This local `feature/frontend` checkout still contains only the merchant README and has not incorporated that merge. The implementation and test results below describe the local checkout. Merchant integration is therefore pending locally, rather than pending a GitHub merge. Its service-owned approval must also be reconciled with Identity approval before enabling the full merchant workflow.

Scope: frontend, identity, parcel, dispatch, pricing, tracking, notifications, gateway and local infrastructure. Merchant Service and AI Assistant Service are deliberately excluded until their changes are merged. Existing merchant registration/approval in Identity remains available; merchant bulk orders/API keys/webhooks remain clearly unavailable in live screens.

## Changes

- Dispatch, pricing and notification APIs now validate signed JWTs. Administrative operations require ADMIN. Couriers can access only their own operational profile and assignments; notification recipients can inspect/read only their own records.
- Dispatch migration versions are unique. Courier and assignment versions guard concurrent updates, and a unique parcel index prevents duplicate assignments. Delivery stages are enforced; completion requires recipient proof. Parcel status updates also enforce assignment ownership and cannot bypass proof. Merchant booking/estimates verify the current Identity approval decision on the server; pending accounts cannot bypass the UI restriction.
- Parcel calls the real pricing calculator with the user's token. Pricing exposes calculation and admin service-rate APIs backed by the existing calculation strategies and persisted rules. New fees are LKR, and confirmed historical fees/currencies are preserved. Pricing outages no longer silently substitute a local fee.
- Five-minute estimates persist in PostgreSQL. Confirmation uses a row lock and consumes the estimate in the booking transaction, so reuse is rejected and transaction failure rolls consumption back.
- Tracking consumes parcel creation/cancellation as well as dispatch progress. Dispatch consumes cancellations to close assignments and release capacity. Terminal public tracking states cannot be reopened by later conflicting updates.
- Account contacts, pickup address and saved addresses persist through Identity APIs. Sign-in email and merchant business identity remain read-only. Admins can update account roles/status with self-demotion/deactivation protection.
- Notification history is filtered server-side. Read state and per-account email preferences persist. SMTP failures are recorded as failures. Courier assignment email uses the courier's configured email instead of a shared hard-coded address.
- Password recovery sends a reset link instead of logging the token. Local Compose includes Mailpit to capture real SMTP submissions without external credentials.
- Shared delivery dashboards use Soft UI statistics cards, buttons, panel headings and tracking timelines. Forms use separate accessible labels and consistent input heights. Removed a global card background rule that hid white text on the admin information card. Fixed the progress theme so loading animations remain contained and percentage bars show their actual values. Dashboard features load on demand, and each page requests only the data it uses.
- CI tests every merged backend service. Docker contexts exclude dependencies/build output. Local service heaps are bounded for the multi-service stack.

## Running locally

Normal source build:

```powershell
docker compose up --build -d
```

If Docker cannot resolve Maven Central but your host Maven cache is available:

```powershell
# Run without -Offline on a first host build.
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1 -Offline -MavenRepository "$env:USERPROFILE/.m2/repository"
```

This builds host JARs and the production frontend, then packages runtime Docker images and starts the same Compose services. It does not delete database volumes. `-SkipTests` is available when the unchanged sources have already passed tests. Generated runtime configuration lives in ignored `.tmp/runtime/`.

- Frontend: `http://localhost:3000`
- Gateway: `http://localhost:8080`
- Local mail/reset links: `http://localhost:8025`

Create the operational courier profile from Admin → Dispatch using an existing identity courier account. Older dispatch demo rows use synthetic identifiers and are excluded from live assignment choices.

Run the acceptance test after services are healthy:

```powershell
python scripts/smoke_test.py
```

The script creates test accounts and parcels and retains them for inspection. It exercises the frontend proxy/gateway and real services, checks fee confirmation, delivery/proof, cancellation, tracking, profile/address persistence and notification privacy/read state. It does not use mock APIs.

## Verification

- Frontend production build passed. All 66 frontend tests passed. The 16 affected delivery/pricing workflow tests also passed after the final page-specific loading change.
- Backend tests passed for identity (8), parcel (36), dispatch (26), pricing (220), tracking (6), and notifications (9). Gateway previously passed its test and its current executable package build passed.
- Browser preview checks covered customer, courier, admin, pricing, public tracking and a 390-pixel mobile booking screen. No runtime exceptions or horizontal overflow were observed. Screenshots in ignored `.tmp/screenshots/` confirmed the repaired admin card and mobile labels.
- A clean source build inside Docker could not resolve Maven Central in this environment. Host packages build successfully from the existing cache; runtime-stack verification uses those packages.
- The real gateway/Nginx smoke test passed customer and approved-merchant booking, pending-merchant rejection, persisted fee confirmation/reuse protection, admin access updates/self-protection, courier assignment and proof, delivery/cancellation synchronization, public tracking, notification privacy/read state, profile/addresses and email preferences.
- The reset email was received through Mailpit; its token successfully changed the password, the old password was rejected, the new password logged in, and token reuse was rejected.
- Production browser checks signed in as admin, customer and courier using isolated sessions. The final pricing, account, mobile booking and courier checks inspect API warnings as well as horizontal overflow. Screenshots are in `.tmp/screenshots/`.
- First startup on this 8 GB host was slow under concurrent build/browser load. Compose now allows a three-minute service startup grace period plus health retries. Runtime images and local database data are retained.

## Remaining external requirements and production limits

Google OAuth requires valid Google client credentials and redirect configuration. Mailpit captures development email; external delivery requires configured SMTP credentials and AUTH/TLS settings. Merchant/AI domain features are excluded, as requested. Current live booking supports today's pickup and no COD, and dispatch assigns immediately; scheduling, COD and GPS/route optimization are separate product capabilities.

This is not a claim of production certification. A durable event outbox/replay, operational monitoring, SMTP retry operations, immediate cross-service revocation of already-issued access tokens, and deployment-specific secrets/TLS still need a production hardening pass. Access tokens expire after the configured short lifetime; account access changes revoke refresh sessions and Identity checks the current account status.
