import React, { useEffect, useState, useSyncExternalStore } from "react";
import { Alert, Box, LinearProgress, Typography } from "@mui/material";
import { ThemeProvider } from "@mui/material/styles";
import theme from "assets/theme";
import { ActionButton as SoftButton } from "./components";
import { useParams, useSearchParams } from "react-router-dom";
import { z } from "zod";
import axios from "axios";
import { api } from "../../services/api";
import {
  publicProjection,
  statuses,
  terminal,
  time,
  type PublicTracking,
} from "./model";
import { getPreview, subscribePreview } from "./preview";
import { backendTime, parcelStatus } from "./service";
import { Field, Panel, Status, TrackingTimeline } from "./components";

const trackingSchema = z.object({
  trackingNumber: z.string(),
  status: z.enum(statuses),
  updatedAt: z.string().datetime(),
  history: z.array(
    z.object({ status: z.enum(statuses), timestamp: z.string().datetime() }),
  ),
});
const backendTrackingSchema = z.object({
  trackingNumber: z.string(),
  currentStatus: z.string(),
  lastUpdatedAt: z.string(),
  timeline: z.array(
    z.object({ newStatus: z.string(), occurredAt: z.string() }),
  ),
});
const parcelTrackingSchema = z.object({
  trackingNumber: z.string(),
  status: z.string(),
  updatedAt: z.string(),
  checkpoints: z.array(z.object({ status: z.string(), timestamp: z.string() })),
});
export function normalizeTracking(data: unknown): PublicTracking {
  const tracked = backendTrackingSchema.safeParse(data);
  if (tracked.success)
    return {
      trackingNumber: tracked.data.trackingNumber,
      status: parcelStatus(tracked.data.currentStatus),
      updatedAt: new Date(
        backendTime(tracked.data.lastUpdatedAt),
      ).toISOString(),
      history: tracked.data.timeline.map((e) => ({
        status: parcelStatus(e.newStatus),
        timestamp: new Date(backendTime(e.occurredAt)).toISOString(),
        note: "",
      })),
    };
  const parcel = parcelTrackingSchema.safeParse(data);
  if (parcel.success)
    return {
      trackingNumber: parcel.data.trackingNumber,
      status: parcelStatus(parcel.data.status),
      updatedAt: parcel.data.updatedAt,
      history: parcel.data.checkpoints.map((e) => ({
        status: parcelStatus(e.status),
        timestamp: e.timestamp,
        note: "",
      })),
    };
  const parsed = trackingSchema.parse(data);
  return {
    ...parsed,
    history: parsed.history.map((e) => ({ ...e, note: "" })),
  };
}
export function TrackingView({
  preview = false,
  initialNumber = "",
}: {
  preview?: boolean;
  initialNumber?: string;
}) {
  const [number, setNumber] = useState(initialNumber);
  const [submitted, setSubmitted] = useState(initialNumber);
  const [record, setRecord] = useState<PublicTracking | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [live, setLive] = useState(false);
  const [revision, setRevision] = useState(0);
  const snapshot = useSyncExternalStore(
    preview ? subscribePreview : () => () => {},
    preview ? getPreview : () => null,
  );
  useEffect(() => {
    setNumber(initialNumber);
    setSubmitted(initialNumber);
  }, [initialNumber]);
  useEffect(() => {
    setRecord(null);
    setError("");
    setLive(false);
    if (!submitted) return;
    if (!/^[A-Z0-9-]{5,50}$/i.test(submitted)) {
      setError(
        "Enter a valid tracking number (letters, numbers, and hyphens).",
      );
      return;
    }
    if (snapshot) {
      const parcel = snapshot.parcels.find(
        (p) => p.trackingNumber.toUpperCase() === submitted.toUpperCase(),
      );
      if (parcel) setRecord(publicProjection(parcel));
      else
        setError(
          "No parcel matches that preview tracking number. Try SD-DEMO-1001.",
        );
      return;
    }
    let active = true;
    let stopped = false;
    let source: EventSource | undefined;
    let inFlight = false;
    let latest = 0;
    const controller = new AbortController();
    const url = `${api.defaults.baseURL}/tracking/${encodeURIComponent(submitted)}`;
    const receive = (data: unknown) => {
      const parsed = normalizeTracking(data);
      const timestamp = new Date(parsed.updatedAt).getTime();
      if (!active || timestamp < latest) return;
      latest = timestamp;
      setRecord({
        ...parsed,
        history: parsed.history.map((entry) => ({ ...entry, note: "" })),
      });
      setError("");
      setLoading(false);
      if (terminal(parsed.status)) {
        stopped = true;
        source?.close();
        setLive(false);
      }
    };
    const load = async () => {
      if (inFlight || stopped) return;
      inFlight = true;
      try {
        let response;
        try {
          response = await axios.get(url, {
            signal: controller.signal,
            withCredentials: false,
            timeout: 15000,
          });
        } catch (error) {
          if (
            (error as { response?: { status?: number } }).response?.status !==
            404
          )
            throw error;
          response = await axios.get(
            `${api.defaults.baseURL}/parcels/tracking/${encodeURIComponent(submitted)}`,
            {
              signal: controller.signal,
              withCredentials: false,
              timeout: 15000,
            },
          );
        }
        receive(response.data);
      } catch (failure) {
        if (!active || controller.signal.aborted) return;
        const status = (failure as { response?: { status?: number } }).response
          ?.status;
        setError(
          status === 404
            ? "No parcel was found. Check your tracking number and try again."
            : "Tracking could not be refreshed. Please try again.",
        );
      } finally {
        inFlight = false;
        if (active) setLoading(false);
      }
    };
    setLoading(true);
    void load();
    if (typeof EventSource !== "undefined") {
      source = new EventSource(`${url}/stream`);
      source.onopen = () => {
        if (active) setLive(true);
      };
      source.onmessage = (event) => {
        try {
          receive(JSON.parse(event.data));
        } catch {
          /* Keep the last validated record; polling provides recovery. */
        }
      };
      source.addEventListener("snapshot", source.onmessage as EventListener);
      source.addEventListener("status", source.onmessage as EventListener);
      source.onerror = () => {
        if (active) setLive(false);
      };
    }
    const interval = window.setInterval(() => {
      if (!document.hidden) void load();
    }, 10000);
    return () => {
      active = false;
      controller.abort();
      source?.close();
      window.clearInterval(interval);
    };
  }, [submitted, snapshot, revision]);
  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitted(number.trim().toUpperCase());
    setRevision((value) => value + 1);
  };
  return (
    <Panel
      title="Track your parcel"
      subtitle="Use your tracking number to view delivery progress. Personal contact details stay private."
    >
      {preview && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Preview tracking · Uses shared sample deliveries. No live delivery is
          shown.
        </Alert>
      )}
      <Box
        component="form"
        onSubmit={submit}
        sx={{ display: "flex", gap: 2, flexWrap: "wrap", alignItems: "center" }}
      >
        <Box sx={{ flex: "1 1 240px" }}>
          <Field
            label="Tracking number"
            value={number}
            onChange={(event) => setNumber(event.target.value)}
            placeholder={
              preview ? "SD-DEMO-1001" : "Enter your tracking number"
            }
            inputProps={{ maxLength: 50 }}
            required
          />
        </Box>
        <SoftButton
          type="submit"
          variant="gradient"
          color="primary"
          disabled={loading}
        >
          Track parcel
        </SoftButton>
      </Box>
      {loading && (
        <Box role="status" aria-label="Loading tracking details" mt={2}>
          <LinearProgress />
        </Box>
      )}
      {error && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
      )}
      {record && (
        <Box aria-live="polite" mt={3}>
          <Typography variant="h5" fontWeight={700}>
            {record.trackingNumber}
          </Typography>
          <Box mt={2}>
            <Status value={record.status} />
          </Box>
          <Typography variant="caption" display="block" mt={2}>
            Last update: {time(record.updatedAt)} ·{" "}
            {preview
              ? "Preview changes appear automatically"
              : terminal(record.status)
                ? "Delivery closed"
                : live
                  ? "Live updates connected"
                  : "Checking for updates every 10 seconds"}
          </Typography>
          <TrackingTimeline record={record} />
        </Box>
      )}
    </Panel>
  );
}
export function PublicTrackingPage({ preview = false }: { preview?: boolean }) {
  const { trackingNumber } = useParams();
  const [params] = useSearchParams();
  return (
    <ThemeProvider theme={theme}>
      <Box
        component="main"
        sx={{ maxWidth: 820, mx: "auto", px: { xs: 2, md: 4 }, py: 6 }}
      >
        <Typography variant="h3" fontWeight={700} mb={3} color="#00274c">
          Every step, closer to you.
        </Typography>
        <TrackingView
          preview={preview}
          initialNumber={trackingNumber || params.get("number") || ""}
        />
      </Box>
    </ThemeProvider>
  );
}
