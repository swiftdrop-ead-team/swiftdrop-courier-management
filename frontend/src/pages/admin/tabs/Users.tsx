import type { ReactNode } from "react";
import { Card } from "@mui/material";
import SoftButton from "components/SoftButton";
import { Download } from "lucide-react";
import type { User } from "@/types/auth";
import { AdminRegister } from "../AdminWorkspaceUI";
import { AdminEmpty } from "../AdminShared";
import { downloadCsv } from "../adminData";

export function Users({
  users,
  matches,
  filters,
  children,
}: {
  users: User[] | null;
  matches: User[];
  filters: ReactNode;
  children: ReactNode;
}) {
  if (!users)
    return (
      <Card>
        <AdminEmpty
          title="Account directory unavailable"
          text="Refresh to retry loading user accounts."
        />
      </Card>
    );
  return (
    <AdminRegister
      title="Account directory"
      count={`${matches.length} of ${users.length} accounts`}
      tools={
        <SoftButton
          variant="outlined"
          color="dark"
          disabled={!matches.length}
          onClick={() =>
            downloadCsv("swiftdrop-users.csv", [
              ["Name", "Email", "Phone", "Roles", "Status", "Merchant status"],
              ...matches.map((item) => [
                item.fullName,
                item.email,
                item.phoneNumber || "",
                item.roles.join(", "),
                item.status,
                item.merchantApprovalStatus || "",
              ]),
            ])
          }
        >
          <Download size={15} style={{ marginRight: 6 }} />
          Export users
        </SoftButton>
      }
      filters={filters}
    >
      {children}
    </AdminRegister>
  );
}
