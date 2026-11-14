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
} from "@mui/material";
import { ActionButton as SoftButton } from "./components";
import { z } from "zod";
import { phoneSchema, time, type Profile, type SavedAddress } from "./model";
import { updatePreview } from "./preview";
import { deliveryApi } from "./service";
import type { Workspace } from "./useDeliveryWorkspace";
import { Empty, Field, Panel } from "./components";

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
          <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "repeat(2, minmax(0, 1fr))" }, columnGap: 3 }}>
          {(
            [
              "fullName",
              "email",
              "phone",
              ...(w.role === "MERCHANT" ? ["businessName"] : []),
              "address",
            ] as (keyof Profile)[]
          ).map((key) => (
            <Field
              key={key}
              label={
                {
                  fullName: "Full name",
                  email: "Email address",
                  phone: "Phone number",
                  businessName: "Business name",
                  address: "Pickup / business address",
                  emailNotifications: "",
                }[key]
              }
              value={draft[key]}
              type={
                key === "email" ? "email" : key === "phone" ? "tel" : "text"
              }
              disabled={!editing || w.busy || Boolean(w.errors.profile) || key === "email" || key === "businessName"}
              onChange={(event) =>
                setDraft({ ...draft, [key]: event.target.value })
              }
            />
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
          {w.addresses.map((item) => (
            <Box
              key={item.id}
              sx={{
                py: 2,
                display: "flex",
                justifyContent: "space-between",
                gap: 2,
              }}
            >
              <Box>
                <Typography fontWeight={700}>{item.label}</Typography>
                <Typography variant="body2">{item.address}</Typography>
              </Box>
              <SoftButton
                variant="text"
                color="error"
                disabled={w.busy || Boolean(w.errors.addresses)}
                onClick={() =>
                  void saveAddresses(
                    w.addresses.filter((entry) => entry.id !== item.id),
                  )
                }
              >
                Remove {item.label}
              </SoftButton>
            </Box>
          ))}
          <Box
            component="form"
            onSubmit={async (event) => {
              event.preventDefault();
              if (!label.trim() || address.trim().length < 5) {
                setError("Enter an address label and a complete address.");
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
                setError("");
              }
            }}
          >
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
              disabled={w.busy || Boolean(w.errors.addresses) || w.addresses.length >= 20}
              sx={{ mt: 2 }}
            >
              Save address
            </SoftButton>
          </Box>
        </Panel>
      )}
    </Box>
  );
}

export function Notifications({ workspace: w }: { workspace: Workspace }) {
  const [unread, setUnread] = useState(false);
  const notices = w.notices.filter((item) => !unread || !item.read);
  return (
    <Panel
      title="Delivery notifications"
      subtitle={
        w.preview
          ? "Sample delivery updates for this preview."
          : "Updates and notification history for your account."
      }
    >
      <FormControlLabel
        label="Show unread only"
        control={
          <Checkbox
            checked={unread}
            onChange={(_, checked) => setUnread(checked)}
          />
        }
      />
      {notices.map((item) => (
        <Box
          key={item.id}
          sx={{
            py: 2,
            borderBottom: "1px solid #e2e8f0",
            display: "flex",
            gap: 2,
            justifyContent: "space-between",
            flexWrap: "wrap",
          }}
        >
          <Box>
            <Typography variant="body2" fontWeight={item.read ? 400 : 700}>
              {item.text}
            </Typography>
            <Typography variant="caption">{time(item.timestamp)}</Typography>
          </Box>
          {!item.read && (
            <SoftButton
              variant="text"
              color="dark"
              disabled={w.busy}
              onClick={() =>
                void w.act(async () => {
                  if (w.preview)
                    updatePreview((state) => {
                      const notice = state.notices.find(
                        (entry) => entry.id === item.id,
                      );
                      if (notice) notice.read = true;
                    });
                  else await deliveryApi.readNotice(item.id);
                }, "Notification marked as read.")
              }
            >
              Mark read
            </SoftButton>
          )}
        </Box>
      ))}
      {!notices.length && (
        <Empty text="No notifications to show. New delivery updates will appear here." />
      )}
    </Panel>
  );
}

export function MerchantIntegration({
  workspace: w,
}: {
  workspace: Workspace;
}) {
  const [url, setUrl] = useState(w.integration?.webhookUrl || "");
  const [enabled, setEnabled] = useState(w.integration?.enabled || false);
  const [editing, setEditing] = useState(false);
  const [reveal, setReveal] = useState(false);
  const [newKey, setNewKey] = useState("");
  const [signingSecret, setSigningSecret] = useState("");
  const apiKey = newKey || w.integration?.apiKey || "";
  const provisioned = Boolean(apiKey || w.integration?.hasKey);
  const [rotate, setRotate] = useState(false);
  const [error, setError] = useState("");
  const integrationKey = JSON.stringify(w.integration);
  useEffect(() => {
    if (!editing) {
      setUrl(w.integration?.webhookUrl || "");
      setEnabled(w.integration?.enabled || false);
    }
  }, [integrationKey, editing]);
  const blocked = !w.preview && w.user?.merchantApprovalStatus !== "APPROVED";
  const copy = async (value: string) => {
    try {
      await navigator.clipboard.writeText(value);
      w.setMessage("Copied to clipboard.");
    } catch {
      setError(
        "Clipboard access is unavailable. Select and copy the text manually.",
      );
    }
  };
  const saveWebhook = async (event: React.FormEvent) => {
    event.preventDefault();
    setError("");
    if (enabled) {
      const parsed = z.string().url().safeParse(url);
      if (!parsed.success || new URL(url).protocol !== "https:") {
        setError("Enter an HTTPS webhook URL.");
        return;
      }
    }
    const ok = await w.act(async () => {
      if (w.preview)
        updatePreview((state) => {
          state.integrations[w.ownerId] = {
            apiKey: state.integrations[w.ownerId]?.apiKey || "",
            clientId: "preview-merchant-client",
            webhookUrl: url.trim(),
            enabled,
          };
        });
      else {
        const saved = await deliveryApi.webhook(url.trim(), enabled);
        setSigningSecret(saved.signingSecret || "");
      }
    }, "Webhook settings saved.");
    if (ok) setEditing(false);
  };
  const snippet = `curl -X POST "${window.location.origin}/api/v1/merchant/orders" \
  -H "X-API-Key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: UNIQUE_STORE_ORDER_REFERENCE" \
  --data '{"senderName":"Store","senderPhone":"0771234567","pickupAddress":"24 Store Road, Colombo","recipientName":"Customer","recipientPhone":"0772345678","deliveryAddress":"18 Main Street, Kandy","deliveryCity":"Kandy","description":"Clothing","weightKg":1,"lengthCm":20,"widthCm":15,"heightCm":10,"deliveryType":"STANDARD"}'`;

  return (
    <Box sx={{ display: "grid", gap: 3 }}>
      {blocked && (
        <Alert severity="warning">
          Merchant approval is required before using API credentials or
          webhooks.
        </Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}
      <Panel
        title="Merchant API credentials"
        subtitle="Keep API keys on your server. Never embed them in a public website or commit them to source control."
      >
        <Typography variant="body2">
          Client ID: {w.integration?.clientId || "Not provisioned"}
        </Typography>
        <Field
          label="API key"
          type={reveal ? "text" : "password"}
          value={apiKey}
          inputProps={{ readOnly: true }}
        />
        <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap", mt: 2 }}>
          <SoftButton
            variant="outlined"
            color="dark"
            disabled={!apiKey}
            onClick={() => setReveal((value) => !value)}
          >
            {reveal ? "Hide key" : "Reveal key"}
          </SoftButton>
          <SoftButton
            variant="outlined"
            color="dark"
            disabled={!apiKey}
            onClick={() => void copy(apiKey)}
          >
            Copy API key
          </SoftButton>
          <SoftButton
            variant="gradient"
            color="primary"
            disabled={blocked || w.busy}
            onClick={() => setRotate(true)}
          >
            {provisioned ? "Rotate API key" : "Generate API key"}
          </SoftButton>
        </Box>
        {w.preview && (
          <Typography variant="caption" display="block" mt={2}>
            Preview keys are sample values and cannot authenticate real API
            calls.
          </Typography>
        )}
      </Panel>
      {!w.preview && (
        <Alert severity="info">
          New API keys are shown only once. Copy a generated key before leaving
          this page.
        </Alert>
      )}
      <Panel
        title="Create an order from your store"
        subtitle="Send each order with a unique store reference. API orders use the current delivery rate; repeat the same reference and payload to safely retry."
      >
        <Box
          component="pre"
          tabIndex={0}
          sx={{
            p: 2,
            bgcolor: "#00274c",
            color: "white",
            borderRadius: 2,
            overflowX: "auto",
            fontSize: 12,
            whiteSpace: "pre",
            maxWidth: "100%",
          }}
        >
          {snippet}
        </Box>
        <SoftButton
          variant="text"
          color="dark"
          onClick={() => void copy(snippet)}
        >
          Copy API example
        </SoftButton>
      </Panel>
      {signingSecret && (
        <Alert severity="success">
          Copy this signing secret now. It is shown only once; saving the
          webhook again rotates it.
          <Field
            label="Webhook signing secret"
            value={signingSecret}
            inputProps={{ readOnly: true }}
          />
          <SoftButton onClick={() => void copy(signingSecret)}>
            Copy signing secret
          </SoftButton>
        </Alert>
      )}
      <Panel
        title="Status webhook"
        subtitle="Receive parcel.status.changed events at your HTTPS endpoint. Verify signatures on your server and return a 2xx response."
      >
        <Box component="form" onSubmit={saveWebhook}>
          <Field
            label="Webhook endpoint URL"
            type="url"
            value={url}
            disabled={blocked || w.busy}
            placeholder="https://store.example.com/webhooks/swiftdrop"
            onChange={(event) => {
              setEditing(true);
              setUrl(event.target.value);
            }}
          />
          <FormControlLabel
            label="Enable status webhooks"
            control={
              <Checkbox
                checked={enabled}
                disabled={blocked || w.busy}
                onChange={(_, checked) => {
                  setEditing(true);
                  setEnabled(checked);
                }}
              />
            }
          />
          <Box>
            <SoftButton
              type="submit"
              variant="gradient"
              color="primary"
              disabled={blocked || w.busy}
            >
              Save webhook settings
            </SoftButton>
          </Box>
        </Box>
        <Typography variant="caption" display="block" mt={2}>
          Failed attempts can be retried below. Retry scheduling and signature
          generation are handled by the merchant backend.
        </Typography>
      </Panel>
      <Panel
        title="Webhook delivery attempts"
        subtitle={
          w.preview
            ? "Simulated delivery log. Preview never contacts your webhook endpoint."
            : "Review delivery outcomes and request a retry."
        }
      >
        {w.attempts.map((attempt) => (
          <Box
            key={attempt.id}
            sx={{
              display: "flex",
              justifyContent: "space-between",
              gap: 2,
              flexWrap: "wrap",
              borderBottom: "1px solid #e2e8f0",
              py: 2,
            }}
          >
            <Box>
              <Typography fontWeight={700} variant="body2">
                {attempt.trackingNumber} · {attempt.status}
              </Typography>
              <Typography variant="caption">
                {time(attempt.timestamp)} · {attempt.result} ·{" "}
                {attempt.attempts} attempts
              </Typography>
            </Box>
            {attempt.result !== "DELIVERED" && (
              <SoftButton
                variant="outlined"
                color="dark"
                disabled={w.busy || blocked}
                onClick={() =>
                  void w.act(
                    async () => {
                      if (w.preview)
                        updatePreview((state) => {
                          const entry = state.webhooks[w.ownerId]?.find(
                            (item) => item.id === attempt.id,
                          );
                          if (entry) {
                            entry.attempts += 1;
                            entry.result = "DELIVERED";
                          }
                        });
                      else await deliveryApi.retry(attempt.id);
                    },
                    w.preview
                      ? "Preview: webhook delivery simulated."
                      : "Webhook retry requested.",
                  )
                }
              >
                {w.preview ? "Simulate retry" : "Retry delivery"}
              </SoftButton>
            )}
          </Box>
        ))}
        {!w.attempts.length && (
          <Empty text="No webhook delivery attempts yet. Enable a webhook, then update a merchant parcel status." />
        )}
      </Panel>
      <Dialog
        open={rotate}
        onClose={() => {
          if (!w.busy) setRotate(false);
        }}
        aria-labelledby="rotate-key-title"
      >
        <DialogTitle id="rotate-key-title">
          {provisioned ? "Rotate your API key?" : "Generate an API key?"}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            Generating a new key revokes the previous key. Update your store
            integration after saving the new key.
          </Typography>
          {w.actionError && <Alert severity="error">{w.actionError}</Alert>}
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <SoftButton
            variant="text"
            color="dark"
            disabled={w.busy}
            onClick={() => setRotate(false)}
          >
            Cancel
          </SoftButton>
          <SoftButton
            variant="gradient"
            color="primary"
            disabled={w.busy}
            onClick={() =>
              void (async () => {
                if (
                  await w.act(async () => {
                    if (w.preview)
                      updatePreview((state) => {
                        state.integrations[w.ownerId] = {
                          apiKey: `preview_sk_${crypto.randomUUID()}`,
                          clientId: "preview-merchant-client",
                          webhookUrl:
                            state.integrations[w.ownerId]?.webhookUrl || "",
                          enabled:
                            state.integrations[w.ownerId]?.enabled || false,
                        };
                      });
                    else {
                      const generated = await deliveryApi.rotateKey();
                      if (!generated.apiKey)
                        throw new Error(
                          "The new API key was not returned. Please contact support.",
                        );
                      setNewKey(generated.apiKey);
                    }
                  }, "New API key generated.")
                ) {
                  setRotate(false);
                  setReveal(true);
                }
              })()
            }
          >
            Confirm key generation
          </SoftButton>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
