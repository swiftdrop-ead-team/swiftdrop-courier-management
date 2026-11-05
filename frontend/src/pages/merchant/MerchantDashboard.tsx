import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  Alert,
  Box,
  Card,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Grid,
  LinearProgress,
  Select,
  Snackbar,
  Step,
  StepConnector,
  stepConnectorClasses,
  StepLabel,
  Stepper,
  styled,
} from "@mui/material";
import {
  AlertTriangle,
  ArrowRight,
  Bell,
  Building2,
  Check,
  ChevronRight,
  Clock,
  Copy,
  Download,
  Edit2,
  Eye,
  FileSpreadsheet,
  Key,
  Loader2,
  MapPin,
  Package,
  Plus,
  Search,
  Send,
  ShieldCheck,
  Truck,
  Upload,
  X,
} from "lucide-react";
import SoftBox from "components/SoftBox";
import SoftTypography from "components/SoftTypography";
import SoftButton from "components/SoftButton";
import SoftInput from "components/SoftInput";
import SoftBadge from "components/SoftBadge";
import MiniStatisticsCard from "examples/Cards/StatisticsCards/MiniStatisticsCard";
import ReportsBarChart from "examples/Charts/BarCharts/ReportsBarChart";
import GradientLineChart from "examples/Charts/LineCharts/GradientLineChart";
import Table from "examples/Tables/Table";
import TimelineList from "examples/Timeline/TimelineList";
import TimelineItem from "examples/Timeline/TimelineItem";
import landscape from "assets/images/ivancik.jpg";
import { useAuth } from "../../hooks/useAuth";
import {
  Shipment,
  ShipmentStatus,
  ManifestItem,
  ManifestOrder,
  Notification,
  currency,
  dateLabel,
  dateTimeLabel,
  demoManifestItems,
  demoManifestOrders,
  manifestOrderStatusColor,
  shipmentStatusColor,
  statusColor,
  statusIndex,
  trackingSteps,
} from "./merchantData";
import {
  shipmentService,
  bulkUploadService,
  trackingService,
  notificationService,
  integrationService,
  merchantService,
  type CreateShipmentPayload,
} from "../../services/merchantService";

/* ================================================================
   Tab Definitions
   ================================================================ */

type Tab =
  | "overview"
  | "shipments"
  | "create-shipment"
  | "shipment-detail"
  | "bulk-upload"
  | "tracking"
  | "history"
  | "profile"
  | "api"
  | "notifications"
  | "settings";

const headings: Record<Tab, [string, string]> = {
  overview: [
    "Your merchant operations, at a glance.",
    "Track shipments, API usage, and delivery performance in one place.",
  ],
  "shipments": [
    "Your shipments.",
    "See all your parcels — who they're going to and where they are.",
  ],
  "create-shipment": [
    "Send a parcel.",
    "Fill in the details and we'll pick it up from your store.",
  ],
  "shipment-detail": [
    "Shipment details.",
    "Everything about this parcel — from pickup to delivery.",
  ],
  "bulk-upload": [
    "Upload many parcels.",
    "Add multiple shipments at once from a CSV file.",
  ],
  tracking: [
    "Track a parcel.",
    "Enter a tracking number to see where your parcel is.",
  ],
  history: [
    "Past deliveries.",
    "See what was delivered, failed, or cancelled.",
  ],
  profile: [
    "Your store.",
    "Update your business information and check your approval status.",
  ],
  api: [
    "API integration.",
    "Manage your API keys, webhooks, and integration settings.",
  ],
  notifications: [
    "Notifications.",
    "Stay updated on your shipments and account activity.",
  ],
  settings: [
    "Account settings.",
    "Update your preferences and security settings.",
  ],
};

/* ================================================================
   Styled Components
   ================================================================ */

const ColorlibConnector = styled(StepConnector)(({ theme }) => ({
  [`&.${stepConnectorClasses.alternativeLabel}`]: {
    top: 22,
  },
  [`&.${stepConnectorClasses.active}`]: {
    [`& .${stepConnectorClasses.line}`]: {
      borderColor: "#2563eb",
    },
  },
  [`&.${stepConnectorClasses.completed}`]: {
    [`& .${stepConnectorClasses.line}`]: {
      borderColor: "#16a34a",
    },
  },
  [`& .${stepConnectorClasses.line}`]: {
    borderColor: "#e2e8f0",
    borderTopWidth: 3,
    borderRadius: 1,
  },
}));

/* ================================================================
   Reusable Sub-Components
   ================================================================ */

function EmptyState({
  title,
  text,
  icon: Icon = Package,
}: {
  title: string;
  text: string;
  icon?: React.ElementType;
}) {
  return (
    <Box sx={{ p: 5, textAlign: "center" }}>
      <Icon size={32} style={{ margin: "0 auto 12px", color: "#8392ab" }} />
      <SoftTypography variant="h6">{title}</SoftTypography>
      <SoftTypography variant="button" color="text" fontWeight="regular">
        {text}
      </SoftTypography>
    </Box>
  );
}

function DetailRow({
  label,
  value,
}: {
  label: string;
  value: string | React.ReactNode;
}) {
  return (
    <Box sx={{ py: 1.5, borderBottom: "1px solid #f1f5f9" }}>
      <SoftTypography variant="caption" color="text" display="block" mb={0.5}>
        {label}
      </SoftTypography>
      <SoftTypography variant="body2" sx={{ overflowWrap: "anywhere" }}>
        {value}
      </SoftTypography>
    </Box>
  );
}

function ShipmentStatusBadge({ status }: { status: ShipmentStatus }) {
  return (
    <SoftBadge
      badgeContent={status}
      color={shipmentStatusColor(status)}
      size="xs"
      container
    />
  );
}

function ShipmentTimeline({
  shipment,
}: {
  shipment: Shipment | undefined;
}) {
  if (!shipment) return null;
  const currentIdx = statusIndex(shipment.status);
  const steps = trackingSteps.filter(
    (_, i) => i <= Math.max(currentIdx, 0),
  );

  // If status is Failed/Cancelled/Returned, append it
  const extraStatuses: ShipmentStatus[] = ["Failed", "Cancelled", "Returned"];
  let activeStep = currentIdx >= 0 ? currentIdx : 0;
  if (extraStatuses.includes(shipment.status)) {
    activeStep = steps.length;
  }

  const allSteps = [
    ...steps,
    ...(extraStatuses.includes(shipment.status)
      ? [{ status: shipment.status, label: shipment.status }]
      : []),
  ];

  return (
    <Card sx={{ p: 3 }}>
      <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 3 }}>
        Delivery Timeline
      </SoftTypography>
      <Stepper
        activeStep={activeStep}
        connector={<ColorlibConnector />}
        alternativeLabel
      >
        {allSteps.map((step, idx) => (
          <Step key={step.status} active={idx === activeStep} completed={idx < activeStep}>
            <StepLabel
              StepIconProps={{
                sx: {
                  color: idx < activeStep ? "#16a34a" : idx === activeStep ? "#2563eb" : "#e2e8f0",
                },
              }}
            >
              <SoftTypography
                variant="button"
                fontWeight={idx <= activeStep ? "bold" : "regular"}
                color={idx <= activeStep ? "dark" : "text"}
              >
                {step.label}
              </SoftTypography>
            </StepLabel>
          </Step>
        ))}
      </Stepper>
      {/* Status history */}
      <Box sx={{ mt: 4 }}>
        <SoftTypography variant="subtitle2" fontWeight="bold" sx={{ mb: 2 }}>
          Status History
        </SoftTypography>
        {shipment.statusHistory.map((entry, i) => (
          <Box
            key={i}
            sx={{
              display: "flex",
              gap: 2,
              alignItems: "flex-start",
              py: 1,
              borderBottom: "1px solid #f1f5f9",
            }}
          >
            <SoftBox
              component="span"
              bgColor={
                entry.status === "Delivered"
                  ? "success"
                  : entry.status === "Failed"
                    ? "error"
                    : "info"
              }
              borderRadius="lg"
              p={1}
              display="flex"
              sx={{ minWidth: 36, justifyContent: "center", color: "#00274c" }}
            >
              {entry.status === "Delivered" ? (
                <Check size={14} />
              ) : entry.status === "Failed" ? (
                <X size={14} />
              ) : (
                <Truck size={14} />
              )}
            </SoftBox>
            <Box sx={{ minWidth: 0 }}>
              <SoftTypography variant="button" fontWeight="bold" display="block">
                {entry.status}
                {entry.note && (
                  <SoftTypography
                    component="span"
                    variant="caption"
                    color="text"
                    sx={{ ml: 1, fontWeight: "regular" }}
                  >
                    — {entry.note}
                  </SoftTypography>
                )}
              </SoftTypography>
              <SoftTypography variant="caption" color="text">
                {dateTimeLabel(entry.timestamp)}
              </SoftTypography>
            </Box>
          </Box>
        ))}
      </Box>
    </Card>
  );
}

function ApprovalStatusBanner({
  status,
}: {
  status: "PENDING" | "APPROVED" | "REJECTED" | undefined;
}) {
  if (status === "APPROVED") {
    return (
      <Alert
        severity="success"
        icon={<ShieldCheck size={16} />}
        sx={{ mb: 3, borderRadius: 3, fontSize: 12 }}
      >
        Merchant account verified — all commercial features are active.
      </Alert>
    );
  }
  if (status === "REJECTED") {
    return (
      <Alert
        severity="error"
        icon={<AlertTriangle size={16} />}
        sx={{ mb: 3, borderRadius: 3, fontSize: 12 }}
      >
        Merchant account rejected — contact support@swiftdrop.io for assistance.
      </Alert>
    );
  }
  return (
    <Alert
      severity="warning"
      icon={<Clock size={16} />}
      sx={{ mb: 3, borderRadius: 3, fontSize: 12 }}
    >
      Your merchant account is pending approval. Some features are restricted
      until verified by the administrator.
    </Alert>
  );
}

function ConfirmDialog({
  open,
  title,
  message,
  onConfirm,
  onCancel,
}: {
  open: boolean;
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <Dialog open={open} onClose={onCancel}>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText>{message}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <SoftButton variant="text" onClick={onCancel}>
          Cancel
        </SoftButton>
        <SoftButton variant="gradient" color="dark" onClick={onConfirm}>
          Confirm
        </SoftButton>
      </DialogActions>
    </Dialog>
  );
}

function QuickActionCard({
  title,
  description,
  icon: Icon,
  onClick,
  highlighted,
}: {
  title: string;
  description: string;
  icon: React.ElementType;
  onClick: () => void;
  highlighted?: boolean;
}) {
  return (
    <Card
      sx={{
        p: 3,
        cursor: "pointer",
        transition: "all 0.2s",
        border: highlighted ? "2px solid #2563eb" : "1px solid #e2e8f0",
        background: highlighted ? "linear-gradient(135deg, #eff6ff 0%, #ffffff 100%)" : "transparent",
        "&:hover": {
          borderColor: "#2563eb",
          boxShadow: "0 4px 14px rgba(37, 99, 235, 0.12)",
          transform: "translateY(-2px)",
        },
      }}
      onClick={onClick}
    >
      <SoftBox
        component="span"
        bgColor={highlighted ? "dark" : "primary"}
        borderRadius="lg"
        p={1.5}
        display="flex"
        sx={{
          mb: 2,
          color: highlighted ? "#fff" : "#00274c",
          boxShadow: "0 3px 10px rgba(0, 39, 76, 0.12)",
        }}
      >
        <Icon size={22} color={highlighted ? "#fff" : "#00274c"} />
      </SoftBox>
      <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 0.5 }}>
        {title}
      </SoftTypography>
      <SoftTypography variant="button" color="text" fontWeight="regular">
        {description}
      </SoftTypography>
    </Card>
  );
}

export const MerchantDashboard: React.FC = () => {
  const { user } = useAuth();
  const { tab = "overview", id: detailId } = useParams();
  const activeTab: Tab = Object.prototype.hasOwnProperty.call(headings, tab)
    ? (tab as Tab)
    : "overview";
  const navigate = useNavigate();
  const base = "/merchant/dashboard";

  const approvalStatus = user?.merchantApprovalStatus ?? undefined;
  const isApproved = approvalStatus === "APPROVED";
  const isPending =
    approvalStatus === "PENDING" || !approvalStatus;

  const go = (next: Tab, param?: string) => {
    const path = param ? `${base}/${next}/${param}` : `${base}/${next}`;
    navigate(path);
  };

  // ── Shared state ──
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [shipmentsLoaded, setShipmentsLoaded] = useState(false);
  const [manifests] = useState<ManifestOrder[]>(demoManifestOrders);
  const [orders] = useState<ManifestItem[]>(demoManifestItems);

  // ── Shipments tab state ──
  const [shipQuery, setShipQuery] = useState("");
  const [shipStatus, setShipStatus] = useState("All");

  // ── Create shipment form state ──
  const [formStep, setFormStep] = useState(0);
  const [formReceiver, setFormReceiver] = useState({
    name: "",
    phone: "",
    email: "",
    address: "",
    city: "",
    postalCode: "",
  });
  const [formParcel, setFormParcel] = useState({
    description: "",
    category: "",
    quantity: 1,
    weight: "",
    length: "",
    width: "",
    height: "",
    specialHandling: "",
  });
  const [formDelivery, setFormDelivery] = useState({
    pickupDate: "",
    preferredTime: "",
    deliveryType: "Standard" as "Standard" | "Express",
    instructions: "",
  });
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  // ── Tracking state ──
  const [trackQuery, setTrackQuery] = useState("");
  const [trackedShipment, setTrackedShipment] = useState<Shipment | null>(null);

  // ── History state ──
  const [historyStatus, setHistoryStatus] = useState("All");
  const [historyQuery, setHistoryQuery] = useState("");

  // ── Notifications state ──
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [notifsLoaded, setNotifsLoaded] = useState(false);

  // ── API state ──
  const [copiedKey, setCopiedKey] = useState(false);
  const [apiCredentials, setApiCredentials] = useState<any>(null);

  // ── Bulk upload state ──
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadResult, setUploadResult] = useState<any>(null);
  const [uploading, setUploading] = useState(false);

  // ── Confirm dialog state ──
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmAction, setConfirmAction] = useState<() => void>(() => {});
  const [confirmTitle, setConfirmTitle] = useState("");
  const [confirmMessage, setConfirmMessage] = useState("");

  // ── Load data ──
  useEffect(() => {
    if (!shipmentsLoaded) {
      shipmentService.listShipments().then((data) => {
        setShipments(data);
        setShipmentsLoaded(true);
      });
    }
  }, [shipmentsLoaded]);

  useEffect(() => {
    if (!notifsLoaded) {
      notificationService.getNotifications().then((data) => {
        setNotifications(data);
        setNotifsLoaded(true);
      });
    }
  }, [notifsLoaded]);

  useEffect(() => {
    if (activeTab === "api" && !apiCredentials) {
      integrationService.getCredentials().then((data) => {
        setApiCredentials(data);
      });
    }
  }, [activeTab, apiCredentials]);

  // ── Computed ──
  const activeShipments = shipments.filter(
    (s) =>
      s.status === "In transit" ||
      s.status === "Picked up" ||
      s.status === "Out for delivery" ||
      s.status === "Assigned",
  );
  const deliveredShipments = shipments.filter(
    (s) => s.status === "Delivered",
  );
  const pendingShipments = shipments.filter((s) => s.status === "Pending");
  const failedShipments = shipments.filter(
    (s) => s.status === "Failed" || s.status === "Cancelled" || s.status === "Returned",
  );
  const totalSpend = shipments
    .filter((s) => s.status !== "Failed")
    .reduce((sum, s) => sum + s.amount, 0);

  const filteredShipments = shipments.filter(
    (s) =>
      (shipStatus === "All" || s.status === shipStatus) &&
      `${s.trackingNumber} ${s.receiver.name} ${s.receiver.city}`
        .toLowerCase()
        .includes(shipQuery.trim().toLowerCase()),
  );

  const historyShipments = shipments.filter(
    (s) =>
      (historyStatus === "All" ||
        (historyStatus === "Delivered" && s.status === "Delivered") ||
        (historyStatus === "Failed" &&
          (s.status === "Failed" || s.status === "Cancelled" || s.status === "Returned"))) &&
      `${s.trackingNumber} ${s.receiver.name} ${s.receiver.city}`
        .toLowerCase()
        .includes(historyQuery.trim().toLowerCase()),
  );

  const charts = useMemo(() => {
    const now = Date.now();
    const weeks = [3, 2, 1, 0].map((week) =>
      shipments.filter((s) => {
        const days = (now - new Date(s.createdAt).getTime()) / 86400000;
        return days >= week * 7 && days < (week + 1) * 7;
      }),
    );
    const labels = ["3 weeks ago", "2 weeks ago", "Last week", "This week"];
    return {
      bar: {
        labels,
        datasets: { label: "Shipments", data: weeks.map((w) => w.length) },
      },
      line: {
        labels,
        datasets: [
          { label: "Created", color: "primary", data: weeks.map((w) => w.length) },
          {
            label: "Delivered",
            color: "info",
            data: weeks.map(
              (w) => w.filter((s) => s.status === "Delivered").length,
            ),
          },
        ],
      },
    };
  }, [shipments]);

  // ── Handlers ──
  const showConfirm = (title: string, message: string, action: () => void) => {
    setConfirmTitle(title);
    setConfirmMessage(message);
    setConfirmAction(action);
    setConfirmOpen(true);
  };


  const handleTrack = async () => {
    if (!trackQuery.trim()) return;
    setLoading(true);
    try {
      const result = await trackingService.trackShipment(trackQuery.trim());
      setTrackedShipment(result);
    } catch {
      setTrackedShipment(null);
    } finally {
      setLoading(false);
    }
  };

  const handleBulkUpload = async () => {
    if (!uploadFile) return;
    setUploading(true);
    try {
      const result = await bulkUploadService.uploadManifest(uploadFile);
      setUploadResult(result);
      setMessage(`Upload complete: ${result.validRecords} valid, ${result.invalidRecords} invalid records.`);
    } catch {
      setMessage("Upload failed. Please try again.");
    } finally {
      setUploading(false);
    }
  };

  const handleCopyApiKey = () => {
    const key = apiCredentials?.apiKey || "sd_live_9a8f21b74c8e3921049281a";
    navigator.clipboard.writeText(key);
    setCopiedKey(true);
    setTimeout(() => setCopiedKey(false), 2000);
  };

  const handleExportCsv = (items: ManifestItem[] | Shipment[]) => {
    const cell = (value: string | number) => {
      let text = String(value);
      if (/^[\s]*[=+@-]/.test(text)) text = `'${text}`;
      return `"${text.replace(/"/g, '""')}"`;
    };
    const headers = ["Tracking", "Recipient", "Destination", "Status", "Date", "Amount (LKR)"];
    const rows = items.map((item: any) => [
      item.trackingNumber,
      item.recipient || item.receiver?.name,
      item.destination || item.receiver?.city,
      item.status,
      item.bookedAt || item.createdAt,
      item.amount,
    ]);
    const csv = [headers, ...rows]
      .map((row) => row.map(cell).join(","))
      .join("\r\n");
    const url = URL.createObjectURL(
      new Blob(["\uFEFF", csv], { type: "text/csv;charset=utf-8;" }),
    );
    const link = document.createElement("a");
    link.href = url;
    link.download = "swiftdrop-merchant-export.csv";
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    setMessage("Records exported.");
  };

  const unreadNotifs = notifications.filter((n) => !n.read).length;

  // ── Shipment table renderer ──
  const renderShipmentTable = (items: Shipment[]) =>
    !items.length ? (
      <EmptyState
        title="No shipments found"
        text="Try a different search or create a new shipment."
      />
    ) : (
      <Table
        columns={[
          { name: "shipment", align: "left" },
          { name: "destination", align: "left" },
          { name: "status", align: "center" },
          { name: "amount", align: "right" },
          { name: "action", align: "right" },
        ]}
        rows={items.map((s) => ({
          id: s.trackingNumber,
          hasBorder: true,
          shipment: (
            <Box
              component="span"
              sx={{ display: "flex", alignItems: "center", gap: 1.5, px: 2 }}
            >
              <SoftBox
                component="span"
                bgColor="primary"
                borderRadius="lg"
                p={1}
                display="flex"
                sx={{
                  color: "#00274c",
                  boxShadow: "0 3px 10px rgba(0, 39, 76, 0.12)",
                }}
              >
                <Package size={18} color="#00274c" />
              </SoftBox>
              <Box component="span">
                <SoftTypography
                  component="span"
                  display="block"
                  variant="button"
                  fontWeight="bold"
                >
                  {s.trackingNumber}
                </SoftTypography>
                <SoftTypography
                  component="span"
                  display="block"
                  variant="caption"
                  color="text"
                >
                  {s.receiver.name}
                </SoftTypography>
              </Box>
            </Box>
          ),
          destination: (
            <Box component="span">
              <SoftTypography component="span" variant="button" display="block">
                {s.receiver.city}
              </SoftTypography>
              <SoftTypography component="span" variant="caption" color="text">
                {dateLabel(s.createdAt)} · {s.delivery.deliveryType}
              </SoftTypography>
            </Box>
          ),
          status: <ShipmentStatusBadge status={s.status} />,
          amount: currency(s.amount),
          action: (
            <SoftButton
              variant="text"
              color="dark"
              size="small"
              onClick={() => go("shipment-detail", s.id)}
              sx={{ fontWeight: "bold" }}
            >
              Details <ArrowRight size={13} style={{ marginLeft: 5 }} />
            </SoftButton>
          ),
        }))}
      />
    );

  // ── Detail shipment lookup ──
  const detailShipment = detailId
    ? shipments.find((s) => s.id === detailId || s.trackingNumber === detailId)
    : undefined;

  // ═══════════════════════════════════════════════════════════
  //  TAB RENDERERS
  // ═══════════════════════════════════════════════════════════

  function renderOverview() {
    return (
      <Grid container spacing={3}>
        {/* Stats */}
        {[
          { title: "Total Shipments", count: shipments.length, icon: "inventory_2", color: "primary" as const },
          { title: "Pending", count: pendingShipments.length, icon: "schedule", color: "warning" as const },
          { title: "In Transit", count: activeShipments.length, icon: "local_shipping", color: "info" as const },
          { title: "Delivered", count: deliveredShipments.length, icon: "task_alt", color: "success" as const },
          { title: "Failed", count: failedShipments.length, icon: "cancel", color: "error" as const },
          { title: "Total Revenue", count: currency(totalSpend), icon: "account_balance_wallet", color: "dark" as const },
        ].map(({ title, count, icon, color }) => (
          <Grid item xs={12} sm={6} xl={3} key={title}>
            <MiniStatisticsCard
              title={{ text: title, fontWeight: "medium" }}
              count={count}
              percentage={{ color: "secondary", text: "" }}
              icon={{ color, component: icon }}
            />
          </Grid>
        ))}

        {/* Welcome Card */}
        <Grid item xs={12} lg={7}>
          <Card sx={{ height: "100%", p: 2 }}>
            <Grid container spacing={2} sx={{ height: "100%" }}>
              <Grid item xs={12} sm={7}>
                <Box
                  sx={{
                    p: 1.5,
                    display: "flex",
                    flexDirection: "column",
                    height: "100%",
                  }}
                >
                  <SoftTypography variant="button" color="text" fontWeight="medium">
                    Built for your business
                  </SoftTypography>
                  <SoftTypography variant="h4" fontWeight="bold" mt={1}>
                    Welcome back, {user?.fullName?.split(" ")[0] || "Merchant"}.
                  </SoftTypography>
                  <SoftTypography variant="body2" color="text" mt={1} mb={3}>
                    Send parcels, track deliveries, and keep your store's
                    logistics running smoothly with SwiftDrop.
                  </SoftTypography>
                  <Box sx={{ display: "flex", gap: 1.5, mt: "auto", flexWrap: "wrap" }}>
                    <SoftButton
                      variant="gradient"
                      color="primary"
                      onClick={() => go("create-shipment")}
                      disabled={!isApproved}
                      sx={{
                        color: "#00274c !important",
                        fontWeight: "bold",
                        px: 3,
                        py: 1.2,
                        fontSize: "1rem",
                        boxShadow: "0 6px 20px rgba(0, 39, 76, 0.2)",
                        borderRadius: 3,
                      }}
                    >
                      <Plus size={18} style={{ marginRight: 8 }} />
                      Send a parcel
                    </SoftButton>
                    <SoftButton
                      variant="outlined"
                      color="dark"
                      onClick={() => go("shipments")}
                      sx={{ fontWeight: "bold" }}
                    >
                      View all shipments
                    </SoftButton>
                  </Box>
                </Box>
              </Grid>
              <Grid item xs={12} sm={5}>
                <SoftBox
                  variant="gradient"
                  bgColor="primary"
                  borderRadius="lg"
                  sx={{
                    height: "100%",
                    minHeight: 200,
                    display: "grid",
                    placeItems: "center",
                    overflow: "hidden",
                    boxShadow: "0 8px 24px rgba(0, 39, 76, 0.12)",
                  }}
                >
                  <Box
                    sx={{
                      display: "flex",
                      flexDirection: "column",
                      alignItems: "center",
                      gap: 1,
                      color: "#fff",
                    }}
                  >
                    <Building2 size={64} style={{ opacity: 0.25 }} />
                    <SoftTypography
                      variant="button"
                      color="white"
                      fontWeight="bold"
                      sx={{ opacity: 0.7 }}
                    >
                      Merchant Portal
                    </SoftTypography>
                  </Box>
                </SoftBox>
              </Grid>
            </Grid>
          </Card>
        </Grid>

        {/* Gradient Info Card */}
        <Grid item xs={12} lg={5}>
          <Card sx={{ p: 2, height: "100%" }}>
            <Box
              sx={{
                borderRadius: 3,
                p: 3,
                minHeight: 250,
                height: "100%",
                backgroundImage: `linear-gradient(310deg, rgba(0,39,76,.95), rgba(17,41,107,.75)), url(${landscape})`,
                backgroundSize: "cover",
                display: "flex",
                flexDirection: "column",
                boxShadow: "0 8px 24px rgba(0, 39, 76, 0.12)",
              }}
            >
              <SoftTypography variant="h5" fontWeight="bold" color="white">
                Send your next parcel in seconds.
              </SoftTypography>
              <SoftTypography
                variant="body2"
                color="white"
                opacity={0.85}
                mt={2}
                mb={3}
              >
                {activeShipments.length} parcels on the way right now.
                Tap "Send a parcel" to get started.
              </SoftTypography>
              <SoftButton
                variant="gradient"
                color="primary"
                onClick={() => go("create-shipment")}
                disabled={!isApproved}
                sx={{
                  mt: "auto",
                  alignSelf: "flex-start",
                  color: "#00274c !important",
                  fontWeight: "bold",
                  px: 2.5,
                  boxShadow: "0 4px 14px rgba(0, 39, 76, 0.14)",
                }}
              >
                Send a parcel{" "}
                <ArrowRight size={16} style={{ marginLeft: 8 }} />
              </SoftButton>
            </Box>
          </Card>
        </Grid>

        {/* Bar Chart */}
        <Grid item xs={12} lg={5}>
          <ReportsBarChart
            title="Shipment activity"
            description="Shipments created across the last four weeks"
            chart={charts.bar}
            items={[
              {
                icon: { color: "primary", component: "inventory_2" },
                label: "Total",
                progress: { content: String(shipments.length), percentage: 100 },
              },
              {
                icon: { color: "success", component: "task_alt" },
                label: "Delivered",
                progress: {
                  content: String(deliveredShipments.length),
                  percentage: shipments.length
                    ? (deliveredShipments.length / shipments.length) * 100
                    : 0,
                },
              },
              {
                icon: { color: "info", component: "local_shipping" },
                label: "Active",
                progress: {
                  content: String(activeShipments.length),
                  percentage: shipments.length
                    ? (activeShipments.length / shipments.length) * 100
                    : 0,
                },
              },
              {
                icon: { color: "warning", component: "upload_file" },
                label: "Manifests",
                progress: { content: String(manifests.length), percentage: 100 },
              },
            ]}
          />
        </Grid>

        {/* Line Chart */}
        <Grid item xs={12} lg={7}>
          <GradientLineChart
            title="Delivery overview"
            description="Created shipments and their delivered status, grouped by week"
            height="17rem"
            chart={charts.line}
          />
        </Grid>

        {/* Recent Shipments Table */}
        <Grid item xs={12} lg={8}>
          <Card sx={{ height: "100%", overflow: "hidden" }}>
            <Box
              sx={{
                p: 3,
                display: "flex",
                justifyContent: "space-between",
                gap: 2,
                alignItems: "center",
              }}
            >
              <Box>
                <SoftTypography variant="h6" fontWeight="bold">
                  Recent parcels
                </SoftTypography>
                <SoftTypography variant="button" color="text" fontWeight="regular">
                  Your latest shipments
                </SoftTypography>
              </Box>
              <SoftButton
                size="small"
                variant="text"
                color="dark"
                onClick={() => go("shipments")}
                sx={{ fontWeight: "bold" }}
              >
                View all
              </SoftButton>
            </Box>
            {renderShipmentTable(shipments.slice(0, 5))}
          </Card>
        </Grid>

        {/* Timeline */}
        <Grid item xs={12} lg={4}>
          <TimelineList title="Recent activity">
            {shipments.slice(0, 4).map((s, index) => (
              <TimelineItem
                key={s.id}
                color={shipmentStatusColor(s.status)}
                icon={s.status === "Delivered" ? "task_alt" : "local_shipping"}
                title={`${s.trackingNumber} · ${s.status}`}
                dateTime={`Created ${dateLabel(s.createdAt)}`}
                description={s.receiver.city}
                lastItem={index === Math.min(shipments.length, 4) - 1}
              />
            ))}
          </TimelineList>
        </Grid>

        {/* Quick Actions */}
        <Grid item xs={12}>
          <SoftTypography variant="h5" fontWeight="bold" sx={{ mb: 2 }}>
            What would you like to do?
          </SoftTypography>
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <QuickActionCard
            title="Send a parcel"
            description="Create a new shipment for your customer."
            icon={Send}
            onClick={isApproved ? () => go("create-shipment") : () => {}}
            highlighted
          />
        </Grid>
        {[
          {
            title: "Upload many parcels",
            description: "Add multiple shipments from a CSV file.",
            icon: Upload,
            action: () => go("bulk-upload"),
          },
          {
            title: "Track a parcel",
            description: "See where your shipment is right now.",
            icon: Search,
            action: () => go("tracking"),
          },
          {
            title: "Past deliveries",
            description: "View your completed and failed deliveries.",
            icon: Clock,
            action: () => go("history"),
          },
        ].map(({ title, description, icon, action }) => (
          <Grid item xs={12} sm={6} lg={3} key={title}>
            <QuickActionCard
              title={title}
              description={description}
              icon={icon}
              onClick={isApproved ? action : () => {}}
            />
          </Grid>
        ))}
      </Grid>
    );
  }

  function renderShipments() {
    return (
      <Card sx={{ overflow: "hidden" }}>
        <Box sx={{ p: 3 }}>
          <Box
            sx={{
              display: "flex",
              justifyContent: "space-between",
              gap: 2,
              flexWrap: "wrap",
              mb: 3,
            }}
          >
            <Box>
              <SoftTypography variant="h6" fontWeight="bold">
                My Shipments
              </SoftTypography>
              <SoftTypography variant="button" color="text" fontWeight="regular">
                {filteredShipments.length} parcels found
              </SoftTypography>
            </Box>
            <Box sx={{ display: "flex", gap: 1 }}>
              <SoftButton
                variant="gradient"
                color="primary"
                onClick={() => go("create-shipment")}
                disabled={!isApproved}
                sx={{ fontWeight: "bold", color: "#00274c !important" }}
              >
                <Plus size={14} style={{ marginRight: 6 }} />
                Send a parcel
              </SoftButton>
              <SoftButton
                variant="outlined"
                color="dark"
                onClick={() => handleExportCsv(filteredShipments)}
                disabled={!filteredShipments.length}
              >
                <Download size={15} style={{ marginRight: 8 }} />
                Export
              </SoftButton>
            </Box>
          </Box>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={8}>
              <SoftTypography
                component="label"
                htmlFor="ship-search"
                variant="caption"
                fontWeight="bold"
                display="block"
                mb={1}
              >
                Search shipments
              </SoftTypography>
              <SoftInput
                id="ship-search"
                fullWidth
                value={shipQuery}
                placeholder="Tracking number, receiver name or city"
                onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                  setShipQuery(e.target.value)
                }
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <SoftTypography
                component="label"
                htmlFor="ship-status-filter"
                variant="caption"
                fontWeight="bold"
                display="block"
                mb={1}
              >
                Filter by status
              </SoftTypography>
              <SoftInput
                id="ship-status-filter"
                fullWidth
                value={shipStatus}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                  setShipStatus(e.target.value)
                }
                select
              >
                <option value="All">All statuses</option>
                <option value="Pending">Pending</option>
                <option value="Assigned">Assigned</option>
                <option value="Picked up">Picked up</option>
                <option value="In transit">In transit</option>
                <option value="Out for delivery">Out for delivery</option>
                <option value="Delivered">Delivered</option>
                <option value="Failed">Failed</option>
                <option value="Cancelled">Cancelled</option>
              </SoftInput>
            </Grid>
          </Grid>
        </Box>
        {renderShipmentTable(filteredShipments)}
      </Card>
    );
  }

  /* ── Success state after creating a shipment ── */
  const [justCreated, setJustCreated] = useState<string | null>(null);

  function renderCreateShipment() {
    /* ── Locked state for unapproved merchants ── */
    if (!isApproved) {
      return (
        <Card sx={{ p: 4 }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <AlertTriangle size={24} color="#f59e0b" />
            <Box>
              <SoftTypography variant="h6" fontWeight="bold" sx={{ color: "#92400e" }}>
                You can't create shipments yet
              </SoftTypography>
              <SoftTypography variant="body2" sx={{ color: "#a16207" }}>
                Your store account is waiting for approval. Once approved, you'll be able to send parcels right away.
              </SoftTypography>
            </Box>
          </Box>
        </Card>
      );
    }

    /* ── Success screen after submission ── */
    if (justCreated) {
      return (
        <Card sx={{ p: { xs: 3, md: 6 }, textAlign: "center" }}>
          <SoftBox
            variant="gradient"
            bgColor="success"
            borderRadius="lg"
            p={2}
            mx="auto"
            mb={3}
            color="white"
            width="fit-content"
          >
            <Check size={36} color="#fff" />
          </SoftBox>
          <SoftTypography variant="h4" fontWeight="bold">
            Shipment created!
          </SoftTypography>
          <SoftTypography variant="body1" color="text" mt={2}>
            Your parcel <strong>{justCreated}</strong> is all set. We'll pick it up and deliver it to your customer.
          </SoftTypography>
          <Box
            sx={{
              display: "flex",
              flexWrap: "wrap",
              justifyContent: "center",
              gap: 2,
              mt: 4,
            }}
          >
            <SoftButton
              variant="gradient"
              color="primary"
              onClick={() => {
                setJustCreated(null);
                go("tracking");
              }}
              sx={{ color: "#00274c !important", fontWeight: "bold" }}
            >
              <Search size={14} style={{ marginRight: 6 }} />
              Track this shipment
            </SoftButton>
            <SoftButton
              variant="gradient"
              color="dark"
              onClick={() => {
                setJustCreated(null);
                setFormStep(0);
                setFormReceiver({ name: "", phone: "", email: "", address: "", city: "", postalCode: "" });
                setFormParcel({ description: "", category: "", quantity: 1, weight: "", length: "", width: "", height: "", specialHandling: "" });
                setFormDelivery({ pickupDate: "", preferredTime: "", deliveryType: "Standard", instructions: "" });
              }}
              sx={{ color: "#00274c !important", fontWeight: "bold" }}
            >
              <Plus size={14} style={{ marginRight: 6 }} />
              Send another
            </SoftButton>
            <SoftButton
              variant="outlined"
              color="dark"
              onClick={() => {
                setJustCreated(null);
                go("shipments");
              }}
              sx={{ fontWeight: "bold" }}
            >
              View all shipments
            </SoftButton>
          </Box>
        </Card>
      );
    }

    /* ── Helper: a single form field with label, hint, and error ── */
    const Field = ({
      id,
      label,
      hint,
      error,
      ...props
    }: {
      id: string;
      label: string;
      hint?: string;
      error?: string;
      [key: string]: unknown;
    }) => (
      <Box>
        <SoftTypography
          component="label"
          htmlFor={id}
          variant="caption"
          fontWeight="bold"
          display="block"
          mb={1}
        >
          {label}
        </SoftTypography>
        <SoftInput id={id} fullWidth error={!!error} {...props} />
        {hint && !error && (
          <SoftTypography variant="caption" color="text" display="block" mt={0.5}>
            {hint}
          </SoftTypography>
        )}
        {error && (
          <SoftTypography variant="caption" sx={{ color: "#dc2626" }} display="block" mt={0.5}>
            {error}
          </SoftTypography>
        )}
      </Box>
    );

    /* ── Step labels (simple language) ── */
    const stepLabels = ["Who receives it?", "What are you sending?", "Delivery details", "Review & send"];

    /* ── Estimated charge calculation ── */
    const estimatedCharge =
      formParcel.weight && parseFloat(formParcel.weight) > 0
        ? Math.round(parseFloat(formParcel.weight) * 150 + 250) +
          (formDelivery.deliveryType === "Express" ? 350 : 0)
        : 0;

    return (
      <Grid container spacing={3}>
        {/* ── Main form card ── */}
        <Grid item xs={12} lg={8}>
          <Card sx={{ p: 3 }}>
            {/* Stepper */}
            <Stepper activeStep={formStep} alternativeLabel>
              {stepLabels.map((label) => (
                <Step key={label}>
                  <StepLabel>{label}</StepLabel>
                </Step>
              ))}
            </Stepper>

            <Box sx={{ mt: 4 }}>
              {/* ═══ Step 0: Receiver ═══ */}
              {formStep === 0 && (
                <Grid container spacing={2.5}>
                  <Grid item xs={12}>
                    <SoftTypography variant="subtitle2" fontWeight="bold" color="text" sx={{ mb: 0.5 }}>
                      Customer details
                    </SoftTypography>
                    <SoftTypography variant="body2" color="text" fontWeight="regular" sx={{ mb: 2 }}>
                      Tell us who will receive the parcel.
                    </SoftTypography>
                  </Grid>
                  <Grid item xs={12}>
                    <Field
                      id="recv-name"
                      label="Customer's full name"
                      hint="e.g. John Perera"
                      error={formErrors.receiverName}
                      placeholder="Customer's full name"
                      value={formReceiver.name}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormReceiver((p) => ({ ...p, name: e.target.value }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Field
                      id="recv-phone"
                      label="Phone number"
                      hint="e.g. 077 123 4567"
                      error={formErrors.receiverPhone}
                      placeholder="Phone number"
                      value={formReceiver.phone}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormReceiver((p) => ({ ...p, phone: e.target.value }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Field
                      id="recv-email"
                      label="Email (optional)"
                      hint="Helpful for delivery notifications"
                      placeholder="Email address"
                      value={formReceiver.email}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormReceiver((p) => ({ ...p, email: e.target.value }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <Field
                      id="recv-address"
                      label="Delivery address"
                      hint="Street address, building, floor"
                      error={formErrors.receiverAddress}
                      placeholder="Full delivery address"
                      value={formReceiver.address}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormReceiver((p) => ({ ...p, address: e.target.value }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Field
                      id="recv-city"
                      label="City"
                      error={formErrors.receiverCity}
                      placeholder="City"
                      value={formReceiver.city}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormReceiver((p) => ({ ...p, city: e.target.value }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Field
                      id="recv-postal"
                      label="Postal code (optional)"
                      hint="e.g. 00300"
                      placeholder="Postal code"
                      value={formReceiver.postalCode}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormReceiver((p) => ({ ...p, postalCode: e.target.value }))
                      }
                    />
                  </Grid>
                </Grid>
              )}

              {/* ═══ Step 1: Parcel ═══ */}
              {formStep === 1 && (
                <Grid container spacing={2.5}>
                  <Grid item xs={12}>
                    <SoftTypography variant="subtitle2" fontWeight="bold" color="text" sx={{ mb: 0.5 }}>
                      Parcel details
                    </SoftTypography>
                    <SoftTypography variant="body2" color="text" fontWeight="regular" sx={{ mb: 2 }}>
                      What's inside the parcel?
                    </SoftTypography>
                  </Grid>
                  <Grid item xs={12}>
                    <Field
                      id="parcel-desc"
                      label="What's inside?"
                      hint="e.g. Men's t-shirt, size M"
                      error={formErrors.parcelDesc}
                      placeholder="Brief description of the item"
                      value={formParcel.description}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormParcel((p) => ({ ...p, description: e.target.value }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Field
                      id="parcel-cat"
                      label="Category"
                      hint="e.g. Clothing, Electronics, Food"
                      error={formErrors.parcelCat}
                      placeholder="Category"
                      value={formParcel.category}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormParcel((p) => ({ ...p, category: e.target.value }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Field
                      id="parcel-qty"
                      label="Quantity"
                      hint="How many items?"
                      type="number"
                      placeholder="1"
                      value={formParcel.quantity}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormParcel((p) => ({ ...p, quantity: parseInt(e.target.value) || 1 }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Field
                      id="parcel-weight"
                      label="Weight (kg)"
                      hint="e.g. 1.5"
                      error={formErrors.parcelWeight}
                      type="number"
                      placeholder="Weight in kilograms"
                      value={formParcel.weight}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormParcel((p) => ({ ...p, weight: e.target.value }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <SoftTypography variant="caption" color="text" fontWeight="regular" display="block" mb={2}>
                      Dimensions are optional — they help us pack your parcel better.
                    </SoftTypography>
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <Field
                      id="parcel-length"
                      label="Length (cm)"
                      type="number"
                      placeholder="Length"
                      value={formParcel.length}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormParcel((p) => ({ ...p, length: e.target.value }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <Field
                      id="parcel-width"
                      label="Width (cm)"
                      type="number"
                      placeholder="Width"
                      value={formParcel.width}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormParcel((p) => ({ ...p, width: e.target.value }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <Field
                      id="parcel-height"
                      label="Height (cm)"
                      type="number"
                      placeholder="Height"
                      value={formParcel.height}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormParcel((p) => ({ ...p, height: e.target.value }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <Field
                      id="parcel-handling"
                      label="Special handling (optional)"
                      hint="e.g. Fragile, keep upright, do not bend"
                      multiline
                      rows={2}
                      placeholder="Any special instructions for handling"
                      value={formParcel.specialHandling}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormParcel((p) => ({ ...p, specialHandling: e.target.value }))
                      }
                    />
                  </Grid>
                </Grid>
              )}

              {/* ═══ Step 2: Delivery ═══ */}
              {formStep === 2 && (
                <Grid container spacing={2.5}>
                  <Grid item xs={12}>
                    <SoftTypography variant="subtitle2" fontWeight="bold" color="text" sx={{ mb: 0.5 }}>
                      When should we pick it up?
                    </SoftTypography>
                    <SoftTypography variant="body2" color="text" fontWeight="regular" sx={{ mb: 2 }}>
                      Choose a pickup date and any extra delivery preferences.
                    </SoftTypography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Field
                      id="del-pickup"
                      label="Pickup date"
                      error={formErrors.pickupDate}
                      type="date"
                      value={formDelivery.pickupDate}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormDelivery((p) => ({ ...p, pickupDate: e.target.value }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Field
                      id="del-time"
                      label="Preferred time (optional)"
                      hint="e.g. Morning, Afternoon, 2 PM"
                      placeholder="Preferred pickup time"
                      value={formDelivery.preferredTime}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormDelivery((p) => ({ ...p, preferredTime: e.target.value }))
                      }
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <SoftTypography
                      component="label"
                      htmlFor="del-type"
                      variant="caption"
                      fontWeight="bold"
                      display="block"
                      mb={1}
                    >
                      Delivery speed
                    </SoftTypography>
                    <SoftInput
                      id="del-type"
                      fullWidth
                      value={formDelivery.deliveryType}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormDelivery((p) => ({ ...p, deliveryType: e.target.value as any }))
                      }
                      select
                    >
                      <option value="Standard">Standard — 2-3 business days</option>
                      <option value="Express">Express — Next business day (+LKR 350)</option>
                    </SoftInput>
                  </Grid>
                  <Grid item xs={12}>
                    <Field
                      id="del-instructions"
                      label="Extra instructions (optional)"
                      hint="e.g. Leave at reception, call before delivery"
                      multiline
                      rows={2}
                      placeholder="Any notes for the courier"
                      value={formDelivery.instructions}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setFormDelivery((p) => ({ ...p, instructions: e.target.value }))
                      }
                    />
                  </Grid>
                </Grid>
              )}

              {/* ═══ Step 3: Review & Send ═══ */}
              {formStep === 3 && (
                <Grid container spacing={3}>
                  <Grid item xs={12}>
                    <SoftTypography variant="subtitle2" fontWeight="bold" color="text" sx={{ mb: 0.5 }}>
                      Looks good?
                    </SoftTypography>
                    <SoftTypography variant="body2" color="text" fontWeight="regular" sx={{ mb: 2 }}>
                      Review the details below before sending your shipment.
                    </SoftTypography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Card sx={{ p: 2, bgcolor: "#f8fafc" }}>
                      <SoftTypography variant="subtitle2" fontWeight="bold" sx={{ mb: 2 }}>
                        📦 From — Your store
                      </SoftTypography>
                      <DetailRow label="Business" value="SwiftDrop Demo Store" />
                      <DetailRow label="Phone" value="+94 77 123 4567" />
                      <DetailRow label="Address" value="123 Galle Road, Colombo 00300" />
                    </Card>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Card sx={{ p: 2, bgcolor: "#f8fafc" }}>
                      <SoftTypography variant="subtitle2" fontWeight="bold" sx={{ mb: 2 }}>
                        🏠 To — Customer
                      </SoftTypography>
                      <DetailRow label="Name" value={formReceiver.name} />
                      <DetailRow label="Phone" value={formReceiver.phone} />
                      <DetailRow
                        label="Address"
                        value={`${formReceiver.address}, ${formReceiver.city}${formReceiver.postalCode ? ` ${formReceiver.postalCode}` : ""}`}
                      />
                    </Card>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Card sx={{ p: 2, bgcolor: "#f8fafc" }}>
                      <SoftTypography variant="subtitle2" fontWeight="bold" sx={{ mb: 2 }}>
                        📋 Parcel
                      </SoftTypography>
                      <DetailRow label="Item" value={formParcel.description} />
                      <DetailRow label="Category" value={formParcel.category} />
                      <DetailRow label="Weight" value={`${formParcel.weight} kg`} />
                      <DetailRow label="Quantity" value={String(formParcel.quantity)} />
                    </Card>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Card sx={{ p: 2, bgcolor: "#f8fafc" }}>
                      <SoftTypography variant="subtitle2" fontWeight="bold" sx={{ mb: 2 }}>
                        🚚 Delivery
                      </SoftTypography>
                      <DetailRow label="Pickup date" value={formDelivery.pickupDate} />
                      <DetailRow label="Speed" value={formDelivery.deliveryType} />
                      {formDelivery.preferredTime && (
                        <DetailRow label="Preferred time" value={formDelivery.preferredTime} />
                      )}
                      <DetailRow label="Est. charge" value={currency(estimatedCharge)} />
                    </Card>
                  </Grid>
                </Grid>
              )}
            </Box>

            {/* ── Navigation buttons ── */}
            <Box
              sx={{
                display: "flex",
                justifyContent: "space-between",
                mt: 4,
                pt: 2,
                borderTop: "1px solid #e2e8f0",
              }}
            >
              <SoftButton
                variant="text"
                color="dark"
                onClick={() => {
                  if (formStep === 0) {
                    go("overview");
                  } else {
                    setFormStep((s) => Math.max(0, s - 1));
                  }
                }}
              >
                {formStep === 0 ? "Cancel" : "Back"}
              </SoftButton>
              <Box sx={{ display: "flex", gap: 1 }}>
                {formStep < 3 ? (
                  <SoftButton
                    variant="gradient"
                    color="primary"
                    onClick={() => setFormStep((s) => Math.min(3, s + 1))}
                    sx={{ color: "#00274c !important", fontWeight: "bold" }}
                  >
                    Continue <ChevronRight size={14} style={{ marginLeft: 4 }} />
                  </SoftButton>
                ) : (
                  <>
                    <SoftButton
                      variant="text"
                      onClick={() => {
                        setFormStep(0);
                        setFormReceiver({ name: "", phone: "", email: "", address: "", city: "", postalCode: "" });
                        setFormParcel({ description: "", category: "", quantity: 1, weight: "", length: "", width: "", height: "", specialHandling: "" });
                        setFormDelivery({ pickupDate: "", preferredTime: "", deliveryType: "Standard", instructions: "" });
                      }}
                    >
                      Start over
                    </SoftButton>
                    <SoftButton
                      variant="gradient"
                      color="primary"
                      onClick={async () => {
                        /* Validate */
                        const errors: Record<string, string> = {};
                        if (!formReceiver.name.trim()) errors.receiverName = "This is required";
                        if (!formReceiver.phone.trim()) errors.receiverPhone = "This is required";
                        if (!formReceiver.address.trim()) errors.receiverAddress = "This is required";
                        if (!formReceiver.city.trim()) errors.receiverCity = "This is required";
                        if (!formParcel.description.trim()) errors.parcelDesc = "This is required";
                        if (!formParcel.category.trim()) errors.parcelCat = "This is required";
                        if (!formParcel.weight || parseFloat(formParcel.weight) <= 0)
                          errors.parcelWeight = "Enter a valid weight";
                        if (!formDelivery.pickupDate) errors.pickupDate = "Pick a date";

                        if (Object.keys(errors).length) {
                          setFormErrors(errors);
                          /* Jump to the step with errors */
                          if (errors.receiverName || errors.receiverPhone || errors.receiverAddress || errors.receiverCity)
                            setFormStep(0);
                          else if (errors.parcelDesc || errors.parcelCat || errors.parcelWeight)
                            setFormStep(1);
                          else if (errors.pickupDate)
                            setFormStep(2);
                          return;
                        }
                        setFormErrors({});

                        setLoading(true);
                        try {
                          const payload: CreateShipmentPayload = {
                            receiver: formReceiver,
                            parcel: {
                              description: formParcel.description,
                              category: formParcel.category,
                              quantity: formParcel.quantity,
                              weight: parseFloat(formParcel.weight),
                              length: formParcel.length ? parseFloat(formParcel.length) : undefined,
                              width: formParcel.width ? parseFloat(formParcel.width) : undefined,
                              height: formParcel.height ? parseFloat(formParcel.height) : undefined,
                              specialHandling: formParcel.specialHandling || undefined,
                            },
                            delivery: formDelivery,
                          };
                          const created = await shipmentService.createShipment(payload);
                          setShipments((prev) => [created, ...prev]);
                          setJustCreated(created.trackingNumber);
                        } catch {
                          setMessage("Something went wrong. Please try again.");
                        } finally {
                          setLoading(false);
                        }
                      }}
                      disabled={loading}
                      sx={{ color: "#00274c !important", fontWeight: "bold" }}
                    >
                      {loading ? (
                        <>
                          <Loader2 size={14} style={{ marginRight: 6, animation: "spin 1s linear infinite" }} />
                          Creating...
                        </>
                      ) : (
                        <>
                          <Send size={14} style={{ marginRight: 6 }} />
                          Send shipment
                        </>
                      )}
                    </SoftButton>
                  </>
                )}
              </Box>
            </Box>
          </Card>
        </Grid>

        {/* ── Sidebar: Estimated cost + tips ── */}
        <Grid item xs={12} lg={4}>
          {/* Estimated cost card */}
          <SoftBox variant="gradient" bgColor="dark" borderRadius="lg" p={3} sx={{ mb: 3 }}>
            <SoftTypography variant="h6" color="white" fontWeight="bold">
              Estimated cost
            </SoftTypography>
            <SoftTypography variant="h3" fontWeight="bold" color="white" mt={2}>
              {estimatedCharge > 0 ? currency(estimatedCharge) : "—"}
            </SoftTypography>
            <SoftTypography variant="button" fontWeight="regular" color="white" opacity={0.8}>
              LKR 250 base + LKR 150 per kg. Express adds LKR 350.
            </SoftTypography>
            <SoftTypography variant="caption" color="white" display="block" mt={2} opacity={0.6}>
              Final charge may vary. No payment is collected yet.
            </SoftTypography>
          </SoftBox>

          {/* Tips card */}
          <Card sx={{ p: 3, mb: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              💡 Quick tips
            </SoftTypography>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
              {["Weigh your parcel before entering the weight.", "Add special handling for fragile items.", "Choose Express for urgent deliveries."].map((tip) => (
                <Box key={tip} sx={{ display: "flex", gap: 1, alignItems: "flex-start" }}>
                  <Check size={16} style={{ color: "#16a34a", marginTop: 2, flexShrink: 0 }} />
                  <SoftTypography variant="button" color="text" fontWeight="regular">
                    {tip}
                  </SoftTypography>
                </Box>
              ))}
            </Box>
          </Card>

          {/* Sender info card */}
          <Card sx={{ p: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              Your store
            </SoftTypography>
            <DetailRow label="Business" value="SwiftDrop Demo Store" />
            <DetailRow label="Phone" value="+94 77 123 4567" />
            <DetailRow label="Address" value="123 Galle Road, Colombo" />
          </Card>
        </Grid>
      </Grid>
    );
  }

  function renderShipmentDetail() {
    if (!detailShipment) {
      return (
        <EmptyState
          title="Shipment not found"
          text="The requested shipment could not be found."
          icon={Search}
        />
      );
    }
    const s = detailShipment;
    return (
      <Grid container spacing={3}>
        {/* Header */}
        <Grid item xs={12}>
          <Card sx={{ p: 3 }}>
            <Box
              sx={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                flexWrap: "wrap",
                gap: 2,
              }}
            >
              <Box>
                <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 1 }}>
                  <SoftTypography variant="h5" fontWeight="bold">
                    {s.trackingNumber}
                  </SoftTypography>
                  <ShipmentStatusBadge status={s.status} />
                </Box>
                <SoftTypography variant="body2" color="text">
                  Created {dateTimeLabel(s.createdAt)} · Est. delivery{" "}
                  {dateLabel(s.estimatedDelivery)}
                </SoftTypography>
              </Box>
              <Box sx={{ display: "flex", gap: 1 }}>
                <SoftButton
                  variant="outlined"
                  color="dark"
                  onClick={() => go("tracking")}
                  sx={{ fontWeight: "bold" }}
                >
                  <Search size={14} style={{ marginRight: 6 }} />
                  Track
                </SoftButton>
                {(s.status === "Pending" || s.status === "Assigned") && (
                  <SoftButton
                    variant="gradient"
                    color="error"
                    onClick={() =>
                      showConfirm(
                        "Cancel Shipment",
                        `Are you sure you want to cancel ${s.trackingNumber}? This action cannot be undone.`,
                        async () => {
                          await shipmentService.cancelShipment(s.id);
                          setShipments((prev) =>
                            prev.map((sh) =>
                              sh.id === s.id ? { ...sh, status: "Cancelled" as ShipmentStatus } : sh,
                            ),
                          );
                          setMessage("Shipment cancelled.");
                          setConfirmOpen(false);
                        },
                      )
                    }
                    sx={{ fontWeight: "bold", color: "#fff !important" }}
                  >
                    Cancel
                  </SoftButton>
                )}
              </Box>
            </Box>
          </Card>
        </Grid>

        {/* Details Grid */}
        <Grid item xs={12} sm={4}>
          <Card sx={{ p: 3, height: "100%" }}>
            <SoftTypography variant="subtitle2" fontWeight="bold" sx={{ mb: 2 }}>
              Sender
            </SoftTypography>
            <DetailRow label="Name" value={s.sender.name} />
            <DetailRow label="Phone" value={s.sender.phone} />
            <DetailRow label="Address" value={`${s.sender.address}, ${s.sender.city} ${s.sender.postalCode}`} />
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card sx={{ p: 3, height: "100%" }}>
            <SoftTypography variant="subtitle2" fontWeight="bold" sx={{ mb: 2 }}>
              Receiver
            </SoftTypography>
            <DetailRow label="Name" value={s.receiver.name} />
            <DetailRow label="Phone" value={s.receiver.phone} />
            {s.receiver.email && <DetailRow label="Email" value={s.receiver.email} />}
            <DetailRow label="Address" value={`${s.receiver.address}, ${s.receiver.city} ${s.receiver.postalCode}`} />
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card sx={{ p: 3, height: "100%" }}>
            <SoftTypography variant="subtitle2" fontWeight="bold" sx={{ mb: 2 }}>
              Parcel
            </SoftTypography>
            <DetailRow label="Description" value={s.parcel.description} />
            <DetailRow label="Category" value={s.parcel.category} />
            <DetailRow label="Weight" value={`${s.parcel.weight} kg`} />
            <DetailRow label="Quantity" value={String(s.parcel.quantity)} />
            {s.parcel.length && (
              <DetailRow label="Dimensions" value={`${s.parcel.length} × ${s.parcel.width} × ${s.parcel.height} cm`} />
            )}
            <DetailRow label="Charge" value={currency(s.amount)} />
          </Card>
        </Grid>

        {/* Timeline */}
        <Grid item xs={12}>
          <ShipmentTimeline shipment={s} />
        </Grid>
      </Grid>
    );
  }

  function renderBulkUpload() {
    return (
      <Grid container spacing={3}>
        {/* Upload Area */}
        <Grid item xs={12}>
          <Card sx={{ p: 3 }}>
            <Box
              sx={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                flexWrap: "wrap",
                gap: 2,
                mb: 3,
              }}
            >
              <Box>
                <SoftTypography variant="h6" fontWeight="bold">
                  Upload CSV Manifest
                </SoftTypography>
                <SoftTypography variant="button" color="text" fontWeight="regular">
                  Batch-create shipments from your store's order export.
                </SoftTypography>
              </Box>
              <SoftButton
                variant="outlined"
                color="dark"
                sx={{ fontWeight: "bold" }}
              >
                <Download size={14} style={{ marginRight: 6 }} />
                Download Template
              </SoftButton>
            </Box>

            {isApproved ? (
              <>
                <SoftBox
                  borderRadius="lg"
                  sx={{
                    p: 4,
                    textAlign: "center",
                    border: "2px dashed #e2e8f0",
                    backgroundColor: "#f8fafc",
                    mb: 3,
                  }}
                >
                  <SoftBox
                    component="span"
                    bgColor="info"
                    borderRadius="lg"
                    p={1.5}
                    display="flex"
                    sx={{
                      mx: "auto",
                      mb: 2,
                      color: "#00274c",
                      boxShadow: "0 3px 10px rgba(0, 39, 76, 0.12)",
                    }}
                  >
                    <Upload size={28} color="#00274c" />
                  </SoftBox>
                  <SoftTypography variant="h6">
                    Drag &amp; drop your CSV here, or{" "}
                    <SoftTypography
                      component="span"
                      variant="h6"
                      sx={{ color: "#2563eb", cursor: "pointer" }}
                      onClick={() => {
                        const input = document.createElement("input");
                        input.type = "file";
                        input.accept = ".csv";
                        input.onchange = (e: any) => {
                          if (e.target.files?.[0]) setUploadFile(e.target.files[0]);
                        };
                        input.click();
                      }}
                    >
                      browse files
                    </SoftTypography>
                  </SoftTypography>
                  <SoftTypography
                    variant="button"
                    color="text"
                    fontWeight="regular"
                    sx={{ maxWidth: 400, mx: "auto", mt: 1 }}
                  >
                    Supports Shopify, WooCommerce, and custom CSV exports.
                  </SoftTypography>
                  {uploadFile && (
                    <Box sx={{ mt: 2, display: "flex", justifyContent: "center", gap: 2 }}>
                      <SoftTypography variant="body2" fontWeight="bold">
                        {uploadFile.name}
                      </SoftTypography>
                      <SoftButton
                        variant="gradient"
                        color="primary"
                        onClick={handleBulkUpload}
                        disabled={uploading}
                        sx={{ color: "#00274c !important", fontWeight: "bold" }}
                      >
                        {uploading ? (
                          <>
                            <Loader2 size={14} style={{ marginRight: 6, animation: "spin 1s linear infinite" }} />
                            Uploading...
                          </>
                        ) : (
                          "Upload"
                        )}
                      </SoftButton>
                    </Box>
                  )}
                </SoftBox>

                {/* Upload Result */}
                {uploadResult && (
                  <Card sx={{ p: 3, mb: 3 }}>
                    <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
                      Upload Results
                    </SoftTypography>
                    <Grid container spacing={2} sx={{ mb: 3 }}>
                      {[
                        { label: "Total Records", value: uploadResult.totalRecords, color: "primary" },
                        { label: "Valid", value: uploadResult.validRecords, color: "success" },
                        { label: "Invalid", value: uploadResult.invalidRecords, color: "error" },
                      ].map(({ label, value, color }: any) => (
                        <Grid item xs={4} key={label}>
                          <SoftBox
                            bgColor={`${color}-100`}
                            borderRadius="lg"
                            p={2}
                            sx={{ textAlign: "center" }}
                          >
                            <SoftTypography variant="h5" fontWeight="bold">
                              {value}
                            </SoftTypography>
                            <SoftTypography variant="caption" color="text">
                              {label}
                            </SoftTypography>
                          </SoftBox>
                        </Grid>
                      ))}
                    </Grid>
                    {uploadResult.rows?.length > 0 && (
                      <Table
                        columns={[
                          { name: "row", align: "center" },
                          { name: "receiver", align: "left" },
                          { name: "destination", align: "left" },
                          { name: "status", align: "center" },
                          { name: "error", align: "left" },
                        ]}
                        rows={uploadResult.rows.map((r: any) => ({
                          id: r.rowNumber,
                          hasBorder: true,
                          row: r.rowNumber,
                          receiver: (
                            <Box>
                              <SoftTypography variant="button" fontWeight="bold" display="block">
                                {r.receiverName}
                              </SoftTypography>
                              <SoftTypography variant="caption" color="text">
                                {r.phone}
                              </SoftTypography>
                            </Box>
                          ),
                          destination: (
                            <Box>
                              <SoftTypography variant="button" display="block">
                                {r.destination}
                              </SoftTypography>
                              <SoftTypography variant="caption" color="text">
                                {r.parcelDescription}
                              </SoftTypography>
                            </Box>
                          ),
                          status: r.valid ? (
                            <SoftBadge badgeContent="Valid" color="success" size="xs" container />
                          ) : (
                            <SoftBadge badgeContent="Invalid" color="error" size="xs" container />
                          ),
                          error: r.errorMessage || "—",
                        }))}
                      />
                    )}
                  </Card>
                )}
              </>
            ) : (
              <SoftBox
                borderRadius="lg"
                sx={{
                  p: 3,
                  display: "flex",
                  alignItems: "center",
                  gap: 1.5,
                  backgroundColor: "#fffbeb",
                  border: "1px solid #fde68a",
                }}
              >
                <AlertTriangle size={20} color="#f59e0b" />
                <SoftTypography variant="body2" sx={{ color: "#92400e" }}>
                  Bulk uploads are locked until your merchant account is approved.
                </SoftTypography>
              </SoftBox>
            )}
          </Card>
        </Grid>

        {/* Manifest History */}
        <Grid item xs={12}>
          <Card sx={{ p: 3 }}>
            <Box sx={{ mb: 2 }}>
              <SoftTypography variant="h6" fontWeight="bold">
                Manifest History
              </SoftTypography>
              <SoftTypography variant="button" color="text" fontWeight="regular">
                Previously uploaded batch manifests
              </SoftTypography>
            </Box>
            {manifests.length > 0 ? (
              <Grid container spacing={2}>
                {manifests.map((m) => (
                  <Grid item xs={12} md={6} lg={4} key={m.id}>
                    <SoftBox
                      bgColor="grey-100"
                      borderRadius="lg"
                      p={2.5}
                      sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}
                    >
                      <Box
                        sx={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "flex-start",
                        }}
                      >
                        <Box>
                          <SoftTypography variant="button" fontWeight="bold" display="block">
                            {m.fileName}
                          </SoftTypography>
                          <SoftTypography variant="caption" color="text" display="block">
                            Uploaded {dateLabel(m.uploadedAt)}
                          </SoftTypography>
                        </Box>
                        <SoftBadge
                          badgeContent={m.status}
                          color={manifestOrderStatusColor(m.status)}
                          size="xs"
                          container
                        />
                      </Box>
                      <Box
                        sx={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                        }}
                      >
                        <SoftTypography variant="caption" color="text">
                          {m.processedCount} / {m.totalParcels} parcels
                        </SoftTypography>
                        <SoftTypography variant="caption" fontWeight="bold" color="dark">
                          {Math.round((m.processedCount / m.totalParcels) * 100)}%
                        </SoftTypography>
                      </Box>
                    </SoftBox>
                  </Grid>
                ))}
              </Grid>
            ) : (
              <EmptyState
                title="No manifests uploaded"
                text="Upload your first CSV manifest to batch-create shipments."
              />
            )}
          </Card>
        </Grid>
      </Grid>
    );
  }

  function renderTracking() {
    return (
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Card sx={{ p: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              Track a Shipment
            </SoftTypography>
            <Box sx={{ display: "flex", gap: 1, mb: 3 }}>
              <SoftInput
                fullWidth
                placeholder="Enter tracking number (e.g. SD-48291)"
                value={trackQuery}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                  setTrackQuery(e.target.value)
                }
                onKeyDown={(e: React.KeyboardEvent) => {
                  if (e.key === "Enter") handleTrack();
                }}
              />
              <SoftButton
                variant="gradient"
                color="primary"
                onClick={handleTrack}
                disabled={loading || !trackQuery.trim()}
                sx={{ color: "#00274c !important", fontWeight: "bold" }}
              >
                {loading ? (
                  <Loader2 size={16} style={{ animation: "spin 1s linear infinite" }} />
                ) : (
                  <Search size={16} />
                )}
              </SoftButton>
            </Box>

            {trackedShipment && (
              <Card sx={{ p: 3, bgcolor: "#f8fafc" }}>
                <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 2 }}>
                  <SoftTypography variant="h5" fontWeight="bold">
                    {trackedShipment.trackingNumber}
                  </SoftTypography>
                  <ShipmentStatusBadge status={trackedShipment.status} />
                </Box>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <DetailRow label="Receiver" value={trackedShipment.receiver.name} />
                    <DetailRow label="Phone" value={trackedShipment.receiver.phone} />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <DetailRow label="Destination" value={`${trackedShipment.receiver.city}`} />
                    <DetailRow label="Est. Delivery" value={dateLabel(trackedShipment.estimatedDelivery)} />
                  </Grid>
                </Grid>
              </Card>
            )}
          </Card>
        </Grid>

        {trackedShipment && (
          <Grid item xs={12}>
            <ShipmentTimeline shipment={trackedShipment} />
          </Grid>
        )}

        {!trackedShipment && !loading && (
          <Grid item xs={12}>
            <EmptyState
              title="No tracking result"
              text="Enter a tracking number above to see the delivery status."
              icon={MapPin}
            />
          </Grid>
        )}
      </Grid>
    );
  }

  function renderHistory() {
    return (
      <Card sx={{ overflow: "hidden" }}>
        <Box sx={{ p: 3 }}>
          <Box
            sx={{
              display: "flex",
              justifyContent: "space-between",
              gap: 2,
              flexWrap: "wrap",
              mb: 3,
            }}
          >
            <Box>
              <SoftTypography variant="h6" fontWeight="bold">
                Delivery History
              </SoftTypography>
              <SoftTypography variant="button" color="text" fontWeight="regular">
                {historyShipments.length} records
              </SoftTypography>
            </Box>
            <SoftButton
              variant="outlined"
              color="dark"
              onClick={() => handleExportCsv(historyShipments)}
              disabled={!historyShipments.length}
            >
              <Download size={15} style={{ marginRight: 8 }} />
              Export
            </SoftButton>
          </Box>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={8}>
              <SoftInput
                fullWidth
                value={historyQuery}
                placeholder="Search by tracking number or receiver"
                onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                  setHistoryQuery(e.target.value)
                }
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <SoftInput
                fullWidth
                value={historyStatus}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                  setHistoryStatus(e.target.value)
                }
                select
              >
                <option value="All">All</option>
                <option value="Delivered">Delivered</option>
                <option value="Failed">Failed / Cancelled</option>
              </SoftInput>
            </Grid>
          </Grid>
        </Box>
        {renderShipmentTable(historyShipments)}
      </Card>
    );
  }

  function renderProfile() {
    return (
      <Grid container spacing={3}>
        <Grid item xs={12} lg={7}>
          <Card sx={{ p: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              Business Information
            </SoftTypography>
            <Grid container spacing={2.5}>
              <Grid item xs={12} sm={6}>
                <SoftTypography component="label" htmlFor="prof-name" variant="caption" fontWeight="bold" display="block" mb={1}>
                  Business Name
                </SoftTypography>
                <SoftInput id="prof-name" fullWidth defaultValue={user?.fullName || ""} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <SoftTypography component="label" htmlFor="prof-reg" variant="caption" fontWeight="bold" display="block" mb={1}>
                  Registration Number
                </SoftTypography>
                <SoftInput id="prof-reg" fullWidth defaultValue="REG-2024-001" />
              </Grid>
              <Grid item xs={12} sm={6}>
                <SoftTypography component="label" htmlFor="prof-email" variant="caption" fontWeight="bold" display="block" mb={1}>
                  Business Email
                </SoftTypography>
                <SoftInput id="prof-email" fullWidth defaultValue={user?.email || ""} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <SoftTypography component="label" htmlFor="prof-phone" variant="caption" fontWeight="bold" display="block" mb={1}>
                  Phone
                </SoftTypography>
                <SoftInput id="prof-phone" fullWidth defaultValue={user?.phoneNumber || ""} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <SoftTypography component="label" htmlFor="prof-cat" variant="caption" fontWeight="bold" display="block" mb={1}>
                  Category
                </SoftTypography>
                <SoftInput id="prof-cat" fullWidth defaultValue="E-Commerce" />
              </Grid>
              <Grid item xs={12} sm={6}>
                <SoftTypography component="label" htmlFor="prof-city" variant="caption" fontWeight="bold" display="block" mb={1}>
                  City
                </SoftTypography>
                <SoftInput id="prof-city" fullWidth defaultValue="Colombo" />
              </Grid>
              <Grid item xs={12}>
                <SoftTypography component="label" htmlFor="prof-addr" variant="caption" fontWeight="bold" display="block" mb={1}>
                  Address
                </SoftTypography>
                <SoftInput id="prof-addr" fullWidth defaultValue="123 Galle Road" />
              </Grid>
              <Grid item xs={12}>
                <SoftTypography component="label" htmlFor="prof-desc" variant="caption" fontWeight="bold" display="block" mb={1}>
                  Description
                </SoftTypography>
                <SoftInput id="prof-desc" fullWidth multiline rows={2} defaultValue="Online retail store." />
              </Grid>
              <Grid item xs={12}>
                <SoftButton variant="gradient" color="primary" sx={{ color: "#00274c !important", fontWeight: "bold" }}>
                  Save Changes
                </SoftButton>
              </Grid>
            </Grid>
          </Card>
        </Grid>

        <Grid item xs={12} lg={5}>
          <Card sx={{ p: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              Verification Status
            </SoftTypography>
            {isApproved && (
              <SoftBox
                borderRadius="lg"
                sx={{
                  p: 2,
                  display: "flex",
                  alignItems: "center",
                  gap: 1.5,
                  backgroundColor: "#eff6ff",
                  border: "1px solid #bfdbfe",
                  mb: 2,
                }}
              >
                <ShieldCheck size={20} color="#2563eb" />
                <SoftTypography variant="body2" sx={{ color: "#1e40af" }}>
                  Fully verified. All features enabled.
                </SoftTypography>
              </SoftBox>
            )}
            {isPending && (
              <SoftBox
                borderRadius="lg"
                sx={{
                  p: 3,
                  display: "flex",
                  alignItems: "flex-start",
                  gap: 2,
                  backgroundColor: "#fffbeb",
                  border: "1px solid #fde68a",
                  mb: 2,
                }}
              >
                <Clock size={20} color="#f59e0b" />
                <Box>
                  <SoftTypography variant="button" fontWeight="bold" sx={{ color: "#92400e" }}>
                    Under Verification
                  </SoftTypography>
                  <SoftTypography variant="body2" sx={{ color: "#a16207", mt: 0.5 }}>
                    Your registration is under review.
                  </SoftTypography>
                </Box>
              </SoftBox>
            )}
            <DetailRow label="Registered" value={user?.createdAt ? dateLabel(user.createdAt) : "—"} />
            <DetailRow label="Status" value={approvalStatus || "—"} />
          </Card>
        </Grid>
      </Grid>
    );
  }

  function renderApi() {
    const creds =
      apiCredentials || {
        apiKey: "sd_live_9a8f21b74c8e3921049281a",
        clientId: "client_merchant_001",
        requestsToday: 1247,
        avgResponseTime: "120ms",
        rateLimit: "100 req/s",
        errorRate: "0.3%",
      };

    return (
      <Grid container spacing={3}>
        {/* API Key Card */}
        <Grid item xs={12}>
          <Card sx={{ p: 3 }}>
            <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 1 }}>
              <SoftBox
                component="span"
                bgColor="info"
                borderRadius="lg"
                p={1}
                display="flex"
                sx={{ color: "#00274c", boxShadow: "0 3px 10px rgba(0, 39, 76, 0.12)" }}
              >
                <Key size={18} color="#00274c" />
              </SoftBox>
              <SoftTypography variant="h6" fontWeight="bold">
                API Credentials
              </SoftTypography>
            </Box>
            <SoftTypography variant="body2" color="text" sx={{ mb: 2 }}>
              Use these credentials to integrate SwiftDrop into your storefront.
            </SoftTypography>

            {isApproved ? (
              <Box sx={{ display: "flex", flexDirection: { xs: "column", sm: "row" }, alignItems: "center", gap: 2, mb: 2 }}>
                <SoftBox
                  borderRadius="lg"
                  sx={{
                    flex: 1,
                    p: 2,
                    fontFamily: "monospace",
                    fontSize: "0.75rem",
                    wordBreak: "break-all",
                    userSelect: "all",
                    backgroundColor: "#f1f5f9",
                    border: "1px solid #e2e8f0",
                    color: "#0f172a",
                  }}
                >
                  {creds.apiKey}
                </SoftBox>
                <SoftButton
                  variant="gradient"
                  color={copiedKey ? "info" : "dark"}
                  onClick={handleCopyApiKey}
                  sx={{ fontWeight: "bold", minWidth: 100, color: "#00274c !important" }}
                >
                  {copiedKey ? (
                    <Check size={14} style={{ marginRight: 4 }} />
                  ) : (
                    <Copy size={14} style={{ marginRight: 4 }} />
                  )}
                  {copiedKey ? "Copied!" : "Copy Key"}
                </SoftButton>
              </Box>
            ) : (
              <SoftBox
                borderRadius="lg"
                sx={{
                  p: 2,
                  display: "flex",
                  alignItems: "center",
                  gap: 1,
                  backgroundColor: "#f8fafc",
                  border: "1px dashed #cbd5e1",
                  mb: 2,
                }}
              >
                <AlertTriangle size={16} color="#f59e0b" />
                <SoftTypography variant="caption" color="text">
                  API keys are locked until your account is approved.
                </SoftTypography>
              </SoftBox>
            )}
            <DetailRow label="Client ID" value={creds.clientId} />
          </Card>
        </Grid>

        {/* API Usage Stats */}
        <Grid item xs={12} lg={6}>
          <Card sx={{ p: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              API Usage Overview
            </SoftTypography>
            <Grid container spacing={2}>
              {[
                { label: "Requests Today", value: creds.requestsToday?.toLocaleString() || "—" },
                { label: "Avg Response Time", value: creds.avgResponseTime || "—" },
                { label: "Rate Limit", value: creds.rateLimit || "—" },
                { label: "Error Rate", value: creds.errorRate || "—" },
              ].map(({ label, value }: { label: string; value: string }) => (
                <Grid item xs={6} key={label}>
                  <SoftBox bgColor="grey-100" borderRadius="lg" p={2}>
                    <SoftTypography variant="caption" color="text" display="block">
                      {label}
                    </SoftTypography>
                    <SoftTypography variant="h5" fontWeight="bold" sx={{ mt: 0.5 }}>
                      {value}
                    </SoftTypography>
                  </SoftBox>
                </Grid>
              ))}
            </Grid>
          </Card>
        </Grid>

        {/* Integration Guide */}
        <Grid item xs={12} lg={6}>
          <Card sx={{ p: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              Integration Guide
            </SoftTypography>
            <SoftBox variant="gradient" bgColor="dark" borderRadius="lg" p={3}>
              <SoftTypography variant="body2" color="white">
                <strong>Base URL:</strong>{" "}
                <code style={{ color: "#90cdf4" }}>
                  {import.meta.env.VITE_API_BASE_URL || "/api/v1"}
                </code>
              </SoftTypography>
              <SoftTypography variant="body2" color="white" sx={{ mt: 2 }}>
                <strong>Authentication:</strong> Include your API key in the header:
              </SoftTypography>
              <SoftTypography
                variant="caption"
                color="white"
                sx={{
                  mt: 1,
                  fontFamily: "monospace",
                  backgroundColor: "rgba(0,0,0,0.2)",
                  p: 1.5,
                  display: "block",
                  borderRadius: 1,
                }}
              >
                Authorization: Bearer {creds.apiKey}
              </SoftTypography>
              <SoftTypography variant="body2" color="white" sx={{ mt: 2 }}>
                <strong>Endpoints:</strong>
              </SoftTypography>
              <SoftTypography
                variant="caption"
                color="white"
                sx={{ fontFamily: "monospace", display: "block", mt: 0.5 }}
              >
                POST /parcels — Create shipment
                <br />
                POST /parcels/bulk-upload — Upload manifest
                <br />
                GET /tracking/:id — Track shipment
                <br />
                GET /parcels — List shipments
              </SoftTypography>
            </SoftBox>
          </Card>
        </Grid>

        {/* Webhooks */}
        <Grid item xs={12}>
          <Card sx={{ p: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              Webhook Configuration
            </SoftTypography>
            <SoftBox
              borderRadius="lg"
              p={3}
              sx={{ backgroundColor: "#f8fafc", border: "1px solid #e2e8f0" }}
            >
              <DetailRow label="Webhook URL" value="https://mystore.example.com/webhooks/swiftdrop" />
              <DetailRow label="Status" value="Active" />
              <SoftTypography variant="caption" fontWeight="bold" display="block" mt={2} mb={1}>
                Subscribed Events
              </SoftTypography>
              {["Shipment Created", "Courier Assigned", "Picked Up", "In Transit", "Delivered", "Delivery Failed"].map(
                (event) => (
                  <Box
                    key={event}
                    sx={{
                      display: "flex",
                      alignItems: "center",
                      gap: 1,
                      py: 0.5,
                    }}
                  >
                    <Check size={14} color="#16a34a" />
                    <SoftTypography variant="body2">{event}</SoftTypography>
                  </Box>
                ),
              )}
            </SoftBox>
          </Card>
        </Grid>
      </Grid>
    );
  }

  function renderNotifications() {
    return (
      <Card sx={{ overflow: "hidden" }}>
        <Box sx={{ p: 3, pb: 2 }}>
          <Box
            sx={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              flexWrap: "wrap",
              gap: 2,
              mb: 2,
            }}
          >
            <Box>
              <SoftTypography variant="h6" fontWeight="bold">
                Notifications
                {unreadNotifs > 0 && (
                  <SoftBadge
                    badgeContent={unreadNotifs}
                    color="error"
                    size="sm"
                    container
                    sx={{ ml: 1 }}
                  />
                )}
              </SoftTypography>
              <SoftTypography variant="button" color="text" fontWeight="regular">
                {notifications.length} total notifications
              </SoftTypography>
            </Box>
            {unreadNotifs > 0 && (
              <SoftButton
                variant="text"
                color="dark"
                onClick={() =>
                  setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))
                }
                sx={{ fontWeight: "bold" }}
              >
                Mark all as read
              </SoftButton>
            )}
          </Box>
        </Box>

        {notifications.length === 0 ? (
          <EmptyState title="No notifications" text="You're all caught up!" icon={Bell} />
        ) : (
          notifications.map((n, i) => (
            <Box
              key={n.id}
              sx={{
                p: 3,
                px: 4,
                display: "flex",
                gap: 2,
                alignItems: "flex-start",
                backgroundColor: n.read ? "#fff" : "#eff6ff",
                borderBottom: i < notifications.length - 1 ? "1px solid #f1f5f9" : "none",
                cursor: "pointer",
                transition: "background-color 0.15s",
                "&:hover": { backgroundColor: n.read ? "#f8fafc" : "#dbeafe" },
              }}
              onClick={() =>
                setNotifications((prev) =>
                  prev.map((nn) => (nn.id === n.id ? { ...nn, read: true } : nn)),
                )
              }
            >
              <SoftBox
                component="span"
                bgColor={
                  n.category === "delivery"
                    ? "success"
                    : n.category === "account"
                      ? "warning"
                      : n.category === "system"
                        ? "dark"
                        : "info"
                }
                borderRadius="lg"
                p={1}
                display="flex"
                sx={{
                  minWidth: 36,
                  alignItems: "center",
                  justifyContent: "center",
                  color: "#00274c",
                }}
              >
                <Bell size={16} />
              </SoftBox>
              <Box sx={{ minWidth: 0, flex: 1 }}>
                <SoftTypography variant="button" fontWeight={n.read ? "regular" : "bold"} display="block">
                  {n.title}
                </SoftTypography>
                <SoftTypography variant="body2" color="text" sx={{ mb: 0.5 }}>
                  {n.message}
                </SoftTypography>
                <SoftTypography variant="caption" color="text">
                  {dateTimeLabel(n.createdAt)}
                </SoftTypography>
              </Box>
              {!n.read && (
                <Box
                  sx={{
                    width: 8,
                    height: 8,
                    borderRadius: "50%",
                    backgroundColor: "#2563eb",
                    mt: 1,
                    flexShrink: 0,
                  }}
                />
              )}
            </Box>
          ))
        )}
      </Card>
    );
  }

  function renderSettings() {
    return (
      <Grid container spacing={3}>
        <Grid item xs={12} lg={7}>
          <Card sx={{ p: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              Account Information
            </SoftTypography>
            <Grid container spacing={2.5}>
              <Grid item xs={12} sm={6}>
                <SoftTypography component="label" htmlFor="set-name" variant="caption" fontWeight="bold" display="block" mb={1}>
                  Full Name
                </SoftTypography>
                <SoftInput id="set-name" fullWidth defaultValue={user?.fullName || ""} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <SoftTypography component="label" htmlFor="set-email" variant="caption" fontWeight="bold" display="block" mb={1}>
                  Email
                </SoftTypography>
                <SoftInput id="set-email" fullWidth defaultValue={user?.email || ""} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <SoftTypography component="label" htmlFor="set-phone" variant="caption" fontWeight="bold" display="block" mb={1}>
                  Phone
                </SoftTypography>
                <SoftInput id="set-phone" fullWidth defaultValue={user?.phoneNumber || ""} />
              </Grid>
              <Grid item xs={12}>
                <SoftButton variant="gradient" color="primary" sx={{ color: "#00274c !important", fontWeight: "bold" }}>
                  Save Changes
                </SoftButton>
              </Grid>
            </Grid>
          </Card>

          <Card sx={{ p: 3, mt: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              Change Password
            </SoftTypography>
            <Grid container spacing={2.5}>
              <Grid item xs={12}>
                <SoftTypography component="label" htmlFor="set-old-pass" variant="caption" fontWeight="bold" display="block" mb={1}>
                  Current Password
                </SoftTypography>
                <SoftInput id="set-old-pass" fullWidth type="password" />
              </Grid>
              <Grid item xs={12} sm={6}>
                <SoftTypography component="label" htmlFor="set-new-pass" variant="caption" fontWeight="bold" display="block" mb={1}>
                  New Password
                </SoftTypography>
                <SoftInput id="set-new-pass" fullWidth type="password" />
              </Grid>
              <Grid item xs={12} sm={6}>
                <SoftTypography component="label" htmlFor="set-confirm-pass" variant="caption" fontWeight="bold" display="block" mb={1}>
                  Confirm Password
                </SoftTypography>
                <SoftInput id="set-confirm-pass" fullWidth type="password" />
              </Grid>
              <Grid item xs={12}>
                <SoftButton variant="gradient" color="primary" sx={{ color: "#00274c !important", fontWeight: "bold" }}>
                  Update Password
                </SoftButton>
              </Grid>
            </Grid>
          </Card>
        </Grid>

        <Grid item xs={12} lg={5}>
          <Card sx={{ p: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              Notification Preferences
            </SoftTypography>
            {["Shipment updates", "Delivery confirmations", "Account notifications", "System alerts"].map(
              (pref) => (
                <Box
                  key={pref}
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    py: 1.5,
                    borderBottom: "1px solid #f1f5f9",
                  }}
                >
                  <SoftTypography variant="body2">{pref}</SoftTypography>
                  <SoftBox
                    component="span"
                    bgColor="primary"
                    borderRadius="lg"
                    p={0.5}
                    sx={{ width: 36, height: 20, display: "flex", alignItems: "center", justifyContent: "flex-end", cursor: "pointer" }}
                  >
                    <Box
                      sx={{
                        width: 16,
                        height: 16,
                        borderRadius: "50%",
                        backgroundColor: "#fff",
                        boxShadow: "0 1px 3px rgba(0,0,0,0.2)",
                      }}
                    />
                  </SoftBox>
                </Box>
              ),
            )}
          </Card>

          <Card sx={{ p: 3, mt: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              Approval Status
            </SoftTypography>
            <ApprovalStatusBanner status={approvalStatus} />
          </Card>
        </Grid>
      </Grid>
    );
  }

  // ═══════════════════════════════════════════════════════════
  //  MAIN RENDER
  // ═══════════════════════════════════════════════════════════

  const renderTab = () => {
    switch (activeTab) {
      case "overview":
        return renderOverview();
      case "shipments":
        return renderShipments();
      case "create-shipment":
        return renderCreateShipment();
      case "shipment-detail":
        return renderShipmentDetail();
      case "bulk-upload":
        return renderBulkUpload();
      case "tracking":
        return renderTracking();
      case "history":
        return renderHistory();
      case "profile":
        return renderProfile();
      case "api":
        return renderApi();
      case "notifications":
        return renderNotifications();
      case "settings":
        return renderSettings();
      default:
        return renderOverview();
    }
  };

  return (
    <Box
      sx={{
        "& .MuiCard-root": { minWidth: 0 },
        "& canvas": { maxWidth: "100%" },
      }}
    >
      {/* Approval Banner */}
      <ApprovalStatusBanner status={approvalStatus} />

      {/* Page Heading */}
      <Box sx={{ mb: 3 }}>
        <SoftTypography component="h1" variant="h4" fontWeight="bold">
          {headings[activeTab][0]}
        </SoftTypography>
        <SoftTypography variant="body2" color="text" mt={0.5}>
          {headings[activeTab][1]}
        </SoftTypography>
      </Box>

      {/* Tab Content */}
      {renderTab()}

      {/* Confirm Dialog */}
      <ConfirmDialog
        open={confirmOpen}
        title={confirmTitle}
        message={confirmMessage}
        onConfirm={() => {
          confirmAction();
          setConfirmOpen(false);
        }}
        onCancel={() => setConfirmOpen(false)}
      />

      {/* Snackbar */}
      <Snackbar
        open={!!message}
        autoHideDuration={4500}
        onClose={() => setMessage("")}
        message={message}
      />
    </Box>
  );
};
