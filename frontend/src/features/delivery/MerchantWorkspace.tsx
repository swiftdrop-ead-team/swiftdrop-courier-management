import React, { useState } from "react";
import {
  Alert,
  Box,
  LinearProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from "@mui/material";
import { Link } from "react-router-dom";
import {
  ActionButton,
  Empty,
  Field,
  Metrics,
  Panel,
  Status,
  WorkspaceText as Typography,
  exportParcels,
} from "./components";
import { day, labels, money, statuses, terminal } from "./model";
import type { Workspace } from "./useDeliveryWorkspace";

export function MerchantOverview({
  workspace: w,
  base,
}: {
  workspace: Workspace;
  base: string;
}) {
  const pending = w.parcels.filter((p) => p.status === "CREATED");
  const failed = w.parcels.filter((p) => p.status === "FAILED");
  const active = w.parcels.filter((p) => !terminal(p.status));
  const unavailable = w.loading || Boolean(w.errors.parcels);
  return (
    <Box sx={{ display: "grid", gap: 3 }}>
      <Box
        sx={{
          p: { xs: 3, md: 4 },
          background: "linear-gradient(135deg, #00274c 0%, #001a35 100%)",
          color: "white",
          borderRadius: "14px",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          flexWrap: "wrap",
          gap: 3,
          position: "relative",
          overflow: "hidden",
          boxShadow: "0 4px 16px rgba(0, 39, 76, 0.2)",
          "&::before": {
            content: '""',
            position: "absolute",
            top: -40,
            right: -40,
            width: 200,
            height: 200,
            borderRadius: "50%",
            background: "rgba(255, 203, 5, 0.08)",
          },
        }}
      >
        <Box sx={{ position: "relative", zIndex: 1 }}>
          <Typography
            variant="overline"
            sx={{
              color: "rgba(255, 203, 5, 0.9) !important",
              fontWeight: 700,
              letterSpacing: "0.08em",
              fontSize: "0.7rem",
            }}
          >
            MERCHANT OPERATIONS
          </Typography>
          <Typography
            variant="h4"
            fontWeight={700}
            sx={{
              color: "#ffffff !important",
              fontSize: "1.5rem",
              letterSpacing: "-0.02em",
              lineHeight: 1.2,
              mt: 0.5,
            }}
          >
            Ready for your next dispatch.
          </Typography>
          <Typography
            variant="body2"
            sx={{
              color: "rgba(255, 255, 255, 0.7) !important",
              mt: 1,
              fontSize: "0.88rem",
              lineHeight: 1.5,
            }}
          >
            Book pickups, follow deliveries, and keep your customers informed.
          </Typography>
        </Box>
        <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap", position: "relative", zIndex: 1 }}>
          <ActionButton
            component={Link}
            to={`${base}/create-shipment`}
            variant="contained"
            color="primary"
            sx={{
              borderRadius: "8px",
              fontWeight: 700,
              px: 2.5,
              py: 1,
              fontSize: "0.85rem",
              color: "#00274c !important",
              background: "linear-gradient(135deg, #ffcb05 0%, #ffdb57 100%) !important",
              boxShadow: "0 2px 8px rgba(255, 203, 5, 0.3)",
              "&:hover": {
                background: "linear-gradient(135deg, #ffdb57 0%, #ffe866 100%) !important",
                boxShadow: "0 4px 12px rgba(255, 203, 5, 0.4)",
              },
            }}
          >
            Create shipment
          </ActionButton>
          <ActionButton
            component={Link}
            to={`${base}/bulk-upload`}
            sx={{
              borderRadius: "8px",
              fontWeight: 600,
              px: 2.5,
              py: 1,
              fontSize: "0.85rem",
              backgroundColor: "rgba(255, 255, 255, 0.12) !important",
              color: "#ffffff !important",
              borderColor: "rgba(255, 255, 255, 0.25) !important",
              "&:hover": {
                backgroundColor: "rgba(255, 255, 255, 0.2) !important",
                borderColor: "rgba(255, 255, 255, 0.4) !important",
              },
            }}
          >
            Import CSV
          </ActionButton>
        </Box>
      </Box>
      <Metrics
        items={[
          {
            label: "Awaiting pickup",
            value: unavailable ? "Unavailable" : pending.length,
          },
          {
            label: "In transit",
            value: unavailable ? "Unavailable" : active.length - pending.length,
          },
          {
            label: "Delivered",
            value: unavailable
              ? "Unavailable"
              : w.parcels.filter((p) => p.status === "DELIVERED").length,
          },
          {
            label: "Failed deliveries",
            value: unavailable ? "Unavailable" : failed.length,
          },
        ]}
      />
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: { xs: "1fr", md: "2fr 1fr" },
          gap: 3,
        }}
      >
        <Panel
          title="Pickup queue"
          subtitle="Open bookings, ordered by scheduled pickup date."
        >
          {!unavailable && pending.length === 0 && (
            <Empty text="No pickups waiting. Create a shipment or import your orders to get started." />
          )}
          {unavailable ? (
            <Empty text="Pickup data is unavailable until shipment records load." />
          ) : (
            [...pending]
              .sort((a, b) => a.pickupDate.localeCompare(b.pickupDate))
              .slice(0, 5)
              .map((p) => (
                <Box
                  key={p.id}
                  sx={{
                    py: 2,
                    borderBottom: "1px solid #e2e8f0",
                    display: "flex",
                    justifyContent: "space-between",
                    gap: 2,
                    flexWrap: "wrap",
                  }}
                >
                  <Box>
                    <Typography fontWeight={700}>{p.trackingNumber}</Typography>
                    <Typography variant="body2">
                      {p.recipientName} · {p.pickupDate}
                    </Typography>
                    <Typography variant="caption">{p.pickupAddress}</Typography>
                  </Box>
                  <ActionButton
                    component={Link}
                    to={`${base}/shipments?q=${encodeURIComponent(p.trackingNumber)}`}
                  >
                    View shipment
                  </ActionButton>
                </Box>
              ))
          )}
          <ActionButton
            component={Link}
            to={`${base}/shipments?status=CREATED`}
            sx={{ mt: 2 }}
          >
            View all pickups
          </ActionButton>
        </Panel>
        <Panel
          title="Needs attention"
          subtitle="Review exceptions before your next dispatch."
        >
          <Typography variant="body2">
            {unavailable
              ? "Shipment exceptions could not be calculated."
              : `${failed.length} failed deliveries need review.`}
          </Typography>
          <ActionButton
            component={Link}
            to={`${base}/shipments?status=FAILED`}
            sx={{ mt: 2, mb: 3 }}
          >
            Review failed deliveries
          </ActionButton>
          <Typography variant="body2">
            Keep pickup locations and contact details current for your courier.
          </Typography>
          <ActionButton component={Link} to={`${base}/profile`} sx={{ mt: 2 }}>
            Business & pickup locations
          </ActionButton>
        </Panel>
      </Box>
      <Panel
        title="Recent shipments"
        subtitle="Latest bookings across all delivery stages."
      >
        {unavailable ? (
          <Empty text="Shipment records could not be loaded yet." />
        ) : w.parcels.length ? (
          [...w.parcels]
            .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
            .slice(0, 5)
            .map((p) => (
              <Box
                key={p.id}
                sx={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  flexWrap: "wrap",
                  gap: 2,
                  py: 2,
                  borderBottom: "1px solid #e2e8f0",
                }}
              >
                <Box>
                  <ActionButton
                    component={Link}
                    variant="text"
                    to={`${base}/shipments?q=${encodeURIComponent(p.trackingNumber)}`}
                  >
                    {p.trackingNumber}
                  </ActionButton>
                  <Typography variant="body2">{p.recipientName}</Typography>
                </Box>
                <Status value={p.status} />
              </Box>
            ))
        ) : (
          <Empty text="Your shipments will appear here after your first booking." />
        )}
      </Panel>
    </Box>
  );
}

export function MerchantReports({ workspace: w }: { workspace: Workspace }) {
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const invalid = Boolean(from && to && from > to);
  const parcels = w.parcels.filter(
    (p) =>
      (!from || day(p.createdAt) >= from) && (!to || day(p.createdAt) <= to),
  );
  const delivered = parcels.filter((p) => p.status === "DELIVERED");
  const unavailable = invalid || w.loading || Boolean(w.errors.parcels);
  const total = (items: typeof parcels, key: "amount" | "cod") =>
    money(items.reduce((sum, p) => sum + p[key], 0));
  return (
    <Box sx={{ display: "grid", gap: 3 }}>
      <Panel
        title="Shipment performance & COD"
        subtitle="Filter by booking date. Export the matching shipments for reconciliation."
      >
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            flexWrap: "wrap",
            gap: 2,
          }}
        >
          <Box>
            <Field
              label="From booking date"
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
            />
          </Box>
          <Box>
            <Field
              label="To booking date"
              type="date"
              value={to}
              onChange={(e) => setTo(e.target.value)}
            />
          </Box>
          <ActionButton
            disabled={unavailable || !parcels.length}
            onClick={() => exportParcels(parcels)}
          >
            Export report CSV
          </ActionButton>
          <ActionButton
            onClick={() => {
              setFrom("");
              setTo("");
            }}
          >
            Clear dates
          </ActionButton>
        </Box>
        {invalid && (
          <Alert severity="error">
            The end date must be on or after the start date.
          </Alert>
        )}
      </Panel>
      <Metrics
        items={[
          {
            label: "Shipments",
            value: unavailable ? "Unavailable" : parcels.length,
          },
          {
            label: "Delivery fees (excluding cancelled)",
            value: unavailable
              ? "Unavailable"
              : total(
                  parcels.filter((p) => p.status !== "CANCELLED"),
                  "amount",
                ),
          },
          {
            label: "COD on delivered parcels",
            value: unavailable ? "Unavailable" : total(delivered, "cod"),
          },
          {
            label: "COD awaiting delivery",
            value: unavailable
              ? "Unavailable"
              : total(
                  parcels.filter((p) => !terminal(p.status)),
                  "cod",
                ),
          },
        ]}
      />
      <Alert severity="info">
        COD figures are based on parcel delivery records. They are not a payout
        statement or confirmation of funds transferred to your business.
      </Alert>
      <Panel title="Delivery outcomes">
        {!unavailable && !parcels.length ? (
          <Empty text="No shipments in this booking period." />
        ) : unavailable ? (
          <Empty text="Load valid shipment data to view your report." />
        ) : (
          <TableContainer>
            <Table aria-label="Merchant delivery outcomes">
              <TableHead>
                <TableRow>
                  <TableCell>Status</TableCell>
                  <TableCell>Shipments</TableCell>
                  <TableCell>Share of bookings</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {statuses.map((status) => {
                  const count = parcels.filter(
                    (p) => p.status === status,
                  ).length;
                  const percent = parcels.length
                    ? (count / parcels.length) * 100
                    : 0;
                  return (
                    <TableRow key={status}>
                      <TableCell>{labels[status]}</TableCell>
                      <TableCell>{count}</TableCell>
                      <TableCell>
                        <Typography variant="caption">
                          {percent.toFixed(1)}%
                        </Typography>
                        <LinearProgress
                          aria-label={`${labels[status]} share`}
                          variant="determinate"
                          value={percent}
                        />
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Panel>
    </Box>
  );
}
