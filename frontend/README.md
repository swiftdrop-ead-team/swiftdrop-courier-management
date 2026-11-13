# SwiftDrop frontend

React 18, TypeScript, Vite, MUI/Soft UI, Tailwind, Axios, Zod, and Vitest. Visual palette: navy `#00274c`, yellow `#ffcb05`, and light cream illustrations specific to each role.

## Setup

Requires Node.js 22 and npm.

```sh
cd frontend
npm ci
npm run dev
```

Open `http://localhost:3000`. On Windows PowerShell with script execution restricted, use `npm.cmd` instead of `npm`.

The Vite dev server proxies `/api`, `/oauth2`, and `/login/oauth2` to `VITE_GATEWAY_URL` (default `http://localhost:8080`). Browser API requests default to `/api/v1`; set `VITE_API_BASE_URL` before building if a separate API origin is required. Production Nginx proxies same-origin requests to `gateway:8080` and supports SSE streaming. Vite environment variables are build-time configuration; changing a running Nginx container's environment does not rebuild the bundle.

## Frontend routes

| Route | Access / features |
| --- | --- |
| `/` | Public landing and tracking lookup |
| `/track/:trackingNumber?` | Public tracking with SSE/polling; no private contacts |
| `/login`, `/register`, `/merchant/register` | Login, Google login, customer/business registration |
| `/forgot-password`, `/reset-password`, `/auth/callback` | Account recovery and OAuth callback |
| `/customer/dashboard/:tab` | Overview, send, track, history/receipts, settings/addresses, notifications |
| `/merchant/dashboard/:tab?` | Overview, shipments, create-shipment, bulk-upload, tracking, history, profile, api/webhooks, notifications, settings |
| `/courier/dashboard/:tab?` | Overview/availability, deliveries, route, history, profile, notifications |
| `/admin/dashboard/:tab?` | Overview, merchants, users, activity, dispatch, deliveries, pricing, reports, access, profile |

Authenticated routes use ProtectedRoute and role guards. Server-side authorization remains mandatory.

## Development previews

Visit `/dashboard-preview` while running `npm run dev`:

- `/customer/dashboard-preview/overview`
- `/dashboard-preview/merchant/overview`
- `/dashboard-preview/courier/overview`
- `/dashboard-preview/admin/dispatch`
- `/tracking-preview/SD-DEMO-1001`

Delivery previews share validated session-storage data in one browser tab. Create a customer/merchant booking, assign it in admin dispatch, update it as a courier, then inspect public tracking, notifications, and reports. All preview screens identify sample data. No email, SMS, webhook, or domain API request is sent. Existing admin account/merchant-approval samples use a separate directory.

All preview routes are excluded from production routing. Authenticated delivery routes never substitute sample data after API failure.

## Backend integration

Existing auth and merchant-approval APIs are implemented by the identity service. Delivery domain services are still placeholders in this checkout. The frontend calls proposed typed APIs and shows unavailable states until those services are connected.

- [API contracts and DTOs](../docs/api-contracts/frontend-delivery-v1.md)
- Routed delivery implementation: `src/features/delivery/`
- Transport adapter: `src/features/delivery/service.ts`
- Shared preview data and transitions: `src/features/delivery/preview.ts`

Previous customer/merchant/courier standalone page files remain as reference/legacy component implementations; application routes use the new shared delivery implementation. Authentication and existing admin merchant review screens continue to be used.

## Verification and submission reports

```sh
npm run typecheck
npm run build
npm test
npm run test:coverage
```

Coverage outputs:

- JUnit test report: `reports/frontend-tests.xml`
- Coverage summary: `reports/coverage/coverage-summary.json`
- Detailed HTML coverage: `reports/coverage/index.html`
- LCOV: `reports/coverage/lcov.info`

Coverage includes application TypeScript/TSX source, including retained legacy pages, and excludes tests, declarations, and type-only folders. It does not measure backend behavior. Tests verify frontend guards, workflows, validation, privacy, and error states using mocks/previews. Perform live Google login and full end-to-end acceptance testing once backend services and credentials are available.
