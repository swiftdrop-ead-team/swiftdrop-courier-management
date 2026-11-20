import { useEffect, useState, type FormEvent } from "react";
import {
  Alert,
  Autocomplete,
  Card,
  TextField,
  MenuItem,
  createFilterOptions,
  Box,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Stack,
} from "@mui/material";
import { Package, Truck, UserPlus } from "lucide-react";
import SoftBadge from "components/SoftBadge";
import SoftProgress from "components/SoftProgress";
import SoftTypography from "components/SoftTypography";
import { adminSelectStyle } from "@/pages/admin/AdminShared";
import { adminService } from "@/services/authService";
import type { User } from "@/types/auth";
import {
  ActionButton,
  Empty,
  Field,
  Panel,
  SelectField,
  WorkspaceText as Text,
} from "@/features/delivery/components";
import { day, terminal, type ParcelRecord } from "@/features/delivery/model";
import { updatePreview } from "@/features/delivery/preview";
import { deliveryApi, errorMessage } from "@/features/delivery/service";
import type { Workspace } from "@/features/delivery/useDeliveryWorkspace";

function previewWorkload(parcels: ParcelRecord[], courierId: string) {
  return parcels.filter((p) => p.courierId === courierId && !terminal(p.status))
    .length;
}

function RegisterCourier({
  workspace: w,
  onClose,
}: {
  workspace: Workspace;
  onClose: () => void;
}) {
  const [accounts, setAccounts] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [accountId, setAccountId] = useState("");
  const [phone, setPhone] = useState("");
  const [capacity, setCapacity] = useState("10");
  useEffect(() => {
    let active = true;
    adminService
      .getAllUsers()
      .then((users) => {
        if (active)
          setAccounts(
            users.filter(
              (u) => u.roles.includes("COURIER") && u.status === "ACTIVE",
            ),
          );
      })
      .catch((reason) => {
        if (active) setError(errorMessage(reason));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);
  const candidates = accounts.filter(
    (u) => !w.couriers.some((c) => c.id === u.id),
  );
  const account = candidates.find((u) => u.id === accountId);
  const max = Number(capacity);
  const ready =
    account &&
    phone.trim() &&
    Number.isInteger(max) &&
    max >= 1 &&
    max <= 100 &&
    !w.errors.couriers;
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!ready || !account) return;
    const ok = await w.act(
      () =>
        deliveryApi.registerCourier(
          account.id,
          account.fullName,
          phone.trim(),
          max,
          account.email,
        ),
      "Courier profile registered.",
    );
    if (ok) onClose();
  }
  return (
    <Dialog
      open
      onClose={w.busy ? undefined : onClose}
      fullWidth
      maxWidth="xs"
      aria-labelledby="register-courier-title"
    >
      <Box component="form" onSubmit={submit}>
        <DialogTitle id="register-courier-title">
          <SoftTypography component="span" variant="h5" fontWeight="bold">
            Register a courier profile
          </SoftTypography>
        </DialogTitle>
        <DialogContent>
          {error && <Alert severity="error">{error}</Alert>}
          {w.actionError && <Alert severity="error">{w.actionError}</Alert>}
          {w.errors.couriers && (
            <Alert severity="warning">
              Courier profiles could not be loaded. Refresh the workspace to try
              again.
            </Alert>
          )}
          <SelectField
            label="Courier account"
            value={account?.id || ""}
            disabled={!candidates.length || w.busy}
            options={[
              {
                value: "",
                label: loading
                  ? "Loading accounts…"
                  : "Choose a courier account",
              },
              ...candidates.map((u) => ({ value: u.id, label: u.fullName })),
            ]}
            onChange={(id) => {
              setAccountId(id);
              setPhone(candidates.find((u) => u.id === id)?.phoneNumber || "");
            }}
          />
          {!loading && !error && !candidates.length && (
            <Text variant="body2">
              No active courier accounts are waiting for registration.
            </Text>
          )}
          <Field
            label="Phone number"
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            required
          />
          <Field
            label="Maximum active deliveries"
            type="number"
            value={capacity}
            onChange={(e) => setCapacity(e.target.value)}
            inputProps={{ min: 1, max: 100 }}
            required
          />
        </DialogContent>
        <DialogActions sx={{ p: 2.5 }}>
          <ActionButton disabled={w.busy} onClick={onClose} variant="text">
            Cancel
          </ActionButton>
          <ActionButton
            type="submit"
            variant="gradient"
            color="primary"
            disabled={!ready || w.busy}
          >
            Register courier
          </ActionButton>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

const filterParcels = createFilterOptions<ParcelRecord>({
  stringify: (p) =>
    [p.trackingNumber, p.pickupAddress, p.dropoffAddress, p.recipientName].join(
      " ",
    ),
});

export function Dispatch({ workspace: w }: { workspace: Workspace }) {
  const [parcelInput, setParcelInput] = useState("");
  const [parcelId, setParcelId] = useState("");
  const [reassignParcel, setReassignParcel] = useState("");
  const [reassignCourier, setReassignCourier] = useState("");
  const [courierId, setCourierId] = useState("");
  const [scheduled, setScheduled] = useState(day);
  const [registerOpen, setRegisterOpen] = useState(false);
  const queue = w.parcels.filter((p) => p.status === "CREATED" && !p.courierId);
  const parcel = queue.find((p) => p.id === parcelId);
  const fleet = w.couriers.map((c) => {
    const load = w.preview
      ? previewWorkload(w.parcels, c.id)
      : (c.activeDeliveries ?? 0);
    const eligible = w.preview
      ? c.available && load < c.capacity
      : c.accountLinked && c.availableForAssignment === true;
    return { ...c, load, eligible };
  });
  const eligible = fleet.filter((c) => c.eligible);
  const courier = eligible.find((c) => c.id === courierId);
  const today = day();
  const date = w.preview ? scheduled : today;
  const ready =
    parcel &&
    parcelInput === parcel.trackingNumber &&
    courier &&
    date >= today &&
    !w.busy;

  async function assign(event: FormEvent) {
    event.preventDefault();
    if (!ready || !parcel || !courier) return;
    const ok = await w.act(async () => {
      if (!w.preview) return deliveryApi.assign(parcel.id, courier.id, date);
      updatePreview((draft) => {
        const target = draft.parcels.find((p) => p.id === parcel.id);
        const driver = draft.couriers.find((c) => c.id === courier.id);
        if (!target || target.courierId || target.status !== "CREATED")
          throw new Error("This parcel is no longer available for assignment.");
        if (
          !driver?.available ||
          previewWorkload(draft.parcels, driver.id) >= driver.capacity
        )
          throw new Error(
            "This courier is unavailable or has reached capacity.",
          );
        target.courierId = driver.id;
        target.assignedDate = date;
      });
    }, `${parcel.trackingNumber} assigned to ${courier.name}.`);
    if (ok) {
      setParcelId("");
      setParcelInput("");
      setCourierId("");
    }
  }
  return (
    <>
      {!w.preview && <Panel title="Reassign before pickup" subtitle="Transfer an existing assignment to another eligible courier. The previous courier loses access.">
        <Field select label="Assigned parcel" value={reassignParcel} onChange={event => { setReassignParcel(event.target.value); setReassignCourier(""); }}>
          <MenuItem value="">Choose a parcel</MenuItem>
          {w.parcels.filter(p => p.status === "CREATED" && p.assignmentId).map(p => <MenuItem key={p.id} value={p.id}>{p.trackingNumber}</MenuItem>)}
        </Field>
        <Field select label="New courier" value={reassignCourier} onChange={event => setReassignCourier(event.target.value)}>
          <MenuItem value="">Choose a courier</MenuItem>
          {eligible.filter(c => c.id !== w.parcels.find(p => p.id === reassignParcel)?.courierId).map(c => <MenuItem key={c.id} value={c.dispatchId}>{c.name}</MenuItem>)}
        </Field>
        <ActionButton disabled={w.busy || !reassignParcel || !reassignCourier} onClick={() => void w.act(async () => {
          const parcel = w.parcels.find(p => p.id === reassignParcel);
          if (!parcel?.assignmentId) throw new Error("Refresh the assignment list");
          await deliveryApi.reassign(parcel.assignmentId, reassignCourier);
          setReassignParcel(""); setReassignCourier("");
        }, "Courier reassigned.")}>Reassign courier</ActionButton>
      </Panel>}

      <Card className="admin-dispatch-layout" sx={{ width: "100%", maxWidth: "100%", overflow: "hidden" }}>
        <Box
          component="form"
          aria-labelledby="dispatch-form-title"
          onSubmit={assign}
          sx={{ p: { xs: 2.5, sm: 4 } }}
        >
          <Stack direction="row" spacing={1.5} alignItems="center" mb={3}>
            <Box
              aria-hidden="true"
              sx={{
                display: "grid",
                placeItems: "center",
                width: 44,
                height: 44,
                borderRadius: 2,
                bgcolor: "#fff4bf",
                color: "#604800",
              }}
            >
              <Package size={22} />
            </Box>
            <Box>
              <Text
                id="dispatch-form-title"
                component="h2"
                variant="h5"
                fontWeight={700}
              >
                Assign a parcel
              </Text>
              <Text variant="caption" color="text.secondary">
                {w.errors.parcels
                  ? "Queue unavailable"
                  : `${queue.length} awaiting assignment`}
                {" · "}
                {w.errors.couriers
                  ? "Fleet unavailable"
                  : `${eligible.length} ${eligible.length === 1 ? "courier" : "couriers"} available`}
              </Text>
            </Box>
          </Stack>
          <SoftTypography
            component="label"
            htmlFor="dispatch-parcel"
            variant="caption"
            fontWeight="bold"
            display="block"
            mb={0.75}
          >
            Unassigned parcel
          </SoftTypography>
          <Autocomplete
            id="dispatch-parcel"
            options={queue}
            value={parcel ?? null}
            inputValue={parcelInput}
            onInputChange={(_, value) => setParcelInput(value)}
            onChange={(_, value) => setParcelId(value?.id || "")}
            getOptionLabel={(p) => p.trackingNumber}
            isOptionEqualToValue={(a, b) => a.id === b.id}
            filterOptions={filterParcels}
            openOnFocus
            disabled={!queue.length || w.busy}
            noOptionsText="No parcels match your search."
            renderInput={(params) => (
              <TextField
                {...params}
                size="small"
                fullWidth
                placeholder="Search by tracking number, address or recipient"
                sx={{ "& .MuiInputBase-root": { minHeight: 44 } }}
              />
            )}
            renderOption={(props, p) => (
              <li {...props} key={p.id}>
                <Box minWidth={0}>
                  <Text variant="body2" fontWeight={700}>
                    {p.trackingNumber}
                  </Text>
                  <Text variant="caption" color="text.secondary">
                    {p.recipientName} · {p.dropoffAddress}
                  </Text>
                </Box>
              </li>
            )}
          />
          {!queue.length && (
            <Text variant="body2" mt={1} color="text.secondary">
              {w.errors.parcels
                ? "The parcel queue is unavailable."
                : "No parcels are waiting for assignment."}
            </Text>
          )}
          {parcel && (
            <Box
              component="dl"
              sx={{
                m: 0,
                mt: 1.5,
                p: 2,
                display: "grid",
                gap: 1.5,
                gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" },
                bgcolor: "#f5f7fa",
                borderRadius: 2,
              }}
            >
              <Box>
                <Text component="dt" variant="caption" fontWeight={700}>
                  Pickup
                </Text>
                <Text component="dd" variant="body2" m={0}>
                  {parcel.pickupAddress}
                </Text>
                <Text variant="caption" color="text.secondary">
                  {parcel.pickupDate} · {parcel.weight} kg
                </Text>
              </Box>
              <Box>
                <Text component="dt" variant="caption" fontWeight={700}>
                  Drop-off
                </Text>
                <Text component="dd" variant="body2" m={0}>
                  {parcel.dropoffAddress}
                </Text>
                <Text variant="caption" color="text.secondary">
                  {parcel.recipientName}
                </Text>
              </Box>
            </Box>
          )}
          <Box
            sx={{
              mt: 2.5,
              display: "grid",
              gridTemplateColumns: { xs: "1fr", sm: "3fr 2fr" },
              gap: 2,
              alignItems: "start",
              "& > div": { my: 0 },
            }}
          >
            <Box>
              <SoftTypography
                component="label"
                htmlFor="dispatch-courier"
                variant="caption"
                fontWeight="bold"
                display="block"
                mb={0.75}
              >
                Available courier
              </SoftTypography>
              <Box
                component="select"
                id="dispatch-courier"
                value={courier?.id || ""}
                required
                disabled={!eligible.length || w.busy}
                onChange={(e) => setCourierId(e.target.value)}
                sx={{
                  ...adminSelectStyle,
                  width: "100%",
                  minHeight: 44,
                  appearance: "auto",
                }}
              >
                <option value="">
                  {w.errors.couriers
                    ? "Courier list unavailable"
                    : eligible.length
                      ? "Choose a courier"
                      : "No available couriers"}
                </option>
                {eligible.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} · {c.capacity - c.load} slots open
                  </option>
                ))}
              </Box>
              {courier && (
                <Text
                  variant="caption"
                  display="block"
                  mt={0.75}
                  color="text.secondary"
                >
                  {courier.load} of {courier.capacity} deliveries assigned
                </Text>
              )}
            </Box>
            <Field
              label="Assignment date"
              type="date"
              value={date}
              onChange={(e) => setScheduled(e.target.value)}
              disabled={!w.preview}
              inputProps={{ min: today }}
              required
            />
          </Box>
          <Divider sx={{ my: 2.5 }} />
          <Box sx={{ display: "flex", justifyContent: "flex-end" }}>
            <ActionButton
              type="submit"
              variant="gradient"
              color="primary"
              disabled={!ready}
              sx={{ width: { xs: "100%", sm: "auto" } }}
            >
              Confirm assignment
            </ActionButton>
          </Box>
        </Box>
        <Box
          component="details"
          open
          sx={{
            borderTop: "1px solid #e2e8f0",
            px: { xs: 2.5, sm: 4 },
            bgcolor: "#fafbfd",
          }}
        >
          <Box
            component="summary"
            sx={{
              py: 1.75,
              cursor: "pointer",
              color: "#344767",
              fontSize: "0.875rem",
              fontWeight: 700,
            }}
          >
            <Truck
              aria-hidden="true"
              size={16}
              style={{
                display: "inline",
                verticalAlign: "middle",
                margin: "0 8px 2px 4px",
              }}
            />
            Courier workloads
            {!w.errors.couriers && (
              <Text variant="caption" color="text.secondary" ml={1}>
                {fleet.reduce((sum, c) => sum + c.load, 0)} active
              </Text>
            )}
          </Box>
          <Stack spacing={2} sx={{ pb: 2.5 }}>
            <Box
              sx={{
                display: "grid",
                gridTemplateColumns: {
                  xs: "1fr",
                  md: "repeat(2, minmax(0, 1fr))",
                },
                gap: 2,
              }}
            >
              {fleet.map((c) => (
                <Box
                  key={c.id}
                  sx={{ border: "1px solid #e2e8f0", borderRadius: 2, p: 2 }}
                >
                  <Stack
                    direction="row"
                    alignItems="center"
                    justifyContent="space-between"
                    spacing={1}
                  >
                    <Text fontWeight={700} variant="body2">
                      {c.name}
                    </Text>
                    <SoftBadge
                      container
                      size="xs"
                      color={c.eligible ? "success" : "secondary"}
                      badgeContent={
                        c.eligible
                          ? "Available"
                          : !c.available
                            ? "Offline"
                            : c.accountLinked === false
                              ? "Account required"
                              : c.load >= c.capacity
                                ? "At capacity"
                                : "Busy"
                      }
                    />
                  </Stack>
                  <Text variant="caption" display="block" my={1}>
                    {c.load} of {c.capacity} active deliveries
                  </Text>
                  <SoftProgress
                    value={
                      c.capacity
                        ? Math.min(100, (100 * c.load) / c.capacity)
                        : 0
                    }
                    variant="gradient"
                    color={c.eligible ? "info" : "secondary"}
                    aria-label={`${c.name} workload`}
                    aria-valuetext={`${c.load} of ${c.capacity} active deliveries`}
                  />
                </Box>
              ))}
            </Box>
            {!fleet.length && (
              <Empty
                text={
                  w.errors.couriers
                    ? "Courier workloads are unavailable."
                    : "No courier accounts have a dispatch profile yet."
                }
              />
            )}
            {!w.preview && (
              <>
                <Divider />
                <ActionButton
                  startIcon={<UserPlus size={16} />}
                  sx={{ alignSelf: "flex-start" }}
                  disabled={w.busy || Boolean(w.errors.couriers)}
                  onClick={() => {
                    w.setActionError("");
                    setRegisterOpen(true);
                  }}
                >
                  Register a courier profile
                </ActionButton>
              </>
            )}
          </Stack>
        </Box>
      </Card>
      {registerOpen && (
        <RegisterCourier workspace={w} onClose={() => setRegisterOpen(false)} />
      )}
    </>
  );
}
