import SoftProgress from "components/SoftProgress";
import { WorkspaceText as Typography } from "@/features/delivery/components";
import { useState } from "react";
import { Alert, Box, Grid } from "@mui/material";
import { ActionButton as SoftButton } from "@/features/delivery/components";
import { downloadCsv } from "@/pages/admin/adminData";
import { day, labels, statuses } from "@/features/delivery/model";
import type { Workspace } from "@/features/delivery/useDeliveryWorkspace";
import { Empty, Field, Metrics, Panel } from "@/features/delivery/components";

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
        title="Reporting period"
        subtitle="Reports group deliveries by booking date. Success/failure rates use completed attempts only."
      >
        <Grid container spacing={2} alignItems="flex-end" className="admin-filter-grid">
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
              sx={{ mb: 1, minHeight: 44 }}
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
                <Box key={date} className="admin-report-day">
                  <Typography variant="body2">
                    {date} · {count} deliveries
                  </Typography>
                  <SoftProgress
                    aria-label={`Deliveries on ${date}`}
                    color="primary"
                    value={
                      (count /
                        Math.max(
                          ...days.map(
                            (d) =>
                              parcels.filter((p) => day(p.createdAt) === d)
                                .length,
                          ),
                          1,
                        )) *
                      100
                    }
                    sx={{ mt: 1 }}
                  />
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
