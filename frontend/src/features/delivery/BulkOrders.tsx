import { WorkspaceText as Typography } from "./components";
import React, { useRef, useState } from "react";
import {
  Alert,
  Box,
  Checkbox,
  FormControlLabel,
} from "@mui/material";
import { FileSpreadsheet, Upload } from "lucide-react";
import { ActionButton as SoftButton } from "./components";
import { downloadCsv } from "../../pages/admin/adminData";

import { bookingSchema, day, money, type Booking, type Quote } from "./model";
import { previewBookMany, previewQuote } from "./preview";
import { deliveryApi, errorMessage } from "./service";
import type { Workspace } from "./useDeliveryWorkspace";
import { Panel } from "./components";

export const csvHeaders: (keyof Booking)[] = [
  "senderName",
  "senderPhone",
  "pickupAddress",
  "recipientName",
  "recipientPhone",
  "dropoffAddress",
  "zoneId",
  "description",
  "weight",
  "length",
  "width",
  "height",
  "service",
  "pickupDate",
  "instructions",
  "cod",
];
// RFC 4180-style quoted fields, embedded line breaks, and escaped quotes.
export function parseCsv(text: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let field = "";
  let quoted = false;
  let closed = false;
  text = text.replace(/^\uFEFF/, "");
  for (let i = 0; i < text.length; i++) {
    const character = text[i];
    if (quoted) {
      if (character === '"') {
        if (text[i + 1] === '"') {
          field += '"';
          i++;
        } else {
          quoted = false;
          closed = true;
        }
      } else field += character;
    } else if (character === '"') {
      if (field || closed) throw new Error("Invalid quote in CSV field.");
      quoted = true;
    } else if (character === "," || character === "\n" || character === "\r") {
      row.push(field);
      field = "";
      closed = false;
      if (character !== ",") {
        if (row.some((value) => value.trim())) rows.push(row);
        row = [];
        if (character === "\r" && text[i + 1] === "\n") i++;
      }
    } else {
      if (closed) {
        if (character.trim())
          throw new Error("Unexpected text after a quoted CSV field.");
      } else field += character;
    }
  }
  if (quoted) throw new Error("CSV contains an unclosed quoted field.");
  row.push(field);
  if (row.some((value) => value.trim())) rows.push(row);
  return rows;
}
export function parseOrders(text: string): {
  orders: Booking[];
  errors: string[];
} {
  const rows = parseCsv(text);
  const header = rows.shift()?.map((value) => value.trim());
  if (
    !header ||
    csvHeaders.some((key) => !header.includes(key)) ||
    new Set(header).size !== header.length
  )
    throw new Error(
      "Use the provided CSV template with every required column and no duplicate columns.",
    );
  if (!rows.length || rows.length > 100)
    throw new Error("Upload between 1 and 100 orders per file.");
  const orders: Booking[] = [];
  const errors: string[] = [];
  rows.forEach((row, index) => {
    if (row.length !== header.length) {
      errors.push(`Row ${index + 2}: column count does not match the header.`);
      return;
    }
    const parsed = bookingSchema.safeParse(
      Object.fromEntries(header.map((key, column) => [key, row[column]])),
    );
    if (parsed.success) orders.push(parsed.data);
    else
      errors.push(
        `Row ${index + 2}: ${parsed.error.issues.map((issue) => `${issue.path[0]}: ${issue.message}`).join("; ")}`,
      );
  });
  return { orders, errors };
}
export function BulkOrders({ workspace: w }: { workspace: Workspace }) {
  const [orders, setOrders] = useState<Booking[]>([]);
  const [quotes, setQuotes] = useState<Quote[]>([]);
  const [errors, setErrors] = useState<string[]>([]);
  const [estimating, setEstimating] = useState(false);
  const [accepted, setAccepted] = useState(false);
  const [fileName, setFileName] = useState("");
  const fileSequence = useRef(0);
  const idempotencyKey = useRef(crypto.randomUUID());
  const completed = useRef(new Set<number>());
  const [validFile, setValidFile] = useState(false);
  const blocked = !w.preview && w.user?.merchantApprovalStatus !== "APPROVED";
  const upload = async (file?: File) => {
    const sequence = ++fileSequence.current;
    setValidFile(false);
    setOrders([]);
    setQuotes([]);
    setErrors([]);
    setAccepted(false);
    setFileName(file?.name || "");
    idempotencyKey.current = crypto.randomUUID();
    completed.current.clear();
    if (!file) return;
    if (!file.name.toLowerCase().endsWith(".csv")) {
      setErrors(["Choose a .csv file."]);
      return;
    }
    if (file.size > 1024 * 1024) {
      setErrors(["Use a CSV file smaller than 1 MB."]);
      return;
    }
    try {
      const result = parseOrders(await file.text());
      if (sequence === fileSequence.current) {
        setOrders(result.orders);
        setErrors(result.errors);
        setValidFile(result.errors.length === 0);
      }
    } catch (error) {
      if (sequence === fileSequence.current) setErrors([errorMessage(error)]);
    }
  };
  const estimateAll = async () => {
    if (estimating || !orders.length || !validFile) return;
    setEstimating(true);
    setQuotes([]);
    setAccepted(false);
    setErrors([]);
    try {
      const result = w.preview
        ? orders.map(previewQuote)
        : await Promise.all(orders.map((order) => deliveryApi.quote(order)));
      if (result.length !== orders.length)
        throw new Error(
          "The pricing service did not return a quote for every order.",
        );
      setQuotes(result);
    } catch (error) {
      setErrors([errorMessage(error)]);
    } finally {
      setEstimating(false);
    }
  };
  const confirm = async () => {
    if (!accepted || quotes.length !== orders.length || !orders.length) return;
    if (
      quotes.some((quote) => new Date(quote.expiresAt).getTime() <= Date.now())
    ) {
      setErrors(["One or more quotes have expired. Request new estimates."]);
      setQuotes([]);
      return;
    }
    const ok = await w.act(async () => {
      if (w.preview)
        previewBookMany(
          orders,
          quotes.map((quote) => quote.id),
          w.ownerId,
          w.role,
        );
      else {
        for (let index = 0; index < orders.length; index++) {
          if (completed.current.has(index)) continue;
          try {
            await deliveryApi.create(
              orders[index],
              quotes[index].id,
              `${idempotencyKey.current}-${index}`,
            );
            completed.current.add(index);
          } catch (error) {
            throw new Error(
              `${completed.current.size} of ${orders.length} orders created. Retry this batch to continue safely. ${errorMessage(error)}`,
            );
          }
        }
      }
    }, `${orders.length} delivery orders created.`);
    if (ok) {
      setOrders([]);
      setQuotes([]);
      setAccepted(false);
      setFileName("");
    }
  };
  return (
    <Panel
      title="Upload delivery orders"
      subtitle="Upload up to 100 orders, validate the file, review delivery fees, and confirm the batch."
    >
      {blocked && (
        <Alert severity="warning">
          Business approval is required before uploading orders.
        </Alert>
      )}
      <Box sx={{ display: "grid", gap: 2, mt: 1 }}>
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            flexWrap: "wrap",
            gap: 2,
            p: 2,
            borderRadius: 2,
            bgcolor: "#f8fafc",
          }}
        >
          <Box sx={{ minWidth: 0 }}>
            <Typography fontWeight={700}>1. Prepare your CSV</Typography>
            <Typography variant="body2" color="text.secondary">
              Start with the template and include 1–100 orders.
            </Typography>
          </Box>
          <SoftButton
            variant="outlined"
            color="dark"
            onClick={() =>
              downloadCsv("swiftdrop-orders-template.csv", [
                csvHeaders,
                [
                  "Store owner",
                  "0771234567",
                  "24 Store Road, Colombo",
                  "Nimal Perera",
                  "0772345678",
                  "18 Main Street, Kandy",
                  w.zones.find((zone) => zone.active)?.id || "kandy",
                  "Clothing",
                  1,
                  20,
                  15,
                  10,
                  "STANDARD",
                  day(),
                  "",
                  0,
                ],
              ])
            }
          >
            Download CSV template
          </SoftButton>
        </Box>
        <Alert severity="info">
          Delivery cities:{" "}
          {w.zones
            .filter((zone) => zone.active)
            .map((zone) => `${zone.name}: ${zone.id}`)
            .join(" · ") ||
            "Use the delivery city name, for example Kandy or Colombo"}
          . Use STANDARD or EXPRESS for the service.
        </Alert>
        <Box
          component="label"
          htmlFor="orders-file"
          sx={{
            position: "relative",
            display: "flex",
            alignItems: "center",
            flexWrap: { xs: "wrap", sm: "nowrap" },
            gap: 2,
            minHeight: 112,
            p: 2.5,
            border: "1px dashed #94a3b8",
            borderRadius: 2,
            bgcolor: "#f8fafc",
            cursor:
              blocked || w.busy || estimating ? "not-allowed" : "pointer",
            "&:hover": {
              borderColor: blocked ? "#94a3b8" : "#00274c",
              bgcolor: blocked ? "#f8fafc" : "#f1f5f9",
            },
            "&:focus-within": {
              outline: "3px solid rgba(0, 39, 76, 0.2)",
              outlineOffset: 2,
            },
          }}
          onDragOver={(event) => event.preventDefault()}
          onDrop={(event) => {
            event.preventDefault();
            if (!blocked && !w.busy && !estimating)
              void upload(event.dataTransfer.files?.[0]);
          }}
        >
          <FileSpreadsheet size={28} aria-hidden="true" color="#00274c" />
          <Box sx={{ minWidth: 0, flex: 1 }}>
            <Typography fontWeight={700}>
              {fileName || "2. Choose or drop your CSV file"}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              CSV only · max 1 MB · up to 100 orders
            </Typography>
            {fileName && (
              <Typography variant="caption" color="text.secondary">
                {orders.length} valid orders
              </Typography>
            )}
          </Box>
          <Box
            sx={{
              display: "inline-flex",
              alignItems: "center",
              gap: 1,
              px: 1.5,
              py: 1,
              borderRadius: 1,
              color: "#00274c",
              bgcolor: "#ffcb05",
              fontSize: 14,
              fontWeight: 700,
              whiteSpace: "nowrap",
              ml: { xs: "auto", sm: 0 },
            }}
          >
            <Upload size={16} aria-hidden="true" />
            Browse files
          </Box>
          <Box
            component="input"
            id="orders-file"
            aria-label="Choose orders CSV"
            type="file"
            accept=".csv,text/csv"
            disabled={blocked || w.busy || estimating}
            onChange={(event) => void upload(event.target.files?.[0])}
            sx={{
              position: "absolute",
              inset: 0,
              width: "100%",
              height: "100%",
              opacity: 0,
              cursor: "inherit",
            }}
          />
        </Box>
      </Box>
      {errors.map((error, index) => (
        <Alert severity="error" key={index} sx={{ mt: 1 }}>
          {error}
        </Alert>
      ))}
      <SoftButton
        variant="outlined"
        color="dark"
        disabled={
          blocked || w.busy || estimating || !orders.length || !validFile
        }
        onClick={() => void estimateAll()}
        sx={{ mt: 3, alignSelf: "flex-start" }}
      >
        {estimating ? "Estimating…" : "3. Estimate all delivery fees"}
      </SoftButton>
      {quotes.length > 0 && (
        <Box mt={3}>
          <Typography variant="h5" fontWeight={700}>
            Total delivery fees:{" "}
            {money(quotes.reduce((total, quote) => total + quote.amount, 0))}
          </Typography>
          <Box sx={{ maxHeight: 240, overflowY: "auto", my: 2 }}>
            {orders.map((order, index) => (
              <Typography key={index} variant="body2">
                Row {index + 2}: {order.recipientName} · {order.dropoffAddress}{" "}
                · {money(quotes[index].amount)}
              </Typography>
            ))}
          </Box>
          <FormControlLabel
            label="I have reviewed all orders and accept the delivery fees."
            control={
              <Checkbox
                checked={accepted}
                onChange={(_, checked) => setAccepted(checked)}
                disabled={w.busy}
              />
            }
          />
          <Box>
            <SoftButton
              variant="gradient"
              color="primary"
              disabled={!accepted || w.busy || blocked}
              onClick={() => void confirm()}
            >
              Confirm batch of {orders.length} orders
            </SoftButton>
          </Box>
        </Box>
      )}
    </Panel>
  );
}
