import React from "react";
import {
  Box,
  Card,
  Chip,
  Grid,
  MenuItem,
  TextField,
  Typography,
  type TextFieldProps,
  type ButtonProps,
} from "@mui/material";
import BaseButton from "components/SoftButton";
import {
  labels,
  money,
  time,
  type ParcelRecord,
  type ParcelStatus,
  type PublicTracking,
} from "./model";
import { downloadCsv } from "../../pages/admin/adminData";

type ActionButtonProps = Omit<ButtonProps, "variant" | "color"> & {
  variant?: "text" | "outlined" | "contained" | "gradient";
  color?: "primary" | "dark" | "error" | "success" | "secondary";
  component?: React.ElementType;
  to?: string;
  target?: string;
  rel?: string;
};
export function ActionButton({
  color = "dark",
  variant = "outlined",
  sx,
  ...props
}: ActionButtonProps) {
  return (
    <BaseButton
      {...props}
      color={color}
      variant={variant}
      sx={[
        {
          alignSelf: "center",
          ...(color === "primary"
            ? { color: "#00274c !important", fontWeight: 700 }
            : {}),
        },
        ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
      ]}
    />
  );
}

export function Field(props: TextFieldProps) {
  return (
    <TextField
      fullWidth
      size="small"
      margin="dense"
      variant="outlined"
      {...props}
      InputLabelProps={{ shrink: true, ...props.InputLabelProps }}
    />
  );
}
export function SelectField({
  label,
  value,
  options,
  onChange,
  disabled = false,
}: {
  label: string;
  value: string;
  options: { value: string; label: string }[];
  onChange: (value: string) => void;
  disabled?: boolean;
}) {
  return (
    <Field
      select
      label={label}
      value={value}
      disabled={disabled}
      onChange={(event) => onChange(event.target.value)}
    >
      {options.map((option) => (
        <MenuItem value={option.value} key={option.value}>
          {option.label}
        </MenuItem>
      ))}
    </Field>
  );
}
export function Panel({
  title,
  children,
  subtitle,
}: {
  title: string;
  children: React.ReactNode;
  subtitle?: string;
}) {
  return (
    <Card sx={{ p: { xs: 2, md: 3 }, minWidth: 0 }}>
      <Typography variant="h6" fontWeight={700} mb={1}>
        {title}
      </Typography>
      {subtitle && (
        <Typography variant="body2" color="text.secondary" mb={2}>
          {subtitle}
        </Typography>
      )}
      {children}
    </Card>
  );
}
export function Empty({
  text = "No records match your filters.",
}: {
  text?: string;
}) {
  return (
    <Typography
      variant="body2"
      color="text.secondary"
      sx={{ py: 4, textAlign: "center" }}
    >
      {text}
    </Typography>
  );
}
export function Status({ value }: { value: ParcelStatus }) {
  return (
    <Chip
      size="small"
      label={labels[value]}
      color={
        value === "DELIVERED"
          ? "success"
          : value === "FAILED"
            ? "error"
            : value === "CANCELLED"
              ? "default"
              : "info"
      }
      variant="outlined"
    />
  );
}
export function Metrics({
  items,
}: {
  items: { label: string; value: string | number }[];
}) {
  return (
    <Grid container spacing={2}>
      {items.map(({ label, value }) => (
        <Grid item xs={6} lg={3} key={label}>
          <Card sx={{ p: 2.5 }}>
            <Typography variant="body2" color="text.secondary">
              {label}
            </Typography>
            <Typography variant="h4" fontWeight={700} mt={1}>
              {value}
            </Typography>
          </Card>
        </Grid>
      ))}
    </Grid>
  );
}
export function TrackingTimeline({ record }: { record: PublicTracking }) {
  return (
    <Box aria-label="Tracking history" sx={{ mt: 3 }}>
      {record.history.map((entry, index) => (
        <Box
          key={`${entry.timestamp}-${index}`}
          sx={{
            borderLeft: "3px solid",
            borderColor:
              index === record.history.length - 1 ? "#ffcb05" : "#e2e8f0",
            pl: 2.5,
            pb: 3,
          }}
        >
          <Typography fontWeight={700} variant="body2">
            {labels[entry.status]}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {time(entry.timestamp)}
          </Typography>
          {entry.note && <Typography variant="body2">{entry.note}</Typography>}
        </Box>
      ))}
    </Box>
  );
}
export function exportParcels(parcels: ParcelRecord[]) {
  downloadCsv("swiftdrop-deliveries.csv", [
    [
      "Tracking number",
      "Recipient",
      "Destination",
      "Status",
      "Fee (LKR)",
      "Booked at",
    ],
    ...parcels.map((p) => [
      p.trackingNumber,
      p.recipientName,
      p.dropoffAddress,
      labels[p.status],
      p.amount,
      p.createdAt,
    ]),
  ]);
}
const escapeHtml = (text: string) =>
  text.replace(
    /[&<>"']/g,
    (character) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        character
      ]!,
  );
export function downloadReceipt(parcel: ParcelRecord) {
  const rows = [
    ["Tracking number", parcel.trackingNumber],
    ["Booked", time(parcel.createdAt)],
    ["Sender", parcel.senderName],
    ["Pickup", parcel.pickupAddress],
    ["Recipient", parcel.recipientName],
    ["Destination", parcel.dropoffAddress],
    ["Parcel", `${parcel.description} / ${parcel.weight} kg`],
    ["Service", parcel.service],
    ["Delivery status", labels[parcel.status]],
    ["Delivery fee", money(parcel.amount)],
    ["Cash on delivery", money(parcel.cod)],
    ["Recipient proof", parcel.recipientProof || "Not recorded"],
  ];
  const html = `<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>SwiftDrop receipt ${escapeHtml(parcel.trackingNumber)}</title><style>body{font:16px system-ui;max-width:760px;margin:40px auto;padding:24px;color:#00274c}h1{border-bottom:5px solid #ffcb05;padding-bottom:20px}dl{display:grid;grid-template-columns:160px 1fr;gap:16px}dd{margin:0;overflow-wrap:anywhere}</style><h1>SwiftDrop delivery receipt</h1><p>Print this receipt from your browser to save a PDF.</p><dl>${rows.map(([label, value]) => `<dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value)}</dd>`).join("")}</dl></html>`;
  const url = URL.createObjectURL(new Blob([html], { type: "text/html" }));
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `${parcel.trackingNumber}-receipt.html`;
  anchor.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
