import type { ReactNode } from "react";
import { Box, Card, Stack } from "@mui/material";
import { BarChart3, Building2, ClipboardList, History, Package, Receipt, ShieldCheck, Truck, UserRound, UsersRound } from "lucide-react";
import { WorkspaceHeader } from "@/components/ui/WorkspaceHeader";
import SoftTypography from "components/SoftTypography";
import "./admin-workspace.css";

const pages = {
  "create-booking": { title: "Create customer booking", description: "Book a delivery for an existing customer, then assign a courier.", Icon: Package },
  analytics: { title: "Platform analytics", description: "Explore shipment trends, delivery outcomes and account growth.", Icon: BarChart3 },
  merchants: { title: "Merchant approvals", description: "Review business details and decide which applications to approve.", Icon: Building2 },
  users: { title: "User management", description: "Find an account by name, contact details, role or status.", Icon: UsersRound },
  activity: { title: "Review activity", description: "Look up merchant decisions, reviewer notes and review dates.", Icon: History },
  dispatch: { title: "Dispatch workspace", description: "Assign a parcel and check the courier’s remaining capacity.", Icon: Truck },
  deliveries: { title: "All deliveries", description: "Find a shipment and inspect its route, charges and delivery history.", Icon: Package },
  pricing: { title: "Delivery pricing", description: "Set booking rates and availability for each delivery service.", Icon: Receipt },
  reports: { title: "Delivery reports", description: "Compare bookings and delivery outcomes over a selected period.", Icon: ClipboardList },
  access: { title: "Manage user access", description: "Review an account’s permissions and update its roles or status.", Icon: ShieldCheck },
  profile: { title: "Admin profile", description: "Manage your contact details and delivery notifications.", Icon: UserRound },
};
export type AdminPage = keyof typeof pages;

export function AdminWorkspaceHeader({ page, action }: { page: AdminPage; action?: ReactNode }) {
  return <WorkspaceHeader {...pages[page]} action={action} />;
}

export function AdminRegister({ title, count, tools, filters, children }: {
  title: string; count: string; tools?: ReactNode; filters: ReactNode; children: ReactNode;
}) {
  return (
    <Card sx={{
      overflow: "hidden",
      border: "1px solid #e2e8f0",
      borderRadius: "12px",
      boxShadow: "0 2px 8px rgba(15, 39, 65, 0.04)",
      background: "#ffffff",
    }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" gap={2} flexWrap="wrap" sx={{ px: { xs: 2, md: 3 }, py: 2.5 }}>
        <Box>
          <SoftTypography component="h2" variant="h6" fontWeight="bold" sx={{ fontSize: "1.05rem", letterSpacing: "-0.01em" }}>{title}</SoftTypography>
          <SoftTypography variant="caption" color="text" sx={{ fontSize: "0.78rem" }}>{count}</SoftTypography>
        </Box>
        {tools}
      </Stack>
      <Box sx={{
        px: { xs: 2, md: 3 },
        py: 2,
        bgcolor: "#f8fafc",
        borderBlock: "1px solid #e9ecef",
        display: "flex",
        alignItems: "flex-end",
        flexWrap: "wrap",
        gap: 2,
        "& select": { width: "100%" },
      }}>
        {filters}
      </Box>
      <Box sx={{
        minWidth: 0,
        overflowX: "auto",
      }}>{children}</Box>
    </Card>
  );
}
