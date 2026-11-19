import { WorkspaceText as Typography } from "@/features/delivery/components";
import React, { useEffect, useState } from "react";
import { Alert, Box, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel } from "@mui/material";
import { ActionButton as SoftButton } from "@/features/delivery/components";
import { z } from "zod";
import {
  phoneSchema,
  type Profile,
  type SavedAddress,
} from "@/features/delivery/model";
import { updatePreview } from "@/features/delivery/preview";
import { deliveryApi } from "@/features/delivery/service";
import type { Workspace } from "@/features/delivery/useDeliveryWorkspace";
import { Field, Panel } from "@/features/delivery/components";

const profileSchema = z.object({
  fullName: z.string().trim().min(2),
  email: z.string().trim().email(),
  phone: z.union([z.literal(""), phoneSchema]),
  businessName: z.string().trim().max(150),
  address: z.string().trim().max(500),
  emailNotifications: z.boolean(),
});
export function Account({ workspace: w }: { workspace: Workspace }) {
  const [draft, setDraft] = useState<Profile>(w.profile);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState("");
  const [removing, setRemoving] = useState<SavedAddress | null>(null);
  const [addressError, setAddressError] = useState("");
  const [label, setLabel] = useState("");
  const [address, setAddress] = useState("");
  const profileKey = JSON.stringify(w.profile);
  useEffect(() => {
    if (!editing) setDraft(JSON.parse(profileKey));
  }, [profileKey, editing]);
  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    const result = profileSchema.safeParse(draft);
    if (!result.success) {
      setError(
        result.error.issues
          .map((issue) => `${issue.path[0]}: ${issue.message}`)
          .join(". "),
      );
      return;
    }
    if (w.role === "MERCHANT" && !draft.businessName.trim()) {
      setError("Enter your business name.");
      return;
    }
    const ok = await w.act(async () => {
      if (w.preview)
        updatePreview((state) => {
          state.profiles[w.ownerId] = result.data;
        });
      else {
        await deliveryApi.saveProfile(result.data);
        await w.refreshAccount();
      }
    }, "Profile saved.");
    if (ok) {
      setEditing(false);
      setError("");
    }
  };
  const saveAddresses = async (addresses: SavedAddress[]) =>
    w.act(async () => {
      if (w.preview)
        updatePreview((state) => {
          state.addresses[w.ownerId] = addresses;
        });
      else await deliveryApi.saveAddresses(addresses);
    }, "Saved addresses updated.");
  return (
    <Box sx={{ display: "grid", gap: 3 }}>
      <Panel
        title={
          w.role === "MERCHANT"
            ? "Business profile & preferences"
            : "Profile & preferences"
        }
        subtitle="Keep your contact details current and choose whether to receive delivery email updates."
      >
        <Box component="form" onSubmit={save} noValidate>
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: {
                xs: "1fr",
                md: "repeat(2, minmax(0, 1fr))",
              },
              columnGap: 3,
            }}
          >
            {([
              ["fullName", "Full name"],
              ["email", "Email address"],
              ["phone", "Phone number"],
              ...(w.role === "MERCHANT" ? [["businessName", "Business name"]] : []),
              ["address", "Pickup / business address"],
            ] as [Exclude<keyof Profile, "emailNotifications">, string][]).map(([key, label]) => (
              editing ? (
                <Field key={key} label={label} value={draft[key]}
                  type={key === "email" ? "email" : key === "phone" ? "tel" : "text"}
                  disabled={w.busy || Boolean(w.errors.profile) || key === "email" || key === "businessName"}
                  onChange={(event) => setDraft({ ...draft, [key]: event.target.value })}
                />
              ) : (
                <Box key={key} sx={{ py: 1.5, borderBottom: "1px solid #e4eaf0", minWidth: 0 }}>
                  <Typography variant="caption" color="text.secondary" display="block">{label}</Typography>
                  <Typography variant="body2" fontWeight={600}>{w.profile[key] || "Not provided"}</Typography>
                </Box>
              )
            ))}
          </Box>
          <FormControlLabel
            label="Email me delivery updates"
            control={
              <Checkbox
                checked={draft.emailNotifications}
                disabled={!editing || w.busy || Boolean(w.errors.profile)}
                onChange={(_, checked) =>
                  setDraft({ ...draft, emailNotifications: checked })
                }
              />
            }
          />
          {error && <Alert severity="error">{error}</Alert>}
          <Box sx={{ display: "flex", gap: 2, mt: 2 }}>
            {editing ? (
              <>
                <SoftButton
                  variant="text"
                  color="dark"
                  disabled={w.busy}
                  onClick={() => {
                    setEditing(false);
                    setError("");
                  }}
                >
                  Cancel
                </SoftButton>
                <SoftButton
                  variant="gradient"
                  color="primary"
                  type="submit"
                  disabled={w.busy}
                >
                  Save profile
                </SoftButton>
              </>
            ) : (
              <SoftButton
                variant="outlined"
                color="dark"
                onClick={() => setEditing(true)}
                disabled={w.busy || Boolean(w.errors.profile)}
              >
                Edit profile
              </SoftButton>
            )}
          </Box>
        </Box>
      </Panel>
      {(w.role === "CUSTOMER" || w.role === "MERCHANT") && (
        <Panel
          title="Saved pickup addresses"
          subtitle="Saved addresses are available when you book your next parcel."
        >
          {!w.addresses.length && !w.errors.addresses && <Typography variant="body2" color="text.secondary">No saved pickup addresses yet.</Typography>}
          {w.addresses.map((item) => (
            <Box
              key={item.id}
              sx={{
                py: 2,
                borderBottom: 1,
                borderColor: "brand.border",
                flexWrap: "wrap",
                display: "flex",
                justifyContent: "space-between",
                gap: 2,
              }}
            >
              <Box>
                <Typography fontWeight={700}>{item.label}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {item.address}
                </Typography>
              </Box>
              <SoftButton
                variant="text"
                color="error"
                disabled={w.busy || Boolean(w.errors.addresses)}
                onClick={() => setRemoving(item)}
              >
                Remove {item.label}
              </SoftButton>
            </Box>
          ))}
          <Box
            component="form"
            className="address-form"
            onSubmit={async (event) => {
              event.preventDefault();
              if (!label.trim() || address.trim().length < 5) {
                setAddressError("Enter an address label and a complete address.");
                return;
              }
              if (
                await saveAddresses([
                  ...w.addresses,
                  {
                    id: crypto.randomUUID(),
                    label: label.trim(),
                    address: address.trim(),
                  },
                ])
              ) {
                setLabel("");
                setAddress("");
                setAddressError("");
              }
            }}
          >
            {addressError && <Alert severity="error">{addressError}</Alert>}
            <Field
              label="Address label"
              placeholder="Home or office"
              value={label}
              onChange={(event) => setLabel(event.target.value)}
              required
              inputProps={{ maxLength: 60 }}
            />
            <Field
              label="Complete address"
              value={address}
              onChange={(event) => setAddress(event.target.value)}
              required
              inputProps={{ minLength: 5, maxLength: 500 }}
            />
            <SoftButton
              type="submit"
              variant="outlined"
              color="dark"
              disabled={
                w.busy ||
                Boolean(w.errors.addresses) ||
                w.addresses.length >= 20
              }
              sx={{ mt: 2 }}
            >
              Save address
            </SoftButton>
          </Box>
        </Panel>
      )}
      <Dialog open={Boolean(removing)} onClose={w.busy ? undefined : () => setRemoving(null)} fullWidth maxWidth="xs" aria-labelledby="remove-address-title">
        <DialogTitle id="remove-address-title">Remove saved address?</DialogTitle>
        <DialogContent>
          <Typography variant="subtitle2" fontWeight={700}>{removing?.label}</Typography>
          <Typography variant="body2">{removing?.address}</Typography>
          {w.actionError && <Alert severity="error">{w.actionError}</Alert>}
        </DialogContent>
        <DialogActions>
          <SoftButton autoFocus disabled={w.busy} onClick={() => setRemoving(null)}>Keep address</SoftButton>
          <SoftButton color="error" variant="contained" disabled={w.busy || Boolean(w.errors.addresses)} onClick={async () => {
            if (removing && await saveAddresses(w.addresses.filter((entry) => entry.id !== removing.id))) setRemoving(null);
          }}>{w.busy ? "Removing..." : "Remove address"}</SoftButton>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
