import type { ReactNode } from "react";
import { Card } from "@mui/material";
import { AdminRegister } from "../AdminWorkspaceUI";
import { AdminEmpty } from "../AdminShared";

export function Merchants({
  available,
  pendingCount,
  count,
  filters,
  children,
}: {
  available: boolean;
  pendingCount: number;
  count: number;
  filters: ReactNode;
  children: ReactNode;
}) {
  if (!available)
    return (
      <Card>
        <AdminEmpty
          title="Review queue unavailable"
          text="Refresh to retry loading merchant applications."
        />
      </Card>
    );
  return (
    <AdminRegister
      title="Application queue"
      count={`${pendingCount} pending · ${count} matching applications`}
      filters={filters}
    >
      {children}
    </AdminRegister>
  );
}
