import { AdminPagination } from "@/pages/admin/AdminPagination";
import { RouteStops } from "@/pages/courier/tabs/RouteStops";
import { WorkspacePagination } from "@/components/ui/WorkspacePagination";
import SoftTypography from "components/SoftTypography";
import { WorkspaceText as Typography } from "@/features/delivery/components";
import Table from "examples/Tables/Table";
import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Grid,
} from "@mui/material";
import { ActionButton as SoftButton } from "@/features/delivery/components";
import { Link, useSearchParams } from "react-router-dom";
import {
  day,
  labels,
  money,
  nextStatus,
  publicProjection,
  statuses,
  terminal,
  time,
  type ParcelRecord,
  type ParcelStatus,
} from "@/features/delivery/model";
import { previewStatus } from "@/features/delivery/preview";
import { deliveryApi } from "@/features/delivery/service";
import type { Workspace } from "@/features/delivery/useDeliveryWorkspace";
import {
  downloadReceipt,
  Empty,
  exportParcels,
  Field,
  Panel,
  SelectField,
  Status,
  TrackingTimeline,
} from "@/features/delivery/components";

export function ParcelList({
  workspace: w,
  history = false,
  route = false,
}: {
  workspace: Workspace;
  history?: boolean;
  route?: boolean;
}) {
  const Pagination = w.role === "ADMIN" ? AdminPagination : WorkspacePagination;
  const [params] = useSearchParams();
  const searchQuery = params.get("q") || "";
  const [query, setQuery] = useState(searchQuery);
  const statusQuery = statuses.includes(params.get("status") as ParcelStatus)
    ? params.get("status")!
    : "ALL";
  const [status, setStatus] = useState(statusQuery);
  const [date, setDate] = useState(
    w.role === "COURIER" && !history ? day() : "",
  );
  const [page, setPage] = useState(0);
  const [rows, setRows] = useState(5);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [note, setNote] = useState("");
  const [proof, setProof] = useState("");
  const [cod, setCod] = useState(false);
  const [action, setAction] = useState<ParcelStatus | null>(null);
  const list = w.parcels.filter(
    (parcel) =>
      (!history || terminal(parcel.status)) &&
      (!route || !terminal(parcel.status)) &&
      (status === "ALL" || parcel.status === status) &&
      (!date ||
        (w.role === "COURIER" ? parcel.assignedDate : day(parcel.createdAt)) ===
          date) &&
      `${parcel.trackingNumber} ${parcel.recipientName} ${parcel.dropoffAddress}`
        .toLowerCase()
        .includes(query.toLowerCase().trim()),
  );
  const currentPage = Math.min(
    page,
    Math.max(0, Math.ceil(list.length / rows) - 1),
  );
  const selected = w.parcels.find((parcel) => parcel.id === selectedId);
  useEffect(() => {
    setPage(0);
    setSelectedId(null);
    setQuery(searchQuery);
    setStatus(statusQuery);
    setDate(w.role === "COURIER" && !history && !searchQuery ? day() : "");
  }, [history, route, w.role, searchQuery, statusQuery]);
  const open = (parcel: ParcelRecord) => {
    setSelectedId(parcel.id);
    setNote("");
    setProof("");
    setCod(false);
    setAction(null);
    w.setActionError("");
  };
  const changeStatus = async () => {
    if (!selected || !action) return;
    if (action === "FAILED" && !note.trim()) {
      w.setActionError("Enter a reason for the failed delivery.");
      return;
    }
    if (
      action === "DELIVERED" &&
      (!proof.trim() || (selected.cod > 0 && !cod))
    ) {
      w.setActionError(
        "Enter the recipient name and confirm any cash collection.",
      );
      return;
    }
    const ok = await w.act(async () => {
      if (w.preview)
        previewStatus(
          selected.id,
          action,
          note,
          proof,
          w.role === "COURIER" ? w.ownerId : undefined,
        );
      else if (action === "CANCELLED") await deliveryApi.cancel(selected.id);
      else await deliveryApi.status(selected.id, action, note, proof);
    }, `Delivery updated: ${labels[action]}.`);
    if (ok) {
      setAction(null);
      setSelectedId(null);
    }
  };
  return (
    <Panel
      actions={
        <SoftButton
            variant="outlined"
            color="dark"
            disabled={!list.length}
            onClick={() => exportParcels(list)}
          >
            Export CSV
          </SoftButton>
      }
      title={
        route
          ? "Pickup and drop-off stops"
          : history
            ? "Completed shipments"
            : w.role === "COURIER"
              ? "Assigned deliveries"
              : w.role === "ADMIN" ? "Shipment register" : "Your shipments"
      }
      subtitle={
        route
          ? "Review each pickup and drop-off. Directions open in Google Maps."
          : "Search, filter, inspect delivery details, and download receipts."
      }
    >
      <Box sx={{ bgcolor: "#f7f9fc", border: "1px solid #e5eaf0", borderRadius: 2, px: 2, py: 0.5, mt: 1 }}>
      <Grid container spacing={1.5} alignItems="flex-end">
        <Grid item xs={12} md={6}>
          <Field
            label="Search deliveries"
            placeholder="Tracking number, recipient or address"
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setPage(0);
            }}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <SelectField
            label="Delivery status"
            value={status}
            onChange={(value) => {
              setStatus(value);
              setPage(0);
            }}
            options={[
              { value: "ALL", label: "All statuses" },
              ...statuses.map((value) => ({ value, label: labels[value] })),
            ]}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Field
            type="date"
            label={
              w.role === "COURIER"
                ? "Assigned date (clear for all)"
                : "Booking date (clear for all)"
            }
            value={date}
            onChange={(event) => {
              setDate(event.target.value);
              setPage(0);
            }}
          />
        </Grid>

      </Grid>
      </Box>
      {route && list.length > 0 ? (
        <RouteStops parcels={list.slice(currentPage * rows, (currentPage + 1) * rows)} onOpen={open} />
      ) : list.length > 0 && <Box mt={1} sx={{ minWidth: 0, "& thead": { bgcolor: "#f7f9fc" }, "& tbody tr:hover": { bgcolor: "#fafbfd" } }}>
        <Table
          aria-label="Deliveries"
          columns={[
            "Tracking number",
            "Recipient / destination",
            "Status",
            "Fee",
            "Action",
          ].map((name) => ({ name, align: "left" }))}
          rows={list
            .slice(currentPage * rows, (currentPage + 1) * rows)
            .map((parcel) => ({
              id: parcel.id,
              hasBorder: true,
              "Tracking number": (
                <>
                  <Typography
                    component="span"
                    display="block"
                    variant="button"
                    fontWeight={700}
                  >
                    {parcel.trackingNumber}
                  </Typography>
                  <Typography component="span" variant="caption">
                    {time(parcel.createdAt)}
                  </Typography>
                </>
              ),
              "Recipient / destination": (
                <>
                  <Typography component="span" display="block" variant="button">
                    {parcel.recipientName}
                  </Typography>
                  <Typography component="span" variant="caption">
                    {parcel.dropoffAddress}
                  </Typography>
                </>
              ),
              Status: <Status value={parcel.status} />,
              Fee: money(parcel.amount, parcel.currency),
              Action: (
                <SoftButton
                  variant="text"
                  color="dark"
                  onClick={() => open(parcel)}
                  aria-label={`View ${parcel.trackingNumber}`}
                >
                  View details
                </SoftButton>
              ),
            }))}
        />
      </Box>}
      {!list.length && (
        <Empty
          text={
            w.errors.parcels
              ? "Delivery records could not be loaded. Use Refresh data to retry."
              : w.role === "COURIER"
                ? "No assignments match these filters. Choose another date or clear your filters."
                : "No deliveries match. Clear the filters or create a booking."
          }
        />
      )}
      {list.length > 0 && <Pagination count={list.length} page={currentPage} pageSize={rows}
        onPageChange={setPage} onPageSizeChange={setRows} />}
      <Dialog
        open={Boolean(selected)}
        onClose={() => {
          if (!w.busy) setSelectedId(null);
        }}
        fullWidth
        maxWidth="md"
        aria-labelledby="parcel-details-title"
      >
        <DialogTitle id="parcel-details-title">
          <SoftTypography component="span" variant="h5" fontWeight="bold">
            Delivery {selected?.trackingNumber}
          </SoftTypography>
        </DialogTitle>
        {selected && (
          <DialogContent>
            <Status value={selected.status} />
            <Grid container spacing={3} sx={{ mt: 1 }}>
              <Grid item xs={12} md={6}>
                <Typography fontWeight={700}>Pickup</Typography>
                <Typography>
                  {selected.senderName} · {selected.senderPhone}
                </Typography>
                <Typography variant="body2">
                  {selected.pickupAddress}
                </Typography>
                <Typography fontWeight={700} mt={2}>
                  Drop-off
                </Typography>
                <Typography>
                  {selected.recipientName} · {selected.recipientPhone}
                </Typography>
                <Typography variant="body2">
                  {selected.dropoffAddress}
                </Typography>
                <Typography variant="body2" mt={2}>
                  {selected.description} · {selected.weight} kg ·{" "}
                  {selected.length} × {selected.width} × {selected.height} cm
                </Typography>
                <Typography variant="body2">
                  Pickup: {selected.pickupDate} · {selected.service}
                </Typography>
                <Typography variant="body2">
                  Fee: {money(selected.amount, selected.currency)} · Cash to
                  collect: {money(selected.cod)}
                </Typography>
                {selected.instructions && (
                  <Alert severity="info" sx={{ mt: 2 }}>
                    {selected.instructions}
                  </Alert>
                )}
                {selected.recipientProof && (
                  <Typography mt={2}>
                    Received by: {selected.recipientProof}
                  </Typography>
                )}
                {selected.failureReason && (
                  <Alert severity="warning" sx={{ mt: 2 }}>
                    {selected.failureReason}
                  </Alert>
                )}
                <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap", mt: 3 }}>
                  <SoftButton
                    variant="outlined"
                    color="dark"
                    onClick={() => downloadReceipt(selected)}
                  >
                    Download receipt
                  </SoftButton>
                  <SoftButton
                    component={Link}
                    to={`${w.preview ? "/tracking-preview" : "/track"}/${encodeURIComponent(selected.trackingNumber)}`}
                    variant="text"
                    color="dark"
                  >
                    Public tracking
                  </SoftButton>
                  {w.role === "COURIER" && (
                    <SoftButton
                      component="a"
                      href={`https://www.google.com/maps/dir/?api=1&origin=${encodeURIComponent(selected.pickupAddress)}&destination=${encodeURIComponent(selected.dropoffAddress)}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      variant="text"
                      color="dark"
                    >
                      Directions
                    </SoftButton>
                  )}
                </Box>
              </Grid>
              <Grid item xs={12} md={6}>
                <TrackingTimeline
                  record={{
                    ...publicProjection(selected),
                    history: selected.history,
                  }}
                />
              </Grid>
            </Grid>
            {action && (
              <Box sx={{ mt: 2, p: 2, bgcolor: "#f8fafc", borderRadius: 2 }}>
                <Typography fontWeight={700}>
                  Confirm: {labels[action]}
                </Typography>
                {action !== "CANCELLED" && (
                  <Field
                    multiline
                    minRows={2}
                    label={
                      action === "FAILED"
                        ? "Failure reason (required)"
                        : "Delivery note (optional)"
                    }
                    value={note}
                    onChange={(event) => setNote(event.target.value)}
                    inputProps={{ maxLength: 500 }}
                    disabled={w.busy}
                  />
                )}
                {action === "DELIVERED" && (
                  <>
                    <Field
                      label="Recipient name (proof of delivery)"
                      value={proof}
                      onChange={(event) => setProof(event.target.value)}
                      disabled={w.busy}
                      required
                    />
                    {selected.cod > 0 && (
                      <FormControlLabel
                        label={`I collected ${money(selected.cod)}`}
                        control={
                          <Checkbox
                            checked={cod}
                            onChange={(_, value) => setCod(value)}
                            disabled={w.busy}
                          />
                        }
                      />
                    )}
                  </>
                )}
                {action === "CANCELLED" && (
                  <Typography variant="body2">
                    This closes your unassigned delivery request.
                  </Typography>
                )}
              </Box>
            )}
            {w.actionError && (
              <Alert severity="error" sx={{ mt: 2 }}>
                {w.actionError}
              </Alert>
            )}
          </DialogContent>
        )}
        <DialogActions sx={{ p: 3, gap: 1, flexWrap: "wrap" }}>
          <SoftButton
            disabled={w.busy}
            onClick={() => setSelectedId(null)}
            variant="text"
            color="dark"
          >
            Close
          </SoftButton>
          {selected &&
            !terminal(selected.status) &&
            (action ? (
              <>
                <SoftButton
                  disabled={w.busy}
                  onClick={() => setAction(null)}
                  variant="text"
                  color="dark"
                >
                  Back
                </SoftButton>
                <SoftButton
                  disabled={w.busy}
                  onClick={() => void changeStatus()}
                  variant="gradient"
                  color="primary"
                >
                  {w.busy ? "Saving…" : "Confirm update"}
                </SoftButton>
              </>
            ) : w.role === "COURIER" ? (
              <>
                {nextStatus[selected.status] && (
                  <SoftButton
                    variant="gradient"
                    color="primary"
                    onClick={() => setAction(nextStatus[selected.status]!)}
                  >{`Mark ${labels[nextStatus[selected.status]!].toLowerCase()}`}</SoftButton>
                )}
                <SoftButton
                  variant="outlined"
                  color="error"
                  onClick={() => setAction("FAILED")}
                >
                  Report failed delivery
                </SoftButton>
              </>
            ) : (
              selected.status === "CREATED" &&
              !selected.courierId && (
                <SoftButton
                  variant="outlined"
                  color="error"
                  onClick={() => setAction("CANCELLED")}
                >
                  Cancel booking
                </SoftButton>
              )
            ))}
        </DialogActions>
      </Dialog>
    </Panel>
  );
}
