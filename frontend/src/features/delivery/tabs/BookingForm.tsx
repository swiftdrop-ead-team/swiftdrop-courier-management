import { MapPin, Package, UserRound } from "lucide-react";
import {
  DeliveryServiceOptions,
  WorkspaceText as Typography,
} from "@/features/delivery/components";
import React, { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Checkbox,
  FormControlLabel,
  Grid,
  MenuItem,
} from "@mui/material";
import { ActionButton as SoftButton } from "@/features/delivery/components";
import {
  bookingSchema,
  emptyBooking,
  money,
  time,
  type Booking,
  type Quote,
} from "@/features/delivery/model";
import { previewBook, previewQuote } from "@/features/delivery/preview";
import { deliveryApi, errorMessage } from "@/features/delivery/service";
import type { Workspace } from "@/features/delivery/useDeliveryWorkspace";
import { adminService } from "@/services/authService";
import type { User } from "@/types/auth";
import { Field, Panel } from "@/features/delivery/components";

export function BookingForm({
  workspace: w,
  onBooked,
}: {
  workspace: Workspace;
  onBooked: (tracking: string) => void;
}) {
  const [form, setForm] = useState<Booking>(() => ({
    ...emptyBooking(w.profile.fullName),
    senderPhone: w.profile.phone,
    pickupAddress: w.profile.address,
  }));
  const [customers, setCustomers] = useState<User[]>([]);
  const [customerId, setCustomerId] = useState("");
  const [customerLoading, setCustomerLoading] = useState(w.role === "ADMIN");
  const [customerError, setCustomerError] = useState("");
  const [customerRevision, setCustomerRevision] = useState(0);
  useEffect(() => {
    if (w.role !== "ADMIN") return;
    let active = true;
    setCustomerLoading(true); setCustomerError("");
    adminService.getAllUsers().then(users => { if (active) setCustomers(users.filter(u => u.roles.includes("CUSTOMER") && u.status === "ACTIVE")); })
      .catch(error => { if (active) setCustomerError(errorMessage(error)); })
      .finally(() => { if (active) setCustomerLoading(false); });
    return () => { active = false; };
  }, [w.role, customerRevision]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [quote, setQuote] = useState<Quote | null>(null);
  const [estimating, setEstimating] = useState(false);
  const [accepted, setAccepted] = useState(false);
  const [failure, setFailure] = useState("");
  const disabled =
    w.busy ||
    estimating ||
    (w.role === "ADMIN" && (!customerId || customerLoading || Boolean(customerError))) ||
    (w.role === "MERCHANT" &&
      !w.preview &&
      w.user?.merchantApprovalStatus !== "APPROVED");
  const update = (key: keyof Booking, value: string | number) => {
    setForm((previous) => ({ ...previous, [key]: value }));
    setQuote(null);
    setAccepted(false);
    setErrors((previous) => ({ ...previous, [key]: "" }));
    setFailure("");
  };
  const validate = () => {
    const result = bookingSchema.safeParse(form);
    if (!result.success) {
      setErrors(
        Object.fromEntries(
          result.error.issues.map((issue) => [issue.path[0], issue.message]),
        ),
      );
      return null;
    }
    setErrors({});
    return result.data;
  };
  const getQuote = async (event: React.FormEvent) => {
    event.preventDefault();
    const booking = validate();
    if (!booking || disabled) return;
    setEstimating(true);
    setFailure("");
    setQuote(null);
    try {
      setForm(booking);
      setQuote(
        w.preview ? previewQuote(booking) : await deliveryApi.quote(booking, customerId || undefined),
      );
    } catch (error) {
      setFailure(errorMessage(error));
    } finally {
      setEstimating(false);
    }
  };
  const confirm = async () => {
    const booking = validate();
    if (!booking || !quote || !accepted || disabled) return;
    if (new Date(quote.expiresAt).getTime() <= Date.now()) {
      setFailure("Your estimate expired. Request a new estimate.");
      setQuote(null);
      return;
    }
    let tracking = "";
    const ok = await w.act(async () => {
      const parcel = w.preview
        ? previewBook(booking, quote.id, w.ownerId, w.role)
        : await deliveryApi.create(booking, quote.id, quote.id, customerId || undefined);
      tracking = parcel.trackingNumber;
    }, "Your delivery request is confirmed.");
    if (ok) {
      setQuote(null);
      setAccepted(false);
      onBooked(tracking);
    }
  };
  const text = (key: keyof Booking, label: string, type = "text") => (
    <Field
      label={label}
      type={type}
      value={form[key]}
      disabled={disabled}
      required={!["instructions", "cod"].includes(key)}
      error={Boolean(errors[key])}
      helperText={errors[key]}
      onChange={(event) =>
        update(
          key,
          type === "number"
            ? event.target.value === ""
              ? ""
              : Number(event.target.value)
            : event.target.value,
        )
      }
      inputProps={
        type === "number"
          ? {
              min: key === "cod" ? 0 : 0.01,
              step: "any",
              max: key === "weight" ? 30 : key === "cod" ? 100000 : 200,
            }
          : type === "date"
            ? {
                min: new Intl.DateTimeFormat("en-CA", {
                  timeZone: "Asia/Colombo",
                }).format(new Date()),
              }
            : { maxLength: key === "instructions" ? 500 : 250 }
      }
    />
  );
  return (
    <Panel
      title="Delivery details"
      subtitle="Enter parcel details, check your fee, then confirm your pickup."
    >
      {w.role === "ADMIN" && <>
        {customerError && <Alert severity="error" action={<SoftButton onClick={() => setCustomerRevision(n => n + 1)}>Retry</SoftButton>}>{customerError}</Alert>}
        <Field select label="Customer account" value={customerId} required disabled={w.busy || customerLoading}
          helperText={customerLoading ? "Loading customer accounts..." : !customers.length ? "No active customer accounts available." : "The booking will belong to this customer."}
          onChange={event => {
            const id = event.target.value, customer = customers.find(u => u.id === id);
            setCustomerId(id); setQuote(null); setAccepted(false); setFailure("");
            setForm(previous => ({ ...previous, senderName: customer?.fullName || "", senderPhone: customer?.phoneNumber || "", pickupAddress: "" }));
          }}>
          <MenuItem value="">Choose a customer</MenuItem>
          {customers.map(customer => <MenuItem key={customer.id} value={customer.id}>{customer.fullName} - {customer.email}</MenuItem>)}
        </Field>
      </>}
      {w.role === "MERCHANT" &&
        !w.preview &&
        w.user?.merchantApprovalStatus !== "APPROVED" && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            Your business must be approved before you can create delivery
            orders.
          </Alert>
        )}
      {!w.preview && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Choose a pickup date and confirm the quoted delivery fee before booking.
        </Alert>
      )}
      <Box component="form" onSubmit={getQuote} noValidate>
        <Box className="booking-sections">
          <Box component="fieldset">
            <Typography component="legend" variant="h6"><MapPin size={18} aria-hidden="true" />Pickup contact</Typography>
            <Grid container spacing={1.5}>
          <Grid item xs={12} sm={6}>
            {text("senderName", "Sender name")}
          </Grid>
          <Grid item xs={12} sm={6}>
            {text("senderPhone", "Sender phone", "tel")}
          </Grid>
          <Grid item xs={12}>
            {text("pickupAddress", "Pickup address")}
            {w.addresses.length > 0 && (
              <Field
                select
                label="Use a saved pickup address"
                value=""
                onChange={(event) =>
                  update("pickupAddress", event.target.value)
                }
              >
                <MenuItem value="">Choose an address</MenuItem>
                {w.addresses.map((address) => (
                  <MenuItem key={address.id} value={address.address}>
                    {address.label}: {address.address}
                  </MenuItem>
                ))}
              </Field>
            )}
          </Grid>
            </Grid>
          </Box>
          <Box component="fieldset">
            <Typography component="legend" variant="h6"><UserRound size={18} aria-hidden="true" />Recipient</Typography>
            <Grid container spacing={1.5}>
          <Grid item xs={12} sm={6}>
            {text("recipientName", "Recipient name")}
          </Grid>
          <Grid item xs={12} sm={6}>
            {text("recipientPhone", "Recipient phone", "tel")}
          </Grid>
          <Grid item xs={12}>
            {text("dropoffAddress", "Drop-off address")}
          </Grid>
            </Grid>
          </Box>
          <Box component="fieldset">
            <Typography component="legend" variant="h6"><Package size={18} aria-hidden="true" />Parcel and service</Typography>
            <Grid container spacing={1.5}>
          <Grid item xs={12} sm={6}>
            {!w.preview ? (
              <Field
                label="Delivery city"
                value={form.zoneId}
                onChange={(event) => update("zoneId", event.target.value)}
                disabled={disabled}
                error={Boolean(errors.zoneId)}
                helperText={errors.zoneId}
                required
              />
            ) : (
              <Field
                select
                label="Delivery zone"
                value={form.zoneId}
                disabled={disabled}
                error={Boolean(errors.zoneId)}
                helperText={errors.zoneId}
                onChange={(event) => update("zoneId", event.target.value)}
              >
                <MenuItem value="">Choose a zone</MenuItem>
                {w.zones
                  .filter((zone) => zone.active)
                  .map((zone) => (
                    <MenuItem key={zone.id} value={zone.id}>
                      {zone.name}
                    </MenuItem>
                  ))}
              </Field>
            )}
          </Grid>
          <Grid item xs={12} sm={6}>
            <DeliveryServiceOptions
              value={form.service}
              disabled={disabled}
              onChange={(value) => update("service", value)}
            />
          </Grid>
          <Grid item xs={12}>
            {text("description", "Parcel contents")}
          </Grid>
          <Grid item xs={6} md={3}>
            {text("weight", "Weight (kg)", "number")}
          </Grid>
          <Grid item xs={6} md={3}>
            {text("length", "Length (cm)", "number")}
          </Grid>
          <Grid item xs={6} md={3}>
            {text("width", "Width (cm)", "number")}
          </Grid>
          <Grid item xs={6} md={3}>
            {text("height", "Height (cm)", "number")}
          </Grid>
          <Grid item xs={12} sm={6}>
            {text("pickupDate", "Pickup date", "date")}
          </Grid>
          <Grid item xs={12} sm={6}>
            {text("cod", "Cash to collect (LKR)", "number")}
          </Grid>
          <Grid item xs={12}>
            {text("instructions", "Delivery instructions (optional)")}
          </Grid>
        </Grid>
          </Box>
        </Box>
        {failure && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {failure}
          </Alert>
        )}
        <SoftButton
          type="submit"
          disabled={
            disabled || (w.preview && !w.zones.some((zone) => zone.active))
          }
          variant="outlined"
          color="dark"
          sx={{ mt: 3 }}
        >
          {estimating ? "Estimating…" : "Get delivery estimate"}
        </SoftButton>
      </Box>
      {quote && (
        <Box sx={{ mt: 3, p: 3, borderRadius: 3, bgcolor: "#fff8da" }}>
          <Typography variant="body2">Delivery fee</Typography>
          <Typography variant="h4" fontWeight={700}>
            {money(quote.amount, quote.currency)}
          </Typography>
          <Typography variant="caption" display="block">
            Valid until {time(quote.expiresAt)}. Cash on delivery is separate
            from your delivery fee.
          </Typography>
          <FormControlLabel
            control={
              <Checkbox
                checked={accepted}
                onChange={(_, value) => setAccepted(value)}
                disabled={disabled}
              />
            }
            label="I confirm the addresses, parcel details, and delivery fee."
          />
          <Box>
            <SoftButton
              variant="gradient"
              color="primary"
              disabled={disabled || !accepted}
              onClick={() => void confirm()}
            >
              {w.busy ? "Confirming…" : "Confirm delivery request"}
            </SoftButton>
          </Box>
        </Box>
      )}
    </Panel>
  );
}
