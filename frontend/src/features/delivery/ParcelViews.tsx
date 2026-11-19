import { WorkspaceText as Typography } from "./components";
import React, { useEffect, useState } from "react";
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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
} from "@mui/material";
import { ActionButton as SoftButton } from "./components";
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
} from "./model";
import { previewStatus, updatePreview } from "./preview";
import { deliveryApi } from "./service";
import { adminService } from "../../services/authService";
import type { User } from "../../types/auth";
import type { Workspace } from "./useDeliveryWorkspace";
import {
  downloadReceipt,
  Empty,
  exportParcels,
  Field,
  Panel,
  SelectField,
  Status,
  TrackingTimeline,
} from "./components";

export function ParcelList({
  workspace: w,
  history = false,
  route = false,
}: {
  workspace: Workspace;
  history?: boolean;
  route?: boolean;
}) {
  const [params] = useSearchParams();
  const searchQuery = params.get("q") || "";
  const [query, setQuery] = useState(searchQuery);
  const statusQuery = statuses.includes(params.get("status") as ParcelStatus) ? params.get("status")! : "ALL";
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
      title={
        route
          ? "Your active route"
          : history
            ? "Delivery history"
            : w.role === "COURIER"
              ? "Assigned deliveries"
              : "Your shipments"
      }
      subtitle={
        route
          ? "Review each pickup and drop-off. Directions open in Google Maps."
          : "Search, filter, inspect delivery details, and download receipts."
      }
    >
      <Grid container spacing={2} alignItems="center">
        <Grid item xs={12} md={4}>
          <Field
            label="Search deliveries"
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setPage(0);
            }}
          />
        </Grid>
        <Grid item xs={12} sm={5} md={3}>
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
        <Grid item xs={12} sm={5} md={3}>
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
        <Grid item xs={12} md={2}>
          <SoftButton
            variant="outlined"
            color="dark"
            disabled={!list.length}
            onClick={() => exportParcels(list)}
          >
            Export CSV
          </SoftButton>
        </Grid>
      </Grid>
      <TableContainer sx={{ mt: 2 }}>
        <Table aria-label="Deliveries">
          <TableHead>
            <TableRow>
              {[
                "Tracking number",
                "Recipient / destination",
                "Status",
                "Fee",
                "Action",
              ].map((label) => (
                <TableCell key={label}>{label}</TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {list
              .slice(currentPage * rows, (currentPage + 1) * rows)
              .map((parcel) => (
                <TableRow key={parcel.id}>
                  <TableCell>
                    <Typography variant="body2" fontWeight={700}>
                      {parcel.trackingNumber}
                    </Typography>
                    <Typography variant="caption">
                      {time(parcel.createdAt)}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">
                      {parcel.recipientName}
                    </Typography>
                    <Typography variant="caption">
                      {parcel.dropoffAddress}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Status value={parcel.status} />
                  </TableCell>
                  <TableCell>{money(parcel.amount, parcel.currency)}</TableCell>
                  <TableCell>
                    <SoftButton
                      variant="text"
                      color="dark"
                      onClick={() => open(parcel)}
                      aria-label={`View ${parcel.trackingNumber}`}
                    >
                      View details
                    </SoftButton>
                  </TableCell>
                </TableRow>
              ))}
          </TableBody>
        </Table>
      </TableContainer>
      {!list.length && (
        <Empty
          text={
            w.errors.parcels
              ? "Delivery records could not be loaded. Use Refresh data to retry."
              : "No deliveries match. Clear the filters or create a booking."
          }
        />
      )}
      <TablePagination
        component="div"
        count={list.length}
        page={currentPage}
        rowsPerPage={rows}
        rowsPerPageOptions={[5, 10, 25]}
        onPageChange={(_, value) => setPage(value)}
        onRowsPerPageChange={(event) => {
          setRows(Number(event.target.value));
          setPage(0);
        }}
        sx={{ "& .MuiTablePagination-toolbar": { flexWrap: "wrap" } }}
      />
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
          Delivery {selected?.trackingNumber}
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

export function Dispatch({ workspace: w }: { workspace: Workspace }) {
  const [accounts, setAccounts] = useState<User[]>([]);
  const [accountId, setAccountId] = useState("");
  const [capacity, setCapacity] = useState(10);
  useEffect(() => {
    if (w.preview) return;
    let active = true;
    adminService
      .getAllUsers()
      .then((users) => {
        if (active)
          setAccounts(users.filter((u) => u.roles.includes("COURIER")));
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, [w.preview]);
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState("");
  const [courierId, setCourierId] = useState("");
  const [date, setDate] = useState(day());
  const unassigned = w.parcels.filter(
    (p) =>
      p.status === "CREATED" &&
      !p.courierId &&
      `${p.trackingNumber} ${p.dropoffAddress}`
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  const assign = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!selected || !courierId || date < day()) return;
    const ok = await w.act(async () => {
      if (!w.preview) return deliveryApi.assign(selected, courierId, date);
      updatePreview((draft) => {
        const parcel = draft.parcels.find((p) => p.id === selected);
        const courier = draft.couriers.find((c) => c.id === courierId);
        if (!parcel || parcel.courierId || parcel.status !== "CREATED")
          throw new Error("This parcel is no longer available for assignment.");
        const workload = draft.parcels.filter(
          (p) => p.courierId === courierId && !terminal(p.status),
        ).length;
        if (!courier?.available || workload >= courier.capacity)
          throw new Error(
            "This courier is unavailable or has reached capacity.",
          );
        parcel.courierId = courierId;
        parcel.assignedDate = date;
      });
    }, "Courier assigned. The delivery is now visible in the courier workspace.");
    if (ok) {
      setSelected("");
      setCourierId("");
    }
  };
  return (
    <Grid container spacing={3}>
      <Grid item xs={12} lg={8}>
        <Panel
          title="Unassigned parcels"
          subtitle="Choose a delivery and assign an available courier."
        >
          <Field
            label="Search unassigned parcels"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          {unassigned.map((parcel) => (
            <Box
              key={parcel.id}
              sx={{
                display: "flex",
                justifyContent: "space-between",
                gap: 2,
                py: 2,
                borderBottom: "1px solid #e2e8f0",
              }}
            >
              <Box>
                <Typography fontWeight={700}>
                  {parcel.trackingNumber}
                </Typography>
                <Typography variant="body2">
                  {parcel.pickupAddress} → {parcel.dropoffAddress}
                </Typography>
                <Typography variant="caption">
                  {parcel.weight} kg · {parcel.pickupDate}
                </Typography>
              </Box>
              <SoftButton
                variant={selected === parcel.id ? "gradient" : "outlined"}
                color={selected === parcel.id ? "primary" : "dark"}
                onClick={() => setSelected(parcel.id)}
              >
                Select
              </SoftButton>
            </Box>
          ))}
          {!unassigned.length && (
            <Empty text="No unassigned parcels match your search." />
          )}
          <Box component="form" onSubmit={assign} sx={{ mt: 3 }}>
            <Typography fontWeight={700}>
              Assign{" "}
              {w.parcels.find((p) => p.id === selected)?.trackingNumber ||
                "a selected parcel"}
            </Typography>
            <SelectField
              label="Available courier"
              value={courierId}
              onChange={setCourierId}
              options={[
                { value: "", label: "Choose a courier" },
                ...w.couriers
                  .filter(
                    (c) =>
                      c.available &&
                      w.parcels.filter(
                        (p) => p.courierId === c.id && !terminal(p.status),
                      ).length < c.capacity,
                  )
                  .map((c) => ({ value: c.id, label: c.name })),
              ]}
            />
            <Field
              type="date"
              label="Assignment date"
              value={date}
              onChange={(event) => setDate(event.target.value)}
              inputProps={{ min: day(), max: w.preview ? undefined : day() }}
              required
            />
            <SoftButton
              type="submit"
              variant="gradient"
              color="primary"
              disabled={w.busy || !selected || !courierId || date < day()}
              sx={{ mt: 2 }}
            >
              Confirm assignment
            </SoftButton>
          </Box>
        </Panel>
      </Grid>
      <Grid item xs={12} lg={4}>
        <Panel
          title="Courier workloads"
          subtitle="Active deliveries across all assignment dates."
        >
          {w.couriers.map((courier) => {
            const count = w.parcels.filter(
              (p) => p.courierId === courier.id && !terminal(p.status),
            ).length;
            return (
              <Box
                key={courier.id}
                sx={{ py: 2, borderBottom: "1px solid #e2e8f0" }}
              >
                <Typography fontWeight={700}>{courier.name}</Typography>
                <Typography variant="body2">
                  {courier.available ? "Available" : "Offline"} · {count} /{" "}
                  {courier.capacity} deliveries
                </Typography>
                <Box
                  role="progressbar"
                  aria-label={`${courier.name} workload`}
                  aria-valuenow={count}
                  aria-valuemin={0}
                  aria-valuemax={courier.capacity}
                  sx={{
                    mt: 1,
                    bgcolor: "#e2e8f0",
                    borderRadius: 2,
                    overflow: "hidden",
                  }}
                >
                  <Box
                    sx={{
                      width: `${Math.min(100, (count / courier.capacity) * 100)}%`,
                      height: 7,
                      bgcolor:
                        count >= courier.capacity ? "#ea0606" : "#ffcb05",
                    }}
                  />
                </Box>
              </Box>
            );
          })}
          {!w.couriers.length && <Empty text="No couriers are available." />}
          {!w.preview && (
            <Box
              component="form"
              sx={{ mt: 3 }}
              onSubmit={async (event) => {
                event.preventDefault();
                const user = accounts.find((u) => u.id === accountId);
                if (!user || !Number.isInteger(capacity) || capacity < 1)
                  return;
                if (
                  await w.act(
                    () =>
                      deliveryApi.registerCourier(
                        user.id,
                        user.fullName,
                        user.phoneNumber || "",
                        capacity,
                        user.email,
                      ),
                    "Courier profile registered.",
                  )
                )
                  setAccountId("");
              }}
            >
              <Typography fontWeight={700}>
                Register a courier profile
              </Typography>
              <SelectField
                label="Courier account"
                value={accountId}
                onChange={setAccountId}
                options={[
                  { value: "", label: "Choose a courier account" },
                  ...accounts
                    .filter((u) => !w.couriers.some((c) => c.id === u.id))
                    .map((u) => ({ value: u.id, label: u.fullName })),
                ]}
              />
              <Field
                label="Maximum active deliveries"
                type="number"
                value={capacity}
                onChange={(e) => setCapacity(Number(e.target.value))}
                inputProps={{ min: 1, max: 100 }}
              />
              <SoftButton
                type="submit"
                disabled={w.busy || !accountId || capacity < 1}
                variant="outlined"
                color="dark"
              >
                Register courier
              </SoftButton>
            </Box>
          )}
        </Panel>
      </Grid>
    </Grid>
  );
}
