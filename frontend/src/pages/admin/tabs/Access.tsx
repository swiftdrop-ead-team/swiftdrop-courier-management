import Table from "examples/Tables/Table";
import { AdminPagination as WorkspacePagination } from "../AdminPagination";
import { AdminBadge } from "../AdminShared";
import SoftTypography from "components/SoftTypography";
import { WorkspaceText as Typography } from "@/features/delivery/components";
import { useEffect, useState } from "react";
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
import { ActionButton as SoftButton } from "@/features/delivery/components";
import { downloadCsv } from "@/pages/admin/adminData";
import { createAdminPreview } from "@/pages/admin/adminData";
import type { User, UserRole, UserStatus } from "@/types/auth";
import { api } from "@/services/api";
import { useAuth } from "@/hooks/useAuth";
import { errorMessage } from "@/features/delivery/service";
import {
  Empty,
  Field,
  Panel,
  SelectField,
} from "@/features/delivery/components";

export function UserAdministration({ preview }: { preview: boolean }) {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<User[]>(() =>
    preview ? createAdminPreview().users : [],
  );
  const [loading, setLoading] = useState(!preview);
  const [revision, setRevision] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(5);
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
  const currentPage = Math.min(page, Math.max(0, Math.ceil(visible.length / pageSize) - 1));
  return (
    <Panel
      title="Account permissions"
      actions={
        <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1 }}>
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
      <SoftButton
        variant="outlined"
        color="dark"
        disabled={!visible.length}
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
        </Box>
      }
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
      <Grid container spacing={2} className="admin-filter-grid">
        <Grid item xs={12} md={6}>
          <Field
            label="Search accounts"
            placeholder="Name or email address"
            value={query}
            onChange={(event) => { setQuery(event.target.value); setPage(0); }}
          />
        </Grid>
        <Grid item xs={6} md={3}>
          <SelectField
            label="Filter role"
            value={role}
            onChange={(value) => { setRole(value); setPage(0); }}
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
            onChange={(value) => { setStatus(value); setPage(0); }}
            options={["ALL", "ACTIVE", "INACTIVE", "SUSPENDED"].map(
              (value) => ({
                value,
                label: value === "ALL" ? "All statuses" : value,
              }),
            )}
          />
        </Grid>
      </Grid>
      {visible.length > 0 && (
        <Table aria-label="Account permissions"
          columns={["Account", "Roles", "Status", "Merchant", "Action"].map((name) => ({ name, align: name === "Action" ? "right" : "left" }))}
          rows={visible.slice(currentPage * pageSize, (currentPage + 1) * pageSize).map((user) => ({
            id: user.id,
            Account: <Box component="span"><Typography component="span" variant="body2" fontWeight={700} display="block">{user.fullName}</Typography>
              <Typography component="span" variant="caption">{user.email}</Typography></Box>,
            Roles: user.roles.join(", "),
            Status: <AdminBadge status={user.status} />,
            Merchant: user.merchantApprovalStatus ? <AdminBadge status={user.merchantApprovalStatus} /> : "Not applicable",
            Action: <SoftButton variant="text" aria-label={`Edit ${user.fullName}`} onClick={() => {
              setDraft({ ...user, roles: [...user.roles] });
              setError("");
            }}>Edit access</SoftButton>,
          }))}
        />
      )}
      {!loading && !visible.length && <Empty />}
      <WorkspacePagination count={visible.length} page={currentPage} pageSize={pageSize} onPageChange={setPage} onPageSizeChange={setPageSize} />
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
          <SoftTypography component="span" variant="h5" fontWeight="bold">
            Edit access: {draft?.fullName}
          </SoftTypography>
        </DialogTitle>
        {draft && (
          <DialogContent>
            <Alert severity="warning" sx={{ mb: 2 }}>
              Confirm carefully. Role and status changes affect access to the
              platform.
            </Alert>
            <Box component="fieldset" className="admin-role-options">
              <legend>Account roles</legend>
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
            </Box>
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
