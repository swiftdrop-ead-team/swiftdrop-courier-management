import SoftTypography from "components/SoftTypography";
import { WorkspaceText as Typography } from "@/features/delivery/components";
import React, { useState } from "react";
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
import { z } from "zod";
import { money, type Zone } from "@/features/delivery/model";
import { updatePreview } from "@/features/delivery/preview";
import { deliveryApi } from "@/features/delivery/service";
import type { Workspace } from "@/features/delivery/useDeliveryWorkspace";
import { Empty, Field, Panel } from "@/features/delivery/components";

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
            <Box className="admin-rate-card">
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
          <SoftTypography component="span" variant="h5" fontWeight="bold">
            {isNew ? "Add delivery zone" : "Edit delivery zone"}
          </SoftTypography>
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
