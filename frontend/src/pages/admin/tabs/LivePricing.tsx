import SoftTypography from "components/SoftTypography";
import SoftBadge from "components/SoftBadge";
import { WorkspaceText as Typography } from "@/features/delivery/components";
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
  LinearProgress,
} from "@mui/material";
import { api } from "@/services/api";
import {
  ActionButton,
  Empty,
  Field,
  Panel,
} from "@/features/delivery/components";
import { errorMessage } from "@/features/delivery/service";
import { money } from "@/features/delivery/model";
type Rate = {
  serviceType: string;
  baseFare: number;
  perKgRate: number;
  perKmRate: number;
  baseWeightKg: number;
  volumetricDivisor: number;
  urgencyMultiplier: number;
  isActive: boolean;
};
export function LivePricing() {
  const [rates, setRates] = useState<Rate[]>([]),
    [draft, setDraft] = useState<Rate | null>(null);
  const [loading, setLoading] = useState(true),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [message, setMessage] = useState("");
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    const c = new AbortController();
    setLoading(true);
    setError("");
    api
      .get<Rate[]>("/pricing/rules", { signal: c.signal })
      .then((r) => setRates(r.data))
      .catch((e) => {
        if (!c.signal.aborted) {
          setRates([]);
          setError(errorMessage(e));
        }
      })
      .finally(() => {
        if (!c.signal.aborted) setLoading(false);
      });
    return () => c.abort();
  }, [revision]);
  const fields = {
    baseFare: "Base fare (LKR)",
    baseWeightKg: "Included weight (kg)",
    perKgRate: "Extra weight rate (LKR/kg)",
    urgencyMultiplier: "Service multiplier",
    volumetricDivisor: "Volumetric divisor",
  } as const;
  async function save(event: React.FormEvent) {
    event.preventDefault();
    if (!draft || busy) return;
    const invalidField = (Object.keys(fields) as (keyof typeof fields)[]).find(key => {
      const minimum = key === "baseWeightKg" ? 0.01 : key === "volumetricDivisor" || key === "urgencyMultiplier" ? 1 : 0;
      return !Number.isFinite(draft[key]) || draft[key] < minimum || (key === "volumetricDivisor" && !Number.isInteger(draft[key]));
    });
    if (invalidField) {
      setError(`Enter a valid value for ${fields[invalidField]}.`);
      return;
    }
    setBusy(true);
    setError("");
    try {
      await api.put(`/pricing/rules/${draft.serviceType}`, {
        ...draft,
        active: draft.isActive,
      });
      setDraft(null);
      setMessage("Rates saved. Confirmed bookings keep their accepted fee.");
      setRevision((r) => r + 1);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <Panel
      actions={
        <ActionButton
        disabled={busy || loading}
        onClick={() => setRevision((r) => r + 1)}
      >
        Refresh rates
      </ActionButton>
      }
      title="Delivery service rates"
      subtitle="Manage rates in LKR for new bookings. The included weight is covered by the base fare; extra billable weight uses the per-kg rate."
    >
      {loading && <LinearProgress aria-label="Loading rates" />}
      {error && (
        <Alert severity="error" sx={{ my: 2 }}>
          {error}
        </Alert>
      )}
      {message && (
        <Alert severity="success" sx={{ my: 2 }}>
          {message}
        </Alert>
      )}
      <Grid container spacing={2} sx={{ mt: 1 }}>
        {rates.map((rate) => (
          <Grid item xs={12} md={4} key={rate.serviceType}>
            <Box className="admin-rate-card">
              <Box className="admin-rate-heading">
                <Typography variant="h6">{rate.serviceType.replace(/_/g, " ")}</Typography>
                <SoftBadge container size="sm" color={rate.isActive ? "success" : "secondary"}
                  badgeContent={rate.isActive ? "Available" : "Unavailable"} />
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Base fare</Typography>
                <Typography variant="h4" fontWeight={700}>{money(Number(rate.baseFare), "LKR")}</Typography>
              </Box>
              <Box component="dl">
                <dt>Included weight</dt><dd>{rate.baseWeightKg} kg</dd>
                <dt>Extra weight / kg</dt><dd>{money(Number(rate.perKgRate), "LKR")}</dd>
                <dt>Service multiplier</dt><dd>{rate.urgencyMultiplier}?</dd>
              </Box>
              <ActionButton
                onClick={() => {
                  setError("");
                  setDraft({ ...rate });
                }}
              >
                Edit {rate.serviceType.toLowerCase()} rates
              </ActionButton>
            </Box>
          </Grid>
        ))}
      </Grid>
      {!loading && !rates.length && (
        <Empty text="No service rates are available. Refresh to try again." />
      )}
      <Dialog
        open={Boolean(draft)}
        onClose={() => {
          if (!busy) setDraft(null);
        }}
        fullWidth
        maxWidth="sm"
        aria-labelledby="rate-title"
      >
        <DialogTitle id="rate-title">
          <SoftTypography component="span" variant="h5" fontWeight="bold">
            Edit {draft?.serviceType.toLowerCase()} rates
          </SoftTypography>
        </DialogTitle>
        {draft && (
          <Box component="form" onSubmit={save}>
            <DialogContent>
              {error && <Alert severity="error">{error}</Alert>}
              <Box className="admin-rate-fields">
              {Object.entries(fields).map(([key, label]) => (
                <Field
                  key={key}
                  label={label}
                  type="number"
                  required
                  value={draft[key as keyof typeof fields]}
                  disabled={busy}
                  inputProps={{
                    min:
                      key === "baseWeightKg"
                        ? 0.01
                        : key === "volumetricDivisor" ||
                            key === "urgencyMultiplier"
                          ? 1
                          : 0,
                    step: key === "volumetricDivisor" ? 1 : 0.01,
                  }}
                  onChange={(e) =>
                    setDraft({ ...draft, [key]: Number(e.target.value) })
                  }
                />
              ))}
              </Box>
              <FormControlLabel
                label="Available for booking"
                control={
                  <Checkbox
                    checked={draft.isActive}
                    disabled={busy}
                    onChange={(_, checked) =>
                      setDraft({ ...draft, isActive: checked })
                    }
                  />
                }
              />
            </DialogContent>
            <DialogActions sx={{ p: 3 }}>
              <ActionButton disabled={busy} onClick={() => setDraft(null)}>
                Cancel
              </ActionButton>
              <ActionButton
                type="submit"
                color="primary"
                variant="gradient"
                disabled={busy}
              >
                {busy ? "Saving..." : "Save rates"}
              </ActionButton>
            </DialogActions>
          </Box>
        )}
      </Dialog>
    </Panel>
  );
}
