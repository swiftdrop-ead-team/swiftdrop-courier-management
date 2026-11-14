import { WorkspaceText as Typography } from "./components";
import React, { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Grid,
  Checkbox,
} from "@mui/material";
import { ActionButton as SoftButton } from "./components";
import { z } from "zod";
import { downloadCsv } from "../../pages/admin/adminData";
import { createAdminPreview } from "../../pages/admin/adminData";
import type { User, UserRole, UserStatus } from "../../types/auth";
import { api } from "../../services/api";
import { useAuth } from "../../hooks/useAuth";
import { day, labels, money, statuses, type Zone } from "./model";
import { updatePreview } from "./preview";
import { deliveryApi, errorMessage } from "./service";
import type { Workspace } from "./useDeliveryWorkspace";
import { Empty, Field, Metrics, Panel, SelectField } from "./components";

const zoneSchema = z.object({
  id: z
    .string()
    .trim()
    .regex(
      /^[a-z0-9-]{2,40}$/,
      "Use 2–40 lowercase letters, digits, or hyphens",
    ),
  name: z.string().trim().min(2),
  base: z.coerce.number().nonnegative().max(100000),
  perKg: z.coerce.number().nonnegative().max(100000),
  expressMultiplier: z.coerce.number().min(1).max(10),
  active: z.boolean(),
});
export function Pricing({ workspace: w }: { workspace: Workspace }) {
  const [draft, setDraft] = useState<Zone | null>(null);
  const [isNew, setIsNew] = useState(false);
  const [error, setError] = useState("");
  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    const result = zoneSchema.safeParse(draft);
    if (!result.success) {
      setError(
        result.error.issues
          .map((issue) => `${issue.path.join(".")}: ${issue.message}`)
          .join(". "),
      );
      return;
    }
    if (isNew && w.zones.some((zone) => zone.id === result.data.id)) {
      setError("This zone ID already exists.");
      return;
    }
    const ok = await w.act(async () => {
      if (w.preview)
        updatePreview((state) => {
          const index = state.zones.findIndex(
            (zone) => zone.id === result.data.id,
          );
          if (index < 0) state.zones.push(result.data);
          else state.zones[index] = result.data;
        });
      else await deliveryApi.saveZone(result.data);
    }, "Delivery zone saved. New estimates use the updated rates.");
    if (ok) setDraft(null);
  };
  return (
    <Panel
      title="Delivery zones & pricing"
      subtitle="Rates apply to new estimates. Existing confirmed bookings retain their quoted fee."
    >
      <SoftButton
        variant="gradient"
        color="primary"
        onClick={() => {
          setDraft({
            id: "",
            name: "",
            base: 250,
            perKg: 80,
            expressMultiplier: 1.5,
            active: true,
          });
          setIsNew(true);
          setError("");
        }}
      >
        Add delivery zone
      </SoftButton>
      <Grid container spacing={2} sx={{ mt: 1 }}>
        {w.zones.map((zone) => (
          <Grid item xs={12} md={4} key={zone.id}>
            <Box sx={{ border: "1px solid #e2e8f0", borderRadius: 3, p: 3 }}>
              <Typography fontWeight={700}>
                {zone.name} · {zone.active ? "Active" : "Inactive"}
              </Typography>
              <Typography variant="body2" mt={1}>
                {money(zone.base)} base + {money(zone.perKg)} / kg
              </Typography>
              <Typography variant="caption">
                Express multiplier: {zone.expressMultiplier}×
              </Typography>
              <Box mt={2}>
                <SoftButton
                  variant="outlined"
                  color="dark"
                  onClick={() => {
                    setDraft({ ...zone });
                    setIsNew(false);
                    setError("");
                  }}
                >
                  Edit {zone.name}
                </SoftButton>
              </Box>
            </Box>
          </Grid>
        ))}
      </Grid>
      {!w.zones.length && <Empty text="No delivery zones have been loaded." />}
      <Dialog
        open={Boolean(draft)}
        onClose={() => {
          if (!w.busy) setDraft(null);
        }}
        fullWidth
        maxWidth="sm"
        aria-labelledby="zone-title"
      >
        <DialogTitle id="zone-title">
          {isNew ? "Add delivery zone" : "Edit delivery zone"}
        </DialogTitle>
        {draft && (
          <Box component="form" onSubmit={save}>
            <DialogContent>
              {(
                ["id", "name", "base", "perKg", "expressMultiplier"] as const
              ).map((key) => (
                <Field
                  key={key}
                  label={
                    {
                      id: "Zone ID",
                      name: "Zone name",
                      base: "Base fee (LKR)",
                      perKg: "Fee per kg (LKR)",
                      expressMultiplier: "Express multiplier",
                    }[key]
                  }
                  value={draft[key]}
                  type={["id", "name"].includes(key) ? "text" : "number"}
                  disabled={w.busy || (key === "id" && !isNew)}
                  onChange={(event) =>
                    setDraft({
                      ...draft,
                      [key]: ["id", "name"].includes(key)
                        ? event.target.value
                        : event.target.value === ""
                          ? ""
                          : Number(event.target.value),
                    })
                  }
                  inputProps={{
                    step: "any",
                    min: key === "expressMultiplier" ? 1 : 0,
                  }}
                  required
                />
              ))}
              <FormControlLabel
                label="Available for new bookings"
                control={
                  <Checkbox
                    checked={draft.active}
                    onChange={(_, active) => setDraft({ ...draft, active })}
                    disabled={w.busy}
                  />
                }
              />
              {error && <Alert severity="error">{error}</Alert>}
              {w.actionError && <Alert severity="error">{w.actionError}</Alert>}
            </DialogContent>
            <DialogActions sx={{ p: 3 }}>
              <SoftButton
                disabled={w.busy}
                onClick={() => setDraft(null)}
                variant="text"
                color="dark"
              >
                Cancel
              </SoftButton>
              <SoftButton
                type="submit"
                disabled={w.busy}
                variant="gradient"
                color="primary"
              >
                Save zone
              </SoftButton>
            </DialogActions>
          </Box>
        )}
      </Dialog>
      <Typography variant="caption" display="block" mt={3}>
        Chargeable weight is the higher of actual kg or length × width × height
        (cm) / 5000. The selected service multiplier is then applied.
      </Typography>
    </Panel>
  );
}
export function Reports({ workspace: w }: { workspace: Workspace }) {
  const [from, setFrom] = useState("");
  const [to, setTo] = useState(day());
  const parcels = w.parcels.filter(
    (p) =>
      (!from || day(p.createdAt) >= from) && (!to || day(p.createdAt) <= to),
  );
  const delivered = parcels.filter((p) => p.status === "DELIVERED").length;
  const failed = parcels.filter((p) => p.status === "FAILED").length;
  const completed = delivered + failed;
  const days = [...new Set(parcels.map((p) => day(p.createdAt)))].sort();
  return (
    <Box sx={{ display: "grid", gap: 3 }}>
      <Panel
        title="Delivery reports"
        subtitle="Reports group deliveries by booking date. Success/failure rates use completed attempts only."
      >
        <Grid container spacing={2}>
          <Grid item xs={12} sm={4}>
            <Field
              label="From booking date"
              type="date"
              value={from}
              onChange={(event) => setFrom(event.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <Field
              label="To booking date"
              type="date"
              value={to}
              onChange={(event) => setTo(event.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <SoftButton
              variant="outlined"
              color="dark"
              disabled={!parcels.length || Boolean(from && to && from > to)}
              sx={{ mt: 1 }}
              onClick={() =>
                downloadCsv("delivery-reports.csv", [
                  ["Booking date", "Deliveries", "Delivered", "Failed"],
                  ...days.map((date) => [
                    date,
                    parcels.filter((p) => day(p.createdAt) === date).length,
                    parcels.filter(
                      (p) =>
                        day(p.createdAt) === date && p.status === "DELIVERED",
                    ).length,
                    parcels.filter(
                      (p) => day(p.createdAt) === date && p.status === "FAILED",
                    ).length,
                  ]),
                ])
              }
            >
              Download report CSV
            </SoftButton>
          </Grid>
        </Grid>
        {from && to && from > to && (
          <Alert severity="error">
            The start date must be before the end date.
          </Alert>
        )}
      </Panel>
      <Metrics
        items={[
          { label: "Deliveries booked", value: parcels.length },
          { label: "Delivered", value: delivered },
          {
            label: "Success rate",
            value: completed
              ? `${((delivered / completed) * 100).toFixed(1)}%`
              : "No completed attempts",
          },
          {
            label: "Failure rate",
            value: completed
              ? `${((failed / completed) * 100).toFixed(1)}%`
              : "No completed attempts",
          },
        ]}
      />
      <Grid container spacing={3}>
        <Grid item xs={12} md={7}>
          <Panel title="Deliveries per day">
            {days.map((date) => {
              const count = parcels.filter(
                (p) => day(p.createdAt) === date,
              ).length;
              return (
                <Box key={date} mt={2}>
                  <Typography variant="body2">
                    {date} · {count} deliveries
                  </Typography>
                  <Box
                    sx={{
                      height: 16,
                      bgcolor: "#f1f5f9",
                      borderRadius: 2,
                      mt: 1,
                    }}
                  >
                    <Box
                      sx={{
                        height: "100%",
                        bgcolor: "#ffcb05",
                        width: `${(count / Math.max(...days.map((d) => parcels.filter((p) => day(p.createdAt) === d).length), 1)) * 100}%`,
                        borderRadius: 2,
                      }}
                    />
                  </Box>
                </Box>
              );
            })}
            {!days.length && <Empty />}
          </Panel>
        </Grid>
        <Grid item xs={12} md={5}>
          <Panel title="Delivery states">
            {statuses.map((status) => (
              <Box
                key={status}
                sx={{
                  display: "flex",
                  justifyContent: "space-between",
                  py: 1.5,
                  borderBottom: "1px solid #e2e8f0",
                }}
              >
                <Typography variant="body2">{labels[status]}</Typography>
                <Typography variant="body2" fontWeight={700}>
                  {parcels.filter((p) => p.status === status).length}
                </Typography>
              </Box>
            ))}
          </Panel>
        </Grid>
      </Grid>
    </Box>
  );
}
export function UserAdministration({ preview }: { preview: boolean }) {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<User[]>(() =>
    preview ? createAdminPreview().users : [],
  );
  const [loading, setLoading] = useState(!preview);
  const [revision, setRevision] = useState(0);
  const [query, setQuery] = useState("");
  const [role, setRole] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [draft, setDraft] = useState<User | null>(null);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    let active = true;
    if (!preview) {
      setLoading(true);
      setError("");
      api
        .get<User[]>("/admin/users")
        .then((response) => {
          if (active) setUsers(response.data);
        })
        .catch((failure) => {
          if (active) setError(errorMessage(failure));
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    }
    return () => {
      active = false;
    };
  }, [preview, revision]);
  const save = async () => {
    if (!draft || !draft.roles.length || busy) return;
    const removingAdmin =
      !draft.roles.includes("ADMIN") || draft.status !== "ACTIVE";
    const currentId = preview ? "demo-admin" : currentUser?.id;
    if (draft.id === currentId && removingAdmin) {
      setError(
        "You cannot remove your own administrator access or deactivate your own account.",
      );
      return;
    }
    if (
      removingAdmin &&
      users.find((user) => user.id === draft.id)?.roles.includes("ADMIN") &&
      !users.some(
        (user) =>
          user.id !== draft.id &&
          user.roles.includes("ADMIN") &&
          user.status === "ACTIVE",
      )
    ) {
      setError("Keep at least one active administrator.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      if (!preview)
        await api.patch(`/admin/users/${encodeURIComponent(draft.id)}`, {
          roles: draft.roles,
          status: draft.status,
        });
      setUsers((previous) =>
        previous.map((user) => (user.id === draft.id ? draft : user)),
      );
      setMessage("Account access updated.");
      setDraft(null);
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  };
  const visible = users.filter(
    (user) =>
      `${user.fullName} ${user.email}`
        .toLowerCase()
        .includes(query.toLowerCase()) &&
      (role === "ALL" || user.roles.includes(role as UserRole)) &&
      (status === "ALL" || user.status === status),
  );
  return (
    <Panel
      title="Manage user access"
      subtitle="Review account roles and status. Merchant registration decisions remain in Merchant Approvals."
    >
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      {message && (
        <Alert severity="success" sx={{ mb: 2 }}>
          {message}
        </Alert>
      )}
      {loading && <Typography role="status">Loading accounts…</Typography>}
      {!preview && (
        <SoftButton
          variant="outlined"
          color="dark"
          disabled={busy || loading}
          onClick={() => setRevision((value) => value + 1)}
        >
          Refresh accounts
        </SoftButton>
      )}
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Field
            label="Search accounts"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </Grid>
        <Grid item xs={6} md={3}>
          <SelectField
            label="Filter role"
            value={role}
            onChange={setRole}
            options={["ALL", "CUSTOMER", "MERCHANT", "COURIER", "ADMIN"].map(
              (value) => ({
                value,
                label: value === "ALL" ? "All roles" : value,
              }),
            )}
          />
        </Grid>
        <Grid item xs={6} md={3}>
          <SelectField
            label="Filter account status"
            value={status}
            onChange={setStatus}
            options={["ALL", "ACTIVE", "INACTIVE", "SUSPENDED"].map(
              (value) => ({
                value,
                label: value === "ALL" ? "All statuses" : value,
              }),
            )}
          />
        </Grid>
      </Grid>
      <SoftButton
        variant="outlined"
        color="dark"
        disabled={!visible.length}
        sx={{ my: 2 }}
        onClick={() =>
          downloadCsv("user-access.csv", [
            ["Name", "Email", "Roles", "Status"],
            ...visible.map((user) => [
              user.fullName,
              user.email,
              user.roles.join(", "),
              user.status,
            ]),
          ])
        }
      >
        Export accounts
      </SoftButton>
      {visible.map((user) => (
        <Box
          key={user.id}
          sx={{
            display: "flex",
            justifyContent: "space-between",
            gap: 2,
            flexWrap: "wrap",
            py: 2,
            borderBottom: "1px solid #e2e8f0",
          }}
        >
          <Box>
            <Typography fontWeight={700}>{user.fullName}</Typography>
            <Typography variant="body2">{user.email}</Typography>
            <Typography variant="caption">
              {user.roles.join(", ")} · {user.status}{" "}
              {user.merchantApprovalStatus &&
                `· Merchant ${user.merchantApprovalStatus}`}
            </Typography>
          </Box>
          <SoftButton
            variant="outlined"
            color="dark"
            onClick={() => {
              setDraft({ ...user, roles: [...user.roles] });
              setError("");
            }}
          >
            Edit {user.fullName}
          </SoftButton>
        </Box>
      ))}
      {!loading && !visible.length && <Empty />}
      <Dialog
        open={Boolean(draft)}
        onClose={() => {
          if (!busy) setDraft(null);
        }}
        fullWidth
        maxWidth="sm"
        aria-labelledby="access-title"
      >
        <DialogTitle id="access-title">
          Edit access: {draft?.fullName}
        </DialogTitle>
        {draft && (
          <DialogContent>
            <Alert severity="warning" sx={{ mb: 2 }}>
              Confirm carefully. Role and status changes affect access to the
              platform.
            </Alert>
            {(["CUSTOMER", "MERCHANT", "COURIER", "ADMIN"] as UserRole[]).map(
              (value) => (
                <FormControlLabel
                  key={value}
                  label={value}
                  control={
                    <Checkbox
                      checked={draft.roles.includes(value)}
                      disabled={busy}
                      onChange={(_, checked) =>
                        setDraft({
                          ...draft,
                          roles: checked
                            ? [...draft.roles, value]
                            : draft.roles.filter((role) => role !== value),
                        })
                      }
                    />
                  }
                />
              ),
            )}
            <SelectField
              label="Account status"
              value={draft.status}
              disabled={busy}
              onChange={(value) =>
                setDraft({ ...draft, status: value as UserStatus })
              }
              options={["ACTIVE", "INACTIVE", "SUSPENDED"].map((value) => ({
                value,
                label: value,
              }))}
            />
            {!draft.roles.length && (
              <Alert severity="error">Select at least one role.</Alert>
            )}
            {error && <Alert severity="error">{error}</Alert>}
          </DialogContent>
        )}
        <DialogActions sx={{ p: 3 }}>
          <SoftButton
            disabled={busy}
            variant="text"
            color="dark"
            onClick={() => setDraft(null)}
          >
            Cancel
          </SoftButton>
          <SoftButton
            variant="gradient"
            color="primary"
            disabled={busy || !draft?.roles.length}
            onClick={() => void save()}
          >
            Confirm access changes
          </SoftButton>
        </DialogActions>
      </Dialog>
    </Panel>
  );
}
