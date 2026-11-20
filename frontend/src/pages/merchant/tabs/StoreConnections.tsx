import SoftTypography from "components/SoftTypography";
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
} from "@mui/material";
import { ActionButton as SoftButton } from "@/features/delivery/components";
import { z } from "zod";
import { time } from "@/features/delivery/model";
import { updatePreview } from "@/features/delivery/preview";
import { deliveryApi } from "@/features/delivery/service";
import type { Workspace } from "@/features/delivery/useDeliveryWorkspace";
import { Empty, Field, Panel } from "@/features/delivery/components";

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
        actions={<SoftButton
          variant="text"
          color="dark"
          onClick={() => void copy(snippet)}
        >
          Copy API example
        </SoftButton>}
        title="Create an order from your store"
        subtitle="Send each order with a unique store reference. API orders use the current delivery rate; repeat the same reference and payload to safely retry."
      >
        <Box
          component="pre"
          tabIndex={0}
          sx={{
            p: 2,
            bgcolor: "#00274c",
            color: "#f8fafc",
            borderRadius: 2,
            overflowX: "auto",
            fontSize: 12,
            whiteSpace: "pre-wrap",
            overflowWrap: "anywhere",
            lineHeight: 1.7,
            maxWidth: "100%",
          }}
        >
          {snippet}
        </Box>
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
          <SoftTypography component="span" variant="h5" fontWeight="bold">
            {provisioned ? "Rotate your API key?" : "Generate an API key?"}
          </SoftTypography>
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
