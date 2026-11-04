import React, { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import {
  Alert,
  Box,
  Card,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Grid,
  IconButton,
  Select,
  Snackbar,
  Switch,
} from "@mui/material";
import {
  ArrowRight,
  Check,
  Copy,
  Download,
  Edit2,
  MapPin,
  Package,
  Plus,
  Trash2,
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
import rocket from "assets/images/illustrations/rocket-white.png";
import landscape from "assets/images/ivancik.jpg";
import { useAuth } from "../../hooks/useAuth";
import {
  Address,
  Shipment,
  Workspace,
  currency,
  dateLabel,
  demoWorkspace,
  estimatePrice,
  parseWorkspace,
  shipmentCsv,
  statusColor,
  statuses,
  trackingSteps,
} from "./customerData";

type Tab = "overview" | "send" | "track" | "history" | "settings";
type Booking = {
  recipient: string;
  phone: string;
  destination: string;
  pickup: string;
  weight: string;
  service: Shipment["service"];
  packageType: string;
  notes: string;
};
const headings: Record<Tab, [string, string]> = {
  overview: [
    "Your deliveries, beautifully organised.",
    "A little less admin. A lot more peace of mind.",
  ],
  send: [
    "Send something good.",
    "Create a shipment, review the details, and keep everything in one place.",
  ],
  track: [
    "Every step, in sight.",
    "Look up a shipment and follow its delivery milestones.",
  ],
  history: [
    "Your shipment history.",
    "Search, filter, and export your parcel records.",
  ],
  settings: [
    "Make yourself at home.",
    "Manage your address book and demo delivery preferences.",
  ],
};
function Field({
  label,
  id,
  ...props
}: {
  label: string;
  id: string;
  [key: string]: unknown;
}) {
  return (
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
      <SoftInput id={id} fullWidth {...props} />
    </Box>
  );
}
function Empty({ title, text }: { title: string; text: string }) {
  return (
    <Box sx={{ p: 5, textAlign: "center" }}>
      <Package size={32} style={{ margin: "0 auto 12px", color: "#8392ab" }} />
      <SoftTypography variant="h6">{title}</SoftTypography>
      <SoftTypography variant="button" color="text" fontWeight="regular">
        {text}
      </SoftTypography>
    </Box>
  );
}
function Details({ items }: { items: string[][] }) {
  return (
    <Box sx={{ display: "grid", gap: 2 }}>
      {items.map(([label, value]) => (
        <Box key={label}>
          <SoftTypography variant="caption" color="text" display="block">
            {label}
          </SoftTypography>
          <SoftTypography variant="body2" sx={{ overflowWrap: "anywhere" }}>
            {value}
          </SoftTypography>
        </Box>
      ))}
    </Box>
  );
}

export const CustomerDashboard: React.FC<{ previewMode?: boolean }> = ({
  previewMode = false,
}) => {
  const { user } = useAuth();
  const { tab = "overview" } = useParams();
  const activeTab: Tab = Object.prototype.hasOwnProperty.call(headings, tab)
    ? (tab as Tab)
    : "overview";
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const base = previewMode
    ? "/customer/dashboard-preview"
    : "/customer/dashboard";
  const storageKey = `swiftdrop:customer-demo:v1:${previewMode ? "preview" : user?.id || "guest"}`;
  const [data, setData] = useState<Workspace>(() => {
    const fallback = demoWorkspace({
      name: previewMode ? "Alex Morgan" : user?.fullName || "Customer",
      email: previewMode ? "alex@example.com" : user?.email || "",
      phone: previewMode ? "+94 77 123 4567" : user?.phoneNumber || "",
    });
    try {
      return parseWorkspace(sessionStorage.getItem(storageKey), fallback);
    } catch {
      return fallback;
    }
  });
  const [storageError, setStorageError] = useState(false);
  const [message, setMessage] = useState("");
  const [booking, setBooking] = useState<Booking>({
    recipient: "",
    phone: "",
    destination: "",
    pickup: data.addresses[0]?.address || "",
    weight: "1",
    service: "Standard",
    packageType: "Small parcel",
    notes: "",
  });
  const [review, setReview] = useState(false);
  const [accepted, setAccepted] = useState(false);
  const [created, setCreated] = useState<Shipment | null>(null);
  const [tracking, setTracking] = useState(
    params.get("id") || data.shipments[0]?.id || "",
  );
  const [trackedId, setTrackedId] = useState(
    params.get("id") || data.shipments[0]?.id || "",
  );
  const [status, setStatus] = useState("All");
  const [page, setPage] = useState(0);
  const [profile, setProfile] = useState(data.profile);
  const [editingAddress, setEditingAddress] = useState<Address | null>(null);
  const [deletingAddress, setDeletingAddress] = useState<Address | null>(null);
  const query = params.get("q") || "";
  const go = (next: Tab) => navigate(`${base}/${next}`);
  const track = (id: string) => {
    setTracking(id);
    setTrackedId(id);
    navigate(`${base}/track?id=${encodeURIComponent(id)}`);
  };
  useEffect(() => {
    try {
      sessionStorage.setItem(storageKey, JSON.stringify(data));
      setStorageError(false);
    } catch {
      setStorageError(true);
    }
  }, [data, storageKey]);
  useEffect(() => {
    const id = params.get("id");
    if (id) {
      setTracking(id);
      setTrackedId(id);
    }
  }, [params]);
  useEffect(() => {
    setPage(0);
  }, [query, status]);
  const active = data.shipments.filter(
    (s) => s.status === "In transit" || s.status === "Awaiting pickup",
  );
  const delivered = data.shipments.filter((s) => s.status === "Delivered");
  const total = data.shipments
    .filter((s) => s.status !== "Cancelled")
    .reduce((sum, s) => sum + s.amount, 0);
  const selected = data.shipments.find(
    (s) => s.id.toLowerCase() === trackedId.trim().toLowerCase(),
  );
  const filtered = data.shipments.filter(
    (s) =>
      (status === "All" || s.status === status) &&
      `${s.id} ${s.recipient} ${s.destination}`
        .toLowerCase()
        .includes(query.trim().toLowerCase()),
  );
  const pageCount = Math.max(1, Math.ceil(filtered.length / 5));
  const currentPage = Math.min(page, pageCount - 1);
  const charts = useMemo(() => {
    const now = Date.now();
    const weeks = [3, 2, 1, 0].map((week) =>
      data.shipments.filter((s) => {
        const days = (now - new Date(s.bookedAt).getTime()) / 86400000;
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
          { label: "Booked", color: "info", data: weeks.map((w) => w.length) },
          {
            label: "Delivered from these bookings",
            color: "dark",
            data: weeks.map(
              (w) => w.filter((s) => s.status === "Delivered").length,
            ),
          },
        ],
      },
    };
  }, [data.shipments]);
  const updateBooking =
    (key: keyof Booking) => (event: React.ChangeEvent<HTMLInputElement>) =>
      setBooking((previous) => ({ ...previous, [key]: event.target.value }));
  const bookingValid =
    booking.recipient.trim() &&
    booking.destination.trim().length >= 5 &&
    booking.pickup.trim().length >= 5 &&
    /^[+\d][\d\s()-]{6,19}$/.test(booking.phone.trim()) &&
    Number(booking.weight) > 0 &&
    Number(booking.weight) <= 30;
  const confirmBooking = () => {
    if (!bookingValid || !accepted) return;
    const shipment: Shipment = {
      ...booking,
      recipient: booking.recipient.trim(),
      destination: booking.destination.trim(),
      id: `SD-${crypto.randomUUID().slice(0, 8).toUpperCase()}`,
      weight: Number(booking.weight),
      bookedAt: new Date().toISOString(),
      status: "Awaiting pickup",
      amount: estimatePrice(Number(booking.weight), booking.service),
    };
    setData((previous) => ({
      ...previous,
      shipments: [shipment, ...previous.shipments],
    }));
    setCreated(shipment);
    setReview(false);
    setAccepted(false);
  };
  const exportHistory = () => {
    const url = URL.createObjectURL(
      new Blob(["\uFEFF", shipmentCsv(filtered)], {
        type: "text/csv;charset=utf-8;",
      }),
    );
    const link = document.createElement("a");
    link.href = url;
    link.download = "swiftdrop-demo-shipments.csv";
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    setMessage("Filtered shipment records exported.");
  };
  const shipmentTable = (shipments: Shipment[]) =>
    !shipments.length ? (
      <Empty
        title="No matching shipments"
        text="Try another search or create your first demo shipment."
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
        rows={shipments.map((s) => ({
          id: s.id,
          hasBorder: true,
          shipment: (
            <Box
              component="span"
              sx={{ display: "flex", alignItems: "center", gap: 1.5, px: 2 }}
            >
              <SoftBox
                component="span"
                bgColor="grey-100"
                borderRadius="lg"
                p={1}
                display="flex"
              >
                <Package size={18} color="#344767" />
              </SoftBox>
              <Box component="span">
                <SoftTypography
                  component="span"
                  display="block"
                  variant="button"
                  fontWeight="bold"
                >
                  {s.id}
                </SoftTypography>
                <SoftTypography
                  component="span"
                  display="block"
                  variant="caption"
                  color="text"
                >
                  {s.recipient}
                </SoftTypography>
              </Box>
            </Box>
          ),
          destination: (
            <Box component="span">
              <SoftTypography component="span" variant="button" display="block">
                {s.destination}
              </SoftTypography>
              <SoftTypography component="span" variant="caption" color="text">
                {dateLabel(s.bookedAt)} · {s.service}
              </SoftTypography>
            </Box>
          ),
          status: (
            <SoftBadge
              badgeContent={s.status}
              color={statusColor(s.status)}
              size="xs"
              container
            />
          ),
          amount: currency(s.amount),
          action: (
            <SoftButton
              variant="text"
              color="info"
              size="small"
              aria-label={`Track ${s.id}`}
              onClick={() => track(s.id)}
            >
              Details <ArrowRight size={13} style={{ marginLeft: 5 }} />
            </SoftButton>
          ),
        }))}
      />
    );

  function overview() {
    return (
      <Grid container spacing={3}>
        {[
          ["Total shipments", data.shipments.length, "inventory_2"],
          ["On the way", active.length, "local_shipping"],
          ["Delivered", delivered.length, "task_alt"],
          ["Shipping spend", currency(total), "account_balance_wallet"],
        ].map(([title, count, icon]) => (
          <Grid item xs={12} sm={6} xl={3} key={title}>
            <MiniStatisticsCard
              title={{ text: title, fontWeight: "medium" }}
              count={count}
              percentage={{ color: "secondary", text: "" }}
              icon={{ color: "info", component: icon }}
            />
          </Grid>
        ))}
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
                  <SoftTypography
                    variant="button"
                    color="text"
                    fontWeight="medium"
                  >
                    Made for your everyday deliveries
                  </SoftTypography>
                  <SoftTypography variant="h4" fontWeight="bold" mt={1}>
                    Welcome back, {data.profile.name.split(" ")[0]}.
                  </SoftTypography>
                  <SoftTypography variant="body2" color="text" mt={1} mb={3}>
                    From a thoughtful gift to your next big idea. Get it where
                    it needs to go, with SwiftDrop.
                  </SoftTypography>
                  <SoftButton
                    variant="text"
                    color="dark"
                    onClick={() => {
                      setCreated(null);
                      go("send");
                    }}
                    sx={{ mt: "auto", alignSelf: "flex-start", px: 0 }}
                  >
                    Send a parcel{" "}
                    <ArrowRight size={16} style={{ marginLeft: 8 }} />
                  </SoftButton>
                </Box>
              </Grid>
              <Grid item xs={12} sm={5}>
                <SoftBox
                  variant="gradient"
                  bgColor="info"
                  borderRadius="lg"
                  sx={{
                    height: "100%",
                    minHeight: 200,
                    display: "grid",
                    placeItems: "center",
                    overflow: "hidden",
                  }}
                >
                  <Box
                    component="img"
                    src={rocket}
                    alt=""
                    sx={{ width: "85%", maxHeight: 230, objectFit: "contain" }}
                  />
                </SoftBox>
              </Grid>
            </Grid>
          </Card>
        </Grid>
        <Grid item xs={12} lg={5}>
          <Card sx={{ p: 2, height: "100%" }}>
            <Box
              sx={{
                borderRadius: 3,
                p: 3,
                minHeight: 250,
                height: "100%",
                backgroundImage: `linear-gradient(310deg, rgba(20,23,39,.94), rgba(58,65,111,.65)), url(${landscape})`,
                backgroundSize: "cover",
                display: "flex",
                flexDirection: "column",
              }}
            >
              <SoftTypography variant="h5" fontWeight="bold" color="white">
                A clearer view of every delivery.
              </SoftTypography>
              <SoftTypography
                variant="body2"
                color="white"
                opacity={0.85}
                mt={2}
                mb={3}
              >
                {active.length} parcels in your active queue. Check each
                milestone and keep the important details close.
              </SoftTypography>
              <SoftButton
                variant="text"
                color="white"
                onClick={() =>
                  active[0] ? track(active[0].id) : go("history")
                }
                sx={{ mt: "auto", alignSelf: "flex-start", px: 0 }}
              >
                View your deliveries{" "}
                <ArrowRight size={16} style={{ marginLeft: 8 }} />
              </SoftButton>
            </Box>
          </Card>
        </Grid>
        <Grid item xs={12} lg={5}>
          <ReportsBarChart
            title="Shipping activity"
            description="Bookings across the last four weeks"
            chart={charts.bar}
            items={[
              {
                icon: { color: "info", component: "inventory_2" },
                label: "Parcels",
                progress: {
                  content: String(data.shipments.length),
                  percentage: 100,
                },
              },
              {
                icon: { color: "success", component: "task_alt" },
                label: "Delivered",
                progress: {
                  content: String(delivered.length),
                  percentage: data.shipments.length
                    ? (delivered.length / data.shipments.length) * 100
                    : 0,
                },
              },
              {
                icon: { color: "warning", component: "local_shipping" },
                label: "Active",
                progress: {
                  content: String(active.length),
                  percentage: data.shipments.length
                    ? (active.length / data.shipments.length) * 100
                    : 0,
                },
              },
              {
                icon: { color: "primary", component: "place" },
                label: "Addresses",
                progress: {
                  content: String(data.addresses.length),
                  percentage: 100,
                },
              },
            ]}
          />
        </Grid>
        <Grid item xs={12} lg={7}>
          <GradientLineChart
            title="Delivery overview"
            description="Booked parcels and their delivered status, grouped by booking week"
            height="17rem"
            chart={charts.line}
          />
        </Grid>
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
                  Recent shipments
                </SoftTypography>
                <SoftTypography
                  variant="button"
                  color="text"
                  fontWeight="regular"
                >
                  The latest from your delivery book
                </SoftTypography>
              </Box>
              <SoftButton
                size="small"
                variant="text"
                color="info"
                onClick={() => go("history")}
              >
                View all
              </SoftButton>
            </Box>
            {shipmentTable(data.shipments.slice(0, 5))}
          </Card>
        </Grid>
        <Grid item xs={12} lg={4}>
          <TimelineList title="Shipment updates">
            {data.shipments.slice(0, 4).map((s, index) => (
              <TimelineItem
                key={s.id}
                color={statusColor(s.status)}
                icon={s.status === "Delivered" ? "task_alt" : "local_shipping"}
                title={`${s.id} · ${s.status}`}
                dateTime={`Booked ${dateLabel(s.bookedAt)}`}
                description={s.destination}
                lastItem={index === Math.min(data.shipments.length, 4) - 1}
              />
            ))}
          </TimelineList>
        </Grid>
      </Grid>
    );
  }

  function sendPage() {
    if (created)
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
            <Check size={30} />
          </SoftBox>
          <SoftTypography variant="h4" fontWeight="bold">
            Your demo shipment is ready.
          </SoftTypography>
          <SoftTypography variant="body2" color="text" mt={2}>
            Reference {created.id}. No courier has been booked and no payment
            was taken.
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
              color="info"
              onClick={() => track(created.id)}
            >
              Track this shipment
            </SoftButton>
            <SoftButton
              variant="outlined"
              color="dark"
              onClick={() => {
                setCreated(null);
                setBooking((b) => ({
                  ...b,
                  recipient: "",
                  phone: "",
                  destination: "",
                  notes: "",
                }));
              }}
            >
              Send another
            </SoftButton>
          </Box>
        </Card>
      );
    return (
      <Grid container spacing={3}>
        <Grid item xs={12} lg={8}>
          <Card sx={{ p: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold">
              Delivery details
            </SoftTypography>
            <SoftTypography
              variant="button"
              color="text"
              fontWeight="regular"
              mb={3}
            >
              All fields except instructions are required.
            </SoftTypography>
            <Box
              component="form"
              onSubmit={(event: React.FormEvent) => {
                event.preventDefault();
                if (bookingValid) setReview(true);
                else
                  setMessage(
                    "Enter valid contact details, addresses and weight (0.1–30 kg).",
                  );
              }}
            >
              <Grid container spacing={2.5}>
                <Grid item xs={12} sm={6}>
                  <Field
                    label="Recipient name"
                    id="recipient"
                    value={booking.recipient}
                    onChange={updateBooking("recipient")}
                    required
                    inputProps={{ maxLength: 100 }}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Field
                    label="Recipient phone"
                    id="recipient-phone"
                    type="tel"
                    value={booking.phone}
                    onChange={updateBooking("phone")}
                    required
                  />
                </Grid>
                <Grid item xs={12}>
                  <Field
                    label="Delivery address"
                    id="delivery-address"
                    value={booking.destination}
                    onChange={updateBooking("destination")}
                    required
                    inputProps={{ minLength: 5, maxLength: 250 }}
                  />
                </Grid>
                <Grid item xs={12}>
                  <Field
                    label="Pickup address"
                    id="pickup-address"
                    value={booking.pickup}
                    onChange={updateBooking("pickup")}
                    required
                    inputProps={{ minLength: 5, maxLength: 250 }}
                  />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <SoftTypography
                    component="label"
                    htmlFor="package-type"
                    variant="caption"
                    fontWeight="bold"
                    display="block"
                    mb={1}
                  >
                    Package type
                  </SoftTypography>
                  <Select
                    native
                    fullWidth
                    value={booking.packageType}
                    onChange={(event) =>
                      setBooking({
                        ...booking,
                        packageType: event.target.value,
                      })
                    }
                    inputProps={{ id: "package-type" }}
                  >
                    <option>Document</option>
                    <option>Small parcel</option>
                    <option>Large parcel</option>
                  </Select>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Field
                    label="Weight (kg)"
                    id="weight"
                    type="number"
                    value={booking.weight}
                    onChange={updateBooking("weight")}
                    required
                    inputProps={{ min: 0.1, max: 30, step: 0.1 }}
                  />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <SoftTypography
                    component="label"
                    htmlFor="service"
                    variant="caption"
                    fontWeight="bold"
                    display="block"
                    mb={1}
                  >
                    Delivery service
                  </SoftTypography>
                  <Select
                    native
                    fullWidth
                    value={booking.service}
                    onChange={(event) =>
                      setBooking({
                        ...booking,
                        service: event.target.value as Shipment["service"],
                      })
                    }
                    inputProps={{ id: "service" }}
                  >
                    <option>Standard</option>
                    <option>Express</option>
                  </Select>
                </Grid>
                <Grid item xs={12}>
                  <Field
                    label="Delivery instructions (optional)"
                    id="instructions"
                    multiline
                    rows={3}
                    value={booking.notes}
                    onChange={updateBooking("notes")}
                    inputProps={{ maxLength: 500 }}
                  />
                </Grid>
                <Grid item xs={12}>
                  <SoftButton type="submit" variant="gradient" color="info">
                    Review shipment{" "}
                    <ArrowRight size={16} style={{ marginLeft: 8 }} />
                  </SoftButton>
                </Grid>
              </Grid>
            </Box>
          </Card>
        </Grid>
        <Grid item xs={12} lg={4}>
          <Card sx={{ p: 3, mb: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold">
              Your address book
            </SoftTypography>
            <SoftTypography
              variant="button"
              color="text"
              fontWeight="regular"
              mb={2}
            >
              Select a saved pickup location.
            </SoftTypography>
            {data.addresses.map((address) => (
              <Box
                key={address.id}
                component="button"
                type="button"
                onClick={() =>
                  setBooking({ ...booking, pickup: address.address })
                }
                aria-pressed={booking.pickup === address.address}
                sx={{
                  display: "flex",
                  gap: 1.5,
                  textAlign: "left",
                  width: "100%",
                  p: 2,
                  mb: 1.5,
                  borderRadius: 2,
                  border: "1px solid",
                  borderColor:
                    booking.pickup === address.address ? "#17c1e8" : "#e9ecef",
                  background: "#fff",
                  cursor: "pointer",
                }}
              >
                <MapPin size={20} />
                <Box>
                  <SoftTypography
                    variant="button"
                    fontWeight="bold"
                    display="block"
                  >
                    {address.label}
                  </SoftTypography>
                  <SoftTypography variant="caption" color="text">
                    {address.address}
                  </SoftTypography>
                </Box>
              </Box>
            ))}
            <SoftButton
              variant="text"
              color="info"
              onClick={() => go("settings")}
            >
              Manage addresses
            </SoftButton>
          </Card>
          <SoftBox variant="gradient" bgColor="dark" borderRadius="lg" p={3}>
            <SoftTypography variant="h6" color="white">
              Estimated shipping
            </SoftTypography>
            <SoftTypography variant="h3" fontWeight="bold" color="white" mt={2}>
              {Number(booking.weight) > 0 && Number(booking.weight) <= 30
                ? currency(
                    estimatePrice(Number(booking.weight), booking.service),
                  )
                : "—"}
            </SoftTypography>
            <SoftTypography
              variant="button"
              fontWeight="regular"
              color="white"
              opacity={0.8}
            >
              Demo estimate only. No payment is collected.
            </SoftTypography>
            <SoftTypography
              variant="caption"
              color="white"
              display="block"
              mt={2}
            >
              LKR 450 base + LKR 150 per started kg. Express adds LKR 350.
            </SoftTypography>
          </SoftBox>
        </Grid>
      </Grid>
    );
  }

  function trackPage() {
    return (
      <>
        <Card sx={{ p: 3, mb: 3 }}>
          <Box
            component="form"
            onSubmit={(event: React.FormEvent) => {
              event.preventDefault();
              setTrackedId(tracking.trim());
              setParams({ id: tracking.trim() });
            }}
            sx={{ display: "flex", flexWrap: "wrap", gap: 2 }}
          >
            <Box sx={{ flex: "1 1 220px" }}>
              <Field
                label="Tracking number"
                id="tracking-number"
                value={tracking}
                onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                  setTracking(event.target.value)
                }
                placeholder="e.g. SD-48291"
                required
              />
            </Box>
            <SoftButton
              type="submit"
              variant="gradient"
              color="info"
              sx={{ alignSelf: "end" }}
            >
              Track shipment
            </SoftButton>
          </Box>
        </Card>
        {selected ? (
          <Grid container spacing={3}>
            <Grid item xs={12} md={7}>
              <TimelineList title={`Tracking ${selected.id}`}>
                {trackingSteps(selected.status).map((step, index) => (
                  <TimelineItem
                    key={step.title}
                    title={step.title}
                    icon={
                      step.complete ? "check_circle" : "radio_button_unchecked"
                    }
                    color={step.complete ? "success" : "secondary"}
                    dateTime={
                      step.complete
                        ? index === 0
                          ? dateLabel(selected.bookedAt)
                          : "Completed"
                        : "Pending"
                    }
                    description={step.description}
                    lastItem={index === 3}
                  />
                ))}
              </TimelineList>
            </Grid>
            <Grid item xs={12} md={5}>
              <Card sx={{ p: 3 }}>
                <Box
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    mb: 3,
                  }}
                >
                  <SoftTypography variant="h6" fontWeight="bold">
                    Shipment details
                  </SoftTypography>
                  <SoftBadge
                    badgeContent={selected.status}
                    color={statusColor(selected.status)}
                    container
                    size="xs"
                  />
                </Box>
                <Details
                  items={[
                    ["Recipient", selected.recipient],
                    ["Phone", selected.phone],
                    ["From", selected.pickup],
                    ["To", selected.destination],
                    [
                      "Package",
                      `${selected.packageType} · ${selected.weight} kg`,
                    ],
                    ["Service", selected.service],
                    ["Demo estimate", currency(selected.amount)],
                    ...(selected.notes
                      ? [["Instructions", selected.notes]]
                      : []),
                  ]}
                />
                <SoftButton
                  variant="outlined"
                  color="info"
                  sx={{ mt: 3 }}
                  onClick={async () => {
                    try {
                      await navigator.clipboard.writeText(selected.id);
                      setMessage("Tracking number copied.");
                    } catch {
                      setMessage(`Copy this tracking number: ${selected.id}`);
                    }
                  }}
                >
                  <Copy size={15} style={{ marginRight: 8 }} />
                  Copy tracking number
                </SoftButton>
                {selected.status === "Awaiting pickup" && (
                  <SoftButton
                    variant="text"
                    color="error"
                    onClick={() => {
                      setData((previous) => ({
                        ...previous,
                        shipments: previous.shipments.map((s) =>
                          s.id === selected.id
                            ? { ...s, status: "Cancelled" }
                            : s,
                        ),
                      }));
                      setMessage("Demo shipment cancelled.");
                    }}
                    sx={{ mt: 2 }}
                  >
                    Cancel demo shipment
                  </SoftButton>
                )}
              </Card>
            </Grid>
          </Grid>
        ) : (
          <Card>
            <Empty
              title="Shipment not found"
              text="Check the number. Only shipments in this demo workspace can be searched."
            />
          </Card>
        )}
      </>
    );
  }

  function historyPage() {
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
                All shipments
              </SoftTypography>
              <SoftTypography
                variant="button"
                color="text"
                fontWeight="regular"
              >
                {filtered.length} matching records
              </SoftTypography>
            </Box>
            <SoftButton
              variant="outlined"
              color="dark"
              onClick={exportHistory}
              disabled={!filtered.length}
            >
              <Download size={15} style={{ marginRight: 8 }} />
              Export CSV
            </SoftButton>
          </Box>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={8}>
              <Field
                label="Search history"
                id="history-search"
                value={query}
                placeholder="Tracking number, recipient or destination"
                onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                  setParams({ q: event.target.value }, { replace: true })
                }
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <SoftTypography
                component="label"
                htmlFor="status-filter"
                variant="caption"
                fontWeight="bold"
                display="block"
                mb={1}
              >
                Status
              </SoftTypography>
              <Select
                native
                fullWidth
                value={status}
                onChange={(event) => setStatus(event.target.value)}
                inputProps={{ id: "status-filter" }}
              >
                <option>All</option>
                {statuses.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </Select>
            </Grid>
          </Grid>
        </Box>
        {shipmentTable(filtered.slice(currentPage * 5, currentPage * 5 + 5))}
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: 1,
            p: 3,
          }}
        >
          <SoftTypography variant="caption" color="text">
            Page {currentPage + 1} of {pageCount}
          </SoftTypography>
          <Box sx={{ display: "flex", gap: 1 }}>
            <SoftButton
              size="small"
              variant="outlined"
              color="secondary"
              disabled={currentPage === 0}
              onClick={() => setPage(currentPage - 1)}
            >
              Previous
            </SoftButton>
            <SoftButton
              size="small"
              variant="gradient"
              color="info"
              disabled={currentPage >= pageCount - 1}
              onClick={() => setPage(currentPage + 1)}
            >
              Next
            </SoftButton>
          </Box>
        </Box>
      </Card>
    );
  }

  function settingsPage() {
    return (
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card sx={{ p: 3, height: "100%" }}>
            <SoftTypography variant="h6" fontWeight="bold" mb={1}>
              Personal details
            </SoftTypography>
            <SoftTypography
              variant="button"
              color="text"
              fontWeight="regular"
              mb={3}
            >
              Saved in this demo workspace only. Your sign-in account is
              unchanged.
            </SoftTypography>
            <Box
              component="form"
              onSubmit={(event: React.FormEvent) => {
                event.preventDefault();
                if (!profile.name.trim()) return;
                setData({
                  ...data,
                  profile: { ...profile, name: profile.name.trim() },
                });
                setMessage("Demo profile saved in this browser tab.");
              }}
              sx={{ display: "grid", gap: 2.5 }}
            >
              <Field
                label="Full name"
                id="profile-name"
                value={profile.name}
                onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                  setProfile({ ...profile, name: event.target.value })
                }
                required
              />
              <Field
                label="Email address"
                id="profile-email"
                type="email"
                value={profile.email}
                onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                  setProfile({ ...profile, email: event.target.value })
                }
                required
              />
              <Field
                label="Phone number"
                id="profile-phone"
                type="tel"
                value={profile.phone}
                onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                  setProfile({ ...profile, phone: event.target.value })
                }
              />
              <SoftButton
                variant="gradient"
                color="info"
                type="submit"
                sx={{ justifySelf: "start" }}
              >
                Save demo profile
              </SoftButton>
            </Box>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card sx={{ p: 3, height: "100%" }}>
            <SoftTypography variant="h6" fontWeight="bold">
              Delivery preferences
            </SoftTypography>
            <SoftTypography
              variant="button"
              color="text"
              fontWeight="regular"
              mb={2}
            >
              Preview your preferences. No messages are sent.
            </SoftTypography>
            {[
              {
                key: "updates" as const,
                title: "Delivery updates",
                text: "Milestones for your active parcels",
              },
              {
                key: "offers" as const,
                title: "Offers & announcements",
                text: "Shipping offers and product news",
              },
            ].map(({ key, title, text }) => (
              <Box
                key={key}
                sx={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  py: 2,
                  borderBottom: "1px solid #f0f2f5",
                }}
              >
                <Box>
                  <SoftTypography
                    variant="button"
                    display="block"
                    fontWeight="bold"
                  >
                    {title}
                  </SoftTypography>
                  <SoftTypography variant="caption" color="text">
                    {text}
                  </SoftTypography>
                </Box>
                <Switch
                  checked={data.preferences[key]}
                  inputProps={{ "aria-label": title }}
                  onChange={(_, checked) => {
                    setData({
                      ...data,
                      preferences: { ...data.preferences, [key]: checked },
                    });
                    setMessage("Demo preference saved.");
                  }}
                />
              </Box>
            ))}
            <SoftBox bgColor="grey-100" borderRadius="lg" p={2} mt={3}>
              <SoftTypography variant="caption" color="text">
                Demo data stays in this browser tab, separate from other
                accounts.
              </SoftTypography>
            </SoftBox>
          </Card>
        </Grid>
        <Grid item xs={12}>
          <Card sx={{ p: 3 }}>
            <Box
              sx={{
                display: "flex",
                justifyContent: "space-between",
                flexWrap: "wrap",
                gap: 2,
                mb: 3,
              }}
            >
              <Box>
                <SoftTypography variant="h6" fontWeight="bold">
                  Saved addresses
                </SoftTypography>
                <SoftTypography
                  variant="button"
                  color="text"
                  fontWeight="regular"
                >
                  Ready for your next pickup.
                </SoftTypography>
              </Box>
              <SoftButton
                variant="gradient"
                color="info"
                onClick={() =>
                  setEditingAddress({
                    id: crypto.randomUUID(),
                    label: "",
                    address: "",
                  })
                }
              >
                <Plus size={16} style={{ marginRight: 8 }} />
                Add address
              </SoftButton>
            </Box>
            <Grid container spacing={2}>
              {data.addresses.map((address) => (
                <Grid item xs={12} md={6} key={address.id}>
                  <SoftBox
                    bgColor="grey-100"
                    borderRadius="lg"
                    p={2.5}
                    display="flex"
                    alignItems="start"
                    gap={1}
                  >
                    <MapPin size={20} />
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <SoftTypography
                        variant="button"
                        fontWeight="bold"
                        display="block"
                      >
                        {address.label}
                      </SoftTypography>
                      <SoftTypography
                        variant="caption"
                        color="text"
                        sx={{ overflowWrap: "anywhere" }}
                      >
                        {address.address}
                      </SoftTypography>
                    </Box>
                    <IconButton
                      aria-label={`Edit ${address.label}`}
                      onClick={() => setEditingAddress({ ...address })}
                    >
                      <Edit2 size={16} />
                    </IconButton>
                    <IconButton
                      aria-label={`Delete ${address.label}`}
                      onClick={() => setDeletingAddress(address)}
                    >
                      <Trash2 size={16} />
                    </IconButton>
                  </SoftBox>
                </Grid>
              ))}
            </Grid>
            {!data.addresses.length && (
              <Empty
                title="Your address book is empty"
                text="Add a pickup address to make booking easier."
              />
            )}
          </Card>
        </Grid>
      </Grid>
    );
  }
  return (
    <Box
      sx={{
        "& .MuiCard-root": { minWidth: 0 },
        "& canvas": { maxWidth: "100%" },
      }}
    >
      <Alert severity="info" sx={{ mb: 3, borderRadius: 3, fontSize: 12 }}>
        Demo workspace · Shipments, prices, profile edits and preferences are
        saved in this browser tab only. No real pickup or payment is made.
      </Alert>
      {storageError && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Browser storage is unavailable. Changes will be lost when you refresh.
        </Alert>
      )}
      <Box sx={{ mb: 3 }}>
        <SoftTypography component="h1" variant="h4" fontWeight="bold">
          {headings[activeTab][0]}
        </SoftTypography>
        <SoftTypography variant="body2" color="text" mt={0.5}>
          {headings[activeTab][1]}
        </SoftTypography>
      </Box>
      {activeTab === "overview" && overview()}
      {activeTab === "send" && sendPage()}
      {activeTab === "track" && trackPage()}
      {activeTab === "history" && historyPage()}
      {activeTab === "settings" && settingsPage()}
      <Dialog
        open={review}
        onClose={() => setReview(false)}
        fullWidth
        maxWidth="sm"
        aria-labelledby="review-title"
      >
        <DialogTitle id="review-title">Review your demo shipment</DialogTitle>
        <DialogContent>
          <Details
            items={[
              ["Recipient", booking.recipient],
              ["Phone", booking.phone],
              ["Pickup", booking.pickup],
              ["Destination", booking.destination],
              [
                "Package",
                `${booking.packageType} · ${booking.weight} kg · ${booking.service}`,
              ],
              [
                "Estimate",
                currency(
                  estimatePrice(Number(booking.weight), booking.service),
                ),
              ],
            ]}
          />
          <FormControlLabel
            control={
              <Checkbox
                checked={accepted}
                onChange={(_, checked) => setAccepted(checked)}
              />
            }
            label={
              <SoftTypography variant="button" fontWeight="regular">
                I understand this creates a demo shipment, not a real courier
                booking.
              </SoftTypography>
            }
          />
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <SoftButton
            color="secondary"
            variant="text"
            onClick={() => setReview(false)}
          >
            Back to edit
          </SoftButton>
          <SoftButton
            color="info"
            variant="gradient"
            disabled={!accepted}
            onClick={confirmBooking}
          >
            Create demo shipment
          </SoftButton>
        </DialogActions>
      </Dialog>
      <Dialog
        open={!!editingAddress}
        onClose={() => setEditingAddress(null)}
        fullWidth
        maxWidth="sm"
        aria-labelledby="address-title"
      >
        <Box
          component="form"
          onSubmit={(event: React.FormEvent) => {
            event.preventDefault();
            if (
              !editingAddress ||
              !editingAddress.label.trim() ||
              editingAddress.address.trim().length < 5
            )
              return;
            const address = {
              ...editingAddress,
              label: editingAddress.label.trim(),
              address: editingAddress.address.trim(),
            };
            setData((previous) => ({
              ...previous,
              addresses: previous.addresses.some((a) => a.id === address.id)
                ? previous.addresses.map((a) =>
                    a.id === address.id ? address : a,
                  )
                : [...previous.addresses, address],
            }));
            setEditingAddress(null);
            setMessage("Address saved.");
          }}
        >
          <DialogTitle id="address-title">
            {data.addresses.some((a) => a.id === editingAddress?.id)
              ? "Edit address"
              : "Add address"}
          </DialogTitle>
          <DialogContent
            sx={{ display: "grid", gap: 3, pt: "12px !important" }}
          >
            <Field
              label="Address label"
              id="address-label"
              value={editingAddress?.label || ""}
              required
              inputProps={{ maxLength: 40 }}
              onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                setEditingAddress(
                  (current) =>
                    current && { ...current, label: event.target.value },
                )
              }
            />
            <Field
              label="Full address"
              id="full-address"
              value={editingAddress?.address || ""}
              required
              inputProps={{ minLength: 5, maxLength: 250 }}
              onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                setEditingAddress(
                  (current) =>
                    current && { ...current, address: event.target.value },
                )
              }
            />
          </DialogContent>
          <DialogActions sx={{ p: 3 }}>
            <SoftButton
              variant="text"
              color="secondary"
              onClick={() => setEditingAddress(null)}
            >
              Cancel
            </SoftButton>
            <SoftButton type="submit" variant="gradient" color="info">
              Save address
            </SoftButton>
          </DialogActions>
        </Box>
      </Dialog>
      <Dialog
        open={!!deletingAddress}
        onClose={() => setDeletingAddress(null)}
        aria-labelledby="delete-title"
      >
        <DialogTitle id="delete-title">
          Delete {deletingAddress?.label}?
        </DialogTitle>
        <DialogContent>
          <SoftTypography variant="body2">
            This removes the saved address. Existing shipment records are kept.
          </SoftTypography>
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <SoftButton
            variant="text"
            color="secondary"
            onClick={() => setDeletingAddress(null)}
          >
            Keep address
          </SoftButton>
          <SoftButton
            variant="gradient"
            color="error"
            onClick={() => {
              setData((previous) => ({
                ...previous,
                addresses: previous.addresses.filter(
                  (a) => a.id !== deletingAddress?.id,
                ),
              }));
              setDeletingAddress(null);
              setMessage("Address removed.");
            }}
          >
            Delete address
          </SoftButton>
        </DialogActions>
      </Dialog>
      <Snackbar
        open={!!message}
        autoHideDuration={4500}
        onClose={() => setMessage("")}
        message={message}
      />
    </Box>
  );
};
