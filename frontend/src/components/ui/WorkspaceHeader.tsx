import "./workspace.css";
import type { ReactNode } from "react";
import { Box, Stack } from "@mui/material";
import type { LucideIcon } from "lucide-react";
import SoftBox from "components/SoftBox";
import SoftTypography from "components/SoftTypography";

export function WorkspaceHeader({ title, description, Icon, action }: {
  title: string;
  description: string;
  Icon: LucideIcon;
  action?: ReactNode;
}) {
  return (
    <Stack component="header" direction="row" alignItems="center" justifyContent="space-between" flexWrap="wrap" gap={1.5}>
        <Stack direction="row" alignItems="center" spacing={1.5} sx={{ flex: "1 1 300px", minWidth: 0 }}>
          <SoftBox aria-hidden="true" bgColor="dark" variant="gradient" borderRadius="lg" shadow="sm"
            sx={{ width: 42, height: 42, display: "grid", placeItems: "center", flexShrink: 0, color: "#ffdc59" }}>
            <Icon size={23} strokeWidth={1.8} />
          </SoftBox>
          <Box>
            <SoftTypography component="h1" variant="h4" fontWeight="bold" sx={{ lineHeight: 1.2, letterSpacing: "-0.025em" }}>{title}</SoftTypography>
            <SoftTypography variant="body2" color="text" mt={0.5}>{description}</SoftTypography>
          </Box>
        </Stack>
        {action}
    </Stack>
  );
}
