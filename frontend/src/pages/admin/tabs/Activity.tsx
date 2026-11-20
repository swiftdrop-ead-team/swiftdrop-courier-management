import type { ReactNode } from "react";
import { Alert, Box } from "@mui/material";
import Table from "examples/Tables/Table";
import { AdminRegister } from "../AdminWorkspaceUI";
import { AdminBadge, AdminEmpty } from "../AdminShared";
import { dateTime, type AdminActivity } from "../adminData";

export function Activity({
  error,
  count,
  total,
  items,
  filtered,
  filters,
  children,
}: {
  error: string;
  count: number;
  total: number;
  items: AdminActivity[];
  filtered: boolean;
  filters: ReactNode;
  children: ReactNode;
}) {
  if (error) return <Alert severity="error">{error}</Alert>;
  return (
    <AdminRegister
      title="Merchant decisions"
      count={`${count} of ${total} reviews`}
      filters={filters}
    >
      {count ? (
        <Table
          columns={[
            { name: "business", align: "left" },
            { name: "decision", align: "center" },
            { name: "reviewer", align: "left" },
            { name: "time", align: "left" },
            { name: "notes", align: "left" },
          ]}
          rows={items.map((item) => ({
            id: item.id,
            hasBorder: true,
            business: item.merchant,
            decision: <AdminBadge status={item.decision} />,
            reviewer: item.actor,
            time: dateTime(item.timestamp),
            notes: (
              <Box
                component="span"
                sx={{
                  display: "block",
                  whiteSpace: "normal",
                  maxWidth: 260,
                  overflowWrap: "anywhere",
                }}
              >
                {item.notes || "No notes added"}
              </Box>
            ),
          }))}
        />
      ) : (
        <AdminEmpty
          title={filtered ? "No matching activity" : "No reviewed applications"}
          text="Approved and rejected applications appear here after review."
        />
      )}
      {children}
    </AdminRegister>
  );
}
