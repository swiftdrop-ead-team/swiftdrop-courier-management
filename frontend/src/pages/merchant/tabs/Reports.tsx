import Table from "examples/Tables/Table";
import { useState } from "react";
import { Alert, Box, LinearProgress } from "@mui/material";
import {
  ActionButton,
  Empty,
  Field,
  Metrics,
  Panel,
  Status,
  WorkspaceText as Typography,
  exportParcels,
} from "@/features/delivery/components";
import {
  day,
  labels,
  money,
  statuses,
  terminal,
} from "@/features/delivery/model";
import type { Workspace } from "@/features/delivery/useDeliveryWorkspace";

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
            alignItems: "flex-end",
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
            sx={{ alignSelf: "flex-end", mb: 1 }}
            disabled={unavailable || !parcels.length}
            onClick={() => exportParcels(parcels)}
          >
            Export report CSV
          </ActionButton>
          <ActionButton variant="text" sx={{ alignSelf: "flex-end", mb: 1 }}
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
          <Table
            aria-label="Merchant delivery outcomes"
            columns={["Status", "Shipments", "Share of bookings"].map(
              (name) => ({ name, align: "left" }),
            )}
            rows={statuses.map((status) => {
              const count = parcels.filter((p) => p.status === status).length;
              const percent = parcels.length
                ? (count / parcels.length) * 100
                : 0;
              return {
                id: status,
                hasBorder: true,
                Status: <Status value={status} />,
                Shipments: count,
                "Share of bookings": (
                  <>
                    <Typography component="span" variant="caption">
                      {percent.toFixed(1)}%
                    </Typography>
                    <LinearProgress
                      aria-label={`${labels[status]} share`}
                      variant="determinate"
                      value={percent}
                    />
                  </>
                ),
              };
            })}
          />
        )}
      </Panel>
    </Box>
  );
}
