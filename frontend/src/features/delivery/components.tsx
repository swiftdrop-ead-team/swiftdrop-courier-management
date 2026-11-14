import React, { useId } from "react";
import {
  Box,
  Card,
  Grid,
  MenuItem,
  TextField,
  Typography,
  type TextFieldProps,
  type ButtonProps,
  type TypographyProps,
} from "@mui/material";
import BaseButton from "components/SoftButton";
import SoftInput from "components/SoftInput";
import SoftBadge from "components/SoftBadge";
import SoftTypography from "components/SoftTypography";
import MiniStatisticsCard from "examples/Cards/StatisticsCards/MiniStatisticsCard";
import TimelineList from "examples/Timeline/TimelineList";
import TimelineItem from "examples/Timeline/TimelineItem";
import {
  labels,
  money,
  time,
  type ParcelRecord,
  type ParcelStatus,
  type PublicTracking,
} from "./model";
import { downloadCsv } from "../../pages/admin/adminData";

// Keep the existing layout props while using the shared Soft UI typography.
export function WorkspaceText({ color, fontWeight, sx, ...props }: TypographyProps & { component?: React.ElementType }) {
  return <SoftTypography {...props} color="inherit" sx={[
    { color: color || "text.primary", fontWeight },
    ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
  ]} />;
}

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

export function Field({
  label,
  id,
  InputLabelProps: _labelProps,
  sx,
  SelectProps,
  helperText,
  InputProps,
  ...props
}: TextFieldProps) {
  const generatedId = useId();
  const fieldId = id || `delivery-field-${generatedId}`;
  return (
    <Box sx={{ width: "100%", minWidth: 0, mt: 1.5, mb: 1 }}>
      {label && (
        <SoftTypography
          component="label"
          htmlFor={fieldId}
          id={`${fieldId}-label`}
          variant="caption"
          fontWeight="bold"
          display="block"
          mb={0.75}
        >
          {label}
          {props.required && <span aria-hidden="true"> *</span>}
        </SoftTypography>
      )}
      {props.select ? <TextField
        fullWidth
        size="small"
        variant="outlined"
        {...props}
        id={fieldId}
        SelectProps={{
          ...SelectProps,
          ...(props.select ? { labelId: `${fieldId}-label` } : {}),
        }}
        helperText={helperText}
        InputProps={InputProps}
        sx={[
          {
            "& .MuiInputBase-root": { minHeight: 44 },
            "& input": { py: 1.25 },
          },
          ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
        ]}
      /> : <>
        <SoftInput
          {...props}
          {...InputProps}
          id={fieldId}
          fullWidth
          inputProps={{
            ...props.inputProps,
            "aria-invalid": Boolean(props.error),
            "aria-describedby": helperText ? `${fieldId}-help` : undefined,
          }}
          sx={[{ minHeight: 44 }, ...(Array.isArray(sx) ? sx : sx ? [sx] : [])]}
        />
        {helperText && <SoftTypography id={`${fieldId}-help`} variant="caption" color={props.error ? "error" : "text"} display="block" mt={0.75}>{helperText}</SoftTypography>}
      </>}
    </Box>
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
    <Card sx={{
      p: { xs: 2.5, md: 3 },
      minWidth: 0,
      height: "100%",
      gap: 1,
      overflow: "hidden",
      borderRadius: "12px",
      boxShadow: "0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04)",
      border: "1px solid rgba(0, 0, 0, 0.06)",
      transition: "box-shadow 200ms ease",
      "&:hover": {
        boxShadow: "0 4px 12px rgba(0, 0, 0, 0.08), 0 2px 4px rgba(0, 0, 0, 0.04)",
      },
    }}>
      <SoftTypography component="h2" variant="h6" fontWeight="bold" mb={0.5} sx={{
        fontSize: "1.05rem",
        letterSpacing: "-0.01em",
      }}>
        {title}
      </SoftTypography>
      {subtitle && (
        <Typography variant="body2" color="text.secondary" mb={2.5} sx={{
          fontSize: "0.82rem",
          lineHeight: 1.5,
        }}>
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
    <SoftBadge
      size="sm"
      badgeContent={labels[value]}
      container
      color={
        value === "DELIVERED"
          ? "success"
          : value === "FAILED"
            ? "error"
            : value === "CANCELLED"
              ? "secondary"
              : "info"
      }
      variant="contained"
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
          <MiniStatisticsCard
            title={{ text: label, fontWeight: "medium" }}
            count={value}
            icon={{
              color: "primary",
              component: [
                "inventory_2",
                "local_shipping",
                "task_alt",
                "payments",
              ][items.findIndex((item) => item.label === label) % 4],
            }}
            percentage={{ color: "success", text: "" }}
          />
        </Grid>
      ))}
    </Grid>
  );
}
export function TrackingTimeline({ record }: { record: PublicTracking }) {
  return (
    <Box aria-label="Tracking history" sx={{ mt: 3 }}>
      <TimelineList title="Tracking history">
        {record.history.map((entry, index) => (
          <TimelineItem
            key={`${entry.timestamp}-${index}`}
            title={labels[entry.status]}
            dateTime={time(entry.timestamp)}
            description={entry.note || ""}
            icon={entry.status === "DELIVERED" ? "task_alt" : "local_shipping"}
            color={
              entry.status === "FAILED"
                ? "error"
                : entry.status === "DELIVERED"
                  ? "success"
                  : "primary"
            }
            lastItem={index === record.history.length - 1}
          />
        ))}
      </TimelineList>
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
      "Fee",
      "Currency",
      "Booked at",
      "Pickup date",
      "Cash on delivery",
      "Failure reason",
    ],
    ...parcels.map((p) => [
      p.trackingNumber,
      p.recipientName,
      p.dropoffAddress,
      labels[p.status],
      p.amount,
      p.currency || "LKR",
      p.createdAt,
      p.pickupDate,
      p.cod,
      p.failureReason || "",
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
    ["Delivery fee", money(parcel.amount, parcel.currency)],
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
