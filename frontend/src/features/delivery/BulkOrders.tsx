import React, { useRef, useState } from "react";
import {
  Alert,
  Box,
  Checkbox,
  FormControlLabel,
  Typography,
} from "@mui/material";
import { ActionButton as SoftButton } from "./components";
import { downloadCsv } from "../../pages/admin/adminData";
import { api } from "../../services/api";
import { bookingSchema, day, money, type Booking, type Quote } from "./model";
import { previewBookMany, previewQuote } from "./preview";
import { errorMessage } from "./service";
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
    if (!file) return;
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
        : (await api.post<Quote[]>("/pricing/quotes/bulk", { orders })).data;
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
      else
        await api.post(
          "/parcels/bulk",
          {
            orders: orders.map((booking, index) => ({
              ...booking,
              quoteId: quotes[index].id,
            })),
          },
          { headers: { "Idempotency-Key": idempotencyKey.current } },
        );
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
      <Typography variant="caption" display="block" mt={2}>
        Zone IDs:{" "}
        {w.zones
          .filter((zone) => zone.active)
          .map((zone) => `${zone.name}: ${zone.id}`)
          .join(" · ") || "No zones loaded"}
        . Use STANDARD or EXPRESS for the service.
      </Typography>
      <Box mt={3}>
        <label htmlFor="orders-file">Choose your orders CSV</label>
        <Box
          component="input"
          id="orders-file"
          type="file"
          accept=".csv,text/csv"
          disabled={blocked || w.busy || estimating}
          onChange={(event) => void upload(event.target.files?.[0])}
          sx={{ display: "block", mt: 1, maxWidth: "100%" }}
        />
      </Box>
      {fileName && (
        <Typography variant="body2" mt={2}>
          {fileName} · {orders.length} valid orders
        </Typography>
      )}
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
        sx={{ mt: 3 }}
      >
        {estimating ? "Estimating…" : "Estimate all delivery fees"}
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
