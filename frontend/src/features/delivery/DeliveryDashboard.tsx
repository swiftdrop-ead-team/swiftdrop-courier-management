import { WorkspaceText as Typography } from "./components";
import React, { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Card,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  LinearProgress,
  Snackbar,
} from "@mui/material";
import {
  Link,
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import { ActionButton as SoftButton } from "./components";
import type { UserRole } from "../../types/auth";
import { AdminDashboard } from "../../pages/admin/AdminDashboard";
import customerImage from "../../assets/images/customer-parcel.png";
import merchantImage from "../../assets/images/merchant-packing.png";
import courierImage from "../../assets/images/courier-shift.png";
import { day, money, terminal } from "./model";
import { previewStorageError, resetPreview, updatePreview } from "./preview";
import { deliveryApi } from "./service";
import { useDeliveryWorkspace, type Workspace } from "./useDeliveryWorkspace";
import { BookingForm } from "./BookingForm";
import { Dispatch, ParcelList } from "./ParcelViews";
import { LivePricing } from "./LivePricing";
import { Pricing, Reports, UserAdministration } from "./AdminOperations";
import { Account, MerchantIntegration, Notifications } from "./AccountViews";
import { BulkOrders } from "./BulkOrders";
import { TrackingView } from "./TrackingView";
import { MerchantOverview, MerchantReports } from "./MerchantWorkspace";
import { Metrics, Panel, Status } from "./components";

const headings: Record<string, [string, string]> = {
  overview: [
    "Your deliveries, connected.",
    "A clear view of your next pickup and every delivery in progress.",
  ],
  send: ["Send a parcel", "Get an estimate and confirm your delivery."],
  "create-shipment": ["Send a parcel", "Create an order for your business."],
  shipments: ["My shipments", "All your orders and their delivery states."],
  deliveries: [
    "Deliveries",
    "Review assignments and update delivery progress.",
  ],
  route: ["Active route", "Your pickups and deliveries for the day."],
  history: ["Delivery history", "Review deliveries and download receipts."],
  track: [
    "Track a shipment",
    "Follow your parcel through each delivery stage.",
  ],
  tracking: [
    "Track a parcel",
    "Follow delivery progress using a tracking number.",
  ],
  dispatch: [
    "Dispatch workspace",
    "Assign parcels and monitor courier workloads.",
  ],
  pricing: ["Delivery pricing", "Manage service rates for new bookings."],
  reports: ["Delivery reports", "Understand delivery volumes and outcomes."],
  profile: ["Your profile", "Manage contact details and delivery preferences."],
  settings: [
    "Account settings",
    "Your profile, preferences, and saved addresses.",
  ],
  api: ["Store connections", "Optional: automate orders and delivery updates with API keys and webhooks."],
  notifications: ["Notifications", "Keep track of important delivery updates."],
  "bulk-upload": [
    "Upload orders",
    "Validate a CSV and create multiple delivery requests.",
  ],
  access: ["User access", "Manage account status and roles."],
};
const roleTabs: Record<UserRole, string[]> = {
  CUSTOMER: [
    "overview",
    "send",
    "track",
    "history",
    "settings",
    "notifications",
  ],
  MERCHANT: [
    "overview",
    "shipments",
    "create-shipment",
    "bulk-upload",
    "tracking",
    "history",
    "profile",
    "reports",
    "api",
    "notifications",
  ],
  COURIER: [
    "overview",
    "deliveries",
    "route",
    "history",
    "profile",
    "notifications",
  ],
  ADMIN: ["dispatch", "deliveries", "pricing", "reports", "access", "profile"],
};
function Overview({
  workspace: w,
  base,
}: {
  workspace: Workspace;
  base: string;
}) {
  const courier = w.role === "COURIER";
  const merchant = w.role === "MERCHANT";
  const active = w.parcels.filter((parcel) => !terminal(parcel.status));
  const available = !w.errors.parcels;
  const self = w.couriers.find((entry) => entry.id === w.ownerId);
  return (
    <Box sx={{ display: "grid", gap: 3 }}>
      <Metrics
        items={[
          {
            label: courier ? "Assigned today" : "Total bookings",
            value: available
              ? courier
                ? w.parcels.filter((p) => p.assignedDate === day()).length
                : w.parcels.length
              : "Unavailable",
          },
          {
            label: "In progress",
            value: available ? active.length : "Unavailable",
          },
          {
            label: "Delivered",
            value: available
              ? w.parcels.filter((p) => p.status === "DELIVERED").length
              : "Unavailable",
          },
          {
            label: courier ? "Cash collected" : "Delivery fees",
            value: available
              ? money(
                  w.parcels
                    .filter((p) =>
                      courier
                        ? p.status === "DELIVERED"
                        : p.status !== "CANCELLED",
                    )
                    .reduce((sum, p) => sum + (courier ? p.cod : p.amount), 0),
                  w.parcels[0]?.currency,
                )
              : "Unavailable",
          },
        ]}
      />
      <Grid container spacing={3}>
        <Grid item xs={12} lg={8}>
          <Card sx={{ p: 3, height: "100%" }}>
            <Grid container spacing={3} alignItems="center">
              <Grid item xs={12} sm={7}>
                <Typography variant="overline" fontWeight={700}>
                  SWIFTDROP · {w.role}
                </Typography>
                <Typography variant="h4" fontWeight={700} mt={1}>
                  {courier
                    ? "Every handover matters."
                    : merchant
                      ? "Your business. On the move."
                      : "A parcel. A little possibility."}
                </Typography>
                <Typography
                  variant="body2"
                  color="text.secondary"
                  mt={2}
                  mb={3}
                >
                  {courier
                    ? "Review your assignments, record each milestone, and complete deliveries with confidence."
                    : merchant
                      ? "Book pickups, manage your orders, and connect your store to SwiftDrop."
                      : "Send something meaningful. Check your fee, arrange a pickup, and follow every step."}
                </Typography>
                <SoftButton
                  component={Link}
                  to={`${base}/${courier ? "deliveries" : merchant ? "create-shipment" : "send"}`}
                  variant="gradient"
                  color="primary"
                >
                  {courier ? "View assigned deliveries" : "Send a parcel"}
                </SoftButton>
              </Grid>
              <Grid item xs={12} sm={5}>
                <Box
                  component="img"
                  src={
                    courier
                      ? courierImage
                      : merchant
                        ? merchantImage
                        : customerImage
                  }
                  alt={
                    courier
                      ? "Courier riding a delivery scooter"
                      : merchant
                        ? "Merchant packing delivery orders"
                        : "Customer receiving a parcel"
                  }
                  sx={{
                    width: "100%",
                    maxHeight: 280,
                    display: "block",
                    objectFit: "contain",
                    borderRadius: 3,
                    bgcolor: "#f7f3e9",
                  }}
                />
              </Grid>
            </Grid>
          </Card>
        </Grid>
        <Grid item xs={12} lg={4}>
          <Panel
            title={courier ? "Your availability" : "Keep deliveries in sight"}
            subtitle={
              courier
                ? "Let dispatch know whether you can accept assignments."
                : `${active.length} deliveries are in progress.`
            }
          >
            {courier ? (
              <>
                <Typography fontWeight={700}>
                  {self
                    ? self.available
                      ? "Available for assignments"
                      : "You are offline"
                    : "Availability unavailable"}
                </Typography>
                <SoftButton
                  sx={{ mt: 3 }}
                  role="switch"
                  aria-label="Courier availability"
                  aria-checked={self?.available || false}
                  variant="gradient"
                  color="primary"
                  disabled={w.busy || !self}
                  onClick={() =>
                    void w.act(async () => {
                      if (w.preview)
                        updatePreview((state) => {
                          const entry = state.couriers.find(
                            (c) => c.id === w.ownerId,
                          );
                          if (entry) entry.available = !entry.available;
                        });
                      else await deliveryApi.availability(!self!.available);
                    }, "Availability updated.")
                  }
                >
                  {self?.available ? "Go offline" : "Go available"}
                </SoftButton>
              </>
            ) : (
              <>
                <Typography variant="body2">
                  Your tracking number is all you need to follow a delivery.
                  Share the public tracking link with your recipient.
                </Typography>
                <SoftButton
                  component={Link}
                  to={`${base}/${merchant ? "tracking" : "track"}`}
                  variant="outlined"
                  color="dark"
                  sx={{ mt: 3 }}
                >
                  Track a parcel
                </SoftButton>
              </>
            )}
          </Panel>
        </Grid>
      </Grid>
      <Panel title="Next deliveries">
        {active.slice(0, 4).map((parcel) => (
          <Box
            key={parcel.id}
            sx={{
              py: 2,
              borderBottom: "1px solid #e2e8f0",
              display: "flex",
              justifyContent: "space-between",
              flexWrap: "wrap",
              gap: 2,
            }}
          >
            <Box>
              <Typography variant="body2" fontWeight={700}>
                {parcel.trackingNumber}
              </Typography>
              <Typography variant="caption">
                {parcel.recipientName} · {parcel.dropoffAddress}
              </Typography>
            </Box>
            <Status value={parcel.status} />
          </Box>
        ))}
        {!active.length && (
          <Typography variant="body2" color="text.secondary" py={3}>
            {available
              ? "Your active queue is clear."
              : "Delivery records could not be loaded."}
          </Typography>
        )}
        <SoftButton
          component={Link}
          to={`${base}/${courier ? "deliveries" : merchant ? "shipments" : "history"}`}
          variant="text"
          color="dark"
          sx={{ mt: 2 }}
        >
          View deliveries
        </SoftButton>
      </Panel>
    </Box>
  );
}
export function DeliveryDashboard({
  role,
  previewMode = false,
}: {
  role: UserRole;
  previewMode?: boolean;
}) {
  const { tab = "overview" } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const base =
    role === "CUSTOMER"
      ? previewMode
        ? "/customer/dashboard-preview"
        : "/customer/dashboard"
      : previewMode
        ? `/dashboard-preview/${role.toLowerCase()}`
        : `/${role.toLowerCase()}/dashboard`;
  const requestedTab =
    role === "MERCHANT" && tab === "settings" ? "profile" : tab;
  const active = roleTabs[role].includes(requestedTab)
    ? requestedTab
    : role === "ADMIN"
      ? "dispatch"
      : "overview";
  const w = useDeliveryWorkspace(role, previewMode, active);
  const [resetOpen, setResetOpen] = useState(false);
  const [booked, setBooked] = useState("");
  useEffect(() => {
    w.setActionError("");
  }, [location.pathname]);
  useEffect(() => {
    if (role === "MERCHANT" && tab === "settings")
      navigate(`${base}/profile`, { replace: true });
  }, [base, navigate, role, tab]);
  const content = () => {
    switch (active) {
      case "overview":
        return role === "MERCHANT" ? <MerchantOverview workspace={w} base={base} /> : <Overview workspace={w} base={base} />;
      case "send":
      case "create-shipment":
        return (
          <BookingForm
            workspace={w}
            onBooked={(tracking) => {
              setBooked(tracking);
              navigate(
                `${base}/${role === "MERCHANT" ? "shipments" : "history"}`,
              );
            }}
          />
        );
      case "shipments":
      case "deliveries":
        return <ParcelList workspace={w} />;
      case "history":
        return (
          <ParcelList
            workspace={w}
            history={role === "COURIER" || role === "MERCHANT"}
          />
        );
      case "route":
        return <ParcelList workspace={w} route />;
      case "track":
      case "tracking":
        return (
          <TrackingView
            preview={previewMode}
            initialNumber={params.get("number") || ""}
          />
        );
      case "dispatch":
        return <Dispatch workspace={w} />;
      case "pricing":
        return previewMode ? <Pricing workspace={w} /> : <LivePricing />;
      case "reports":
        return role === "MERCHANT" ? <MerchantReports workspace={w} /> : <Reports workspace={w} />;
      case "access":
        return <UserAdministration preview={previewMode} />;
      case "profile":
      case "settings":
        return <Account workspace={w} />;
      case "api":
        return <MerchantIntegration workspace={w} />;
      case "notifications":
        return <Notifications workspace={w} />;
      case "bulk-upload":
        return <BulkOrders workspace={w} />;
      default:
        return null;
    }
  };
  return (
    <Box
      sx={{
        "& .MuiCard-root": { minWidth: 0 },
        "& .MuiTypography-root": { overflowWrap: "anywhere" },
        display: "grid",
        gap: 3,
      }}
    >
      {previewMode && (
        <Alert severity="info" sx={{ mb: 0 }}>
          Shared frontend preview · Bookings, assignments, and status changes
          stay in this browser tab across role previews. No email, webhook, or
          backend request is sent.
        </Alert>
      )}
      {previewMode && previewStorageError() && (
        <Alert severity="warning" sx={{ mb: 0 }}>
          {previewStorageError()}
        </Alert>
      )}
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          gap: 2,
          flexWrap: "wrap",
        }}
      >
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography component="h1" variant="h4" fontWeight={700} sx={{
            fontSize: "1.5rem",
            letterSpacing: "-0.02em",
            lineHeight: 1.2,
          }}>
            {role === "MERCHANT" && active === "overview" ? "Business overview" : role === "MERCHANT" && active === "reports" ? "Reports & COD" : headings[active][0]}
          </Typography>
          <Typography variant="body2" color="text.secondary" mt={0.5} sx={{
            fontSize: "0.88rem",
            lineHeight: 1.5,
          }}>
            {headings[active][1]}
          </Typography>
        </Box>
        <SoftButton
          variant="outlined"
          color="dark"
          disabled={w.loading || w.busy}
          onClick={() => {
            if (previewMode) setResetOpen(true);
            else
              void (async () => {
                await w.refresh();
                if (role === "MERCHANT") await w.refreshAccount();
              })();
          }}
          sx={{
            borderRadius: "8px",
            flexShrink: 0,
          }}
        >
          {previewMode ? "Reset shared preview" : "Refresh data"}
        </SoftButton>
      </Box>
      {role === "MERCHANT" && (
        <Alert
          severity={
            previewMode || w.user?.merchantApprovalStatus === "APPROVED"
              ? "success"
              : "warning"
          }
          sx={{ mb: 2 }}
        >
          {previewMode
            ? "Preview business approved · sample operations only."
            : w.user?.merchantApprovalStatus === "APPROVED"
              ? "Business approved. You can create orders and manage your store integration."
              : `Business approval: ${w.user?.merchantApprovalStatus || "Pending"}. Booking and API access require approval. Refresh data to check for an updated decision.`}
        </Alert>
      )}
      {Object.entries(w.errors).map(([resource, message]) => (
        <Alert key={resource} severity="warning" sx={{ mb: 2 }}>
          {resource}: {message}
        </Alert>
      ))}
      {w.actionError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {w.actionError}
        </Alert>
      )}
      {booked && (
        <Alert severity="success" onClose={() => setBooked("")} sx={{ mb: 2 }}>
          Booking confirmed: <strong>{booked}</strong>. Open its details for a
          receipt and tracking link.
        </Alert>
      )}
      {w.loading ? (
        <Box role="status" aria-label="Loading delivery workspace">
          <LinearProgress />
          <Typography variant="body2" mt={2}>
            Loading your workspace…
          </Typography>
        </Box>
      ) : (
        content()
      )}
      <Snackbar
        open={Boolean(w.message)}
        autoHideDuration={5000}
        message={w.message}
        onClose={() => w.setMessage("")}
      />
      <Dialog
        open={resetOpen}
        onClose={() => setResetOpen(false)}
        aria-labelledby="reset-operations-title"
      >
        <DialogTitle id="reset-operations-title">
          Reset all delivery previews?
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            This restores sample deliveries, zones, courier assignments,
            profiles, addresses, credentials, and notifications for every role
            in this tab.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <SoftButton
            variant="text"
            color="dark"
            onClick={() => setResetOpen(false)}
          >
            Keep changes
          </SoftButton>
          <SoftButton
            variant="gradient"
            color="primary"
            onClick={() => {
              resetPreview();
              setResetOpen(false);
              setBooked("");
              navigate(`${base}/${role === "ADMIN" ? "dispatch" : "overview"}`);
            }}
          >
            Reset preview data
          </SoftButton>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
export function AdminWorkspace({
  previewMode = false,
}: {
  previewMode?: boolean;
}) {
  const { tab } = useParams();
  return roleTabs.ADMIN.includes(tab || "") ? (
    <DeliveryDashboard role="ADMIN" previewMode={previewMode} />
  ) : (
    <AdminDashboard previewMode={previewMode} />
  );
}
