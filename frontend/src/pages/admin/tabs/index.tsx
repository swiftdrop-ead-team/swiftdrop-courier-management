import { Analytics } from "@/features/delivery/tabs/Analytics";
import type { DashboardTabs } from "@/features/delivery/tabs/types";
import { Dispatch } from "./Dispatch";
import { ParcelList } from "@/features/delivery/tabs/ParcelList";
import { Pricing } from "./Pricing";
import { LivePricing } from "./LivePricing";
import { Reports } from "./Reports";
import { UserAdministration } from "./Access";
import { Account } from "@/features/delivery/tabs/Account";

export const adminTabs: DashboardTabs = {
  analytics: ({ workspace }) => <Analytics workspace={workspace} />,
  dispatch: ({ workspace }) => <Dispatch workspace={workspace} />,
  deliveries: ({ workspace }) => <ParcelList workspace={workspace} />,
  pricing: ({ workspace }) =>
    workspace.preview ? <Pricing workspace={workspace} /> : <LivePricing />,
  reports: ({ workspace }) => <Reports workspace={workspace} />,
  access: ({ workspace }) => <UserAdministration preview={workspace.preview} />,
  profile: ({ workspace }) => <Account workspace={workspace} />,
};
