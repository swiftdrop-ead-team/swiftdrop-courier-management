import type { LucideIcon } from "lucide-react";
import type { ReactElement } from "react";
import type { Workspace } from "../useDeliveryWorkspace";

export type DashboardTabs = Record<
  string,
  (props: {
    workspace: Workspace;
    trackingNumber: string;
    onBooked: (tracking: string) => void;
  }) => ReactElement
>;

export interface TabHeading {
  title: string;
  description: string;
  Icon: LucideIcon;
}
