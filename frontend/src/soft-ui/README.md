# Soft UI Dashboard integration

Vendored from the supplied `soft-ui-dashboard-react-v1.0.0` folder (its package metadata identifies version 4.0.1). Copyright Creative Tim; the original [MIT license](./LICENSE.md) and source notices are retained.

The full `components`, `examples`, `assets`, and `context` trees are available here. JavaScript filenames are mechanically renamed to `.jsx` so Vite handles the template's JSX. Vite aliases preserve the original absolute imports. The original demo application, routes, and business content are not substituted for SwiftDrop.

## Active customer integration

- [Customer layout](../components/layout/CustomerDashboardLayout.tsx): template theme, context, `DashboardLayout` and `SidenavCollapse`; accessible responsive MUI drawer with SwiftDrop links and actions.
- [Customer dashboard](../pages/customer/CustomerDashboard.tsx): Soft UI primitives, mini statistics, report/gradient charts, tables and timeline examples adapted to customer shipping workflows.
- Other roles and public/authentication pages retain their existing styling.

## Compatibility adjustments

- React Chart.js bindings use React-18-compatible 4.x with Chart.js 3.x; chart controllers are registered by the customer layout.
- GradientLineChart initializes empty chart data before its first effect to support those bindings.
- Table cells use stable keys and column headers have a scope attribute; this preserves action focus across updates.
- Roboto and Material Icons are hosted locally through font packages, rather than depending on external font requests. The icon theme uses the matching `material-icons` class.
- JavaScript template props retain their original PropTypes; ambient declarations provide the TypeScript interop boundary.

Only imported modules/assets are emitted into the application bundle. A complete example library does not mean every demo widget (credit-card billing, VR, marketing upgrades, etc.) belongs on a courier customer page.

## Customer functionality

The overview, booking review/confirmation, status-aware tracking, searchable/filterable/paginated history with CSV export, address create/edit/delete, profile edits, and preferences work against a validated, per-account session-storage demo workspace. Refreshing the same tab retains that state. A persistent notice explains that no pickup, payment, account update or notification is sent to the server. Parcel/pricing/profile-update APIs are not implemented in the current frontend service layer.

Open `/customer/dashboard-preview/overview` for the public preview; the authenticated `/customer/dashboard/overview` retains the existing customer role guard.
