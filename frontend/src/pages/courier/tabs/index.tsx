import { BarChart3 } from "lucide-react";
import { Analytics } from "@/features/delivery/tabs/Analytics";
import { Bell, History, Route, Truck, UserRound } from "lucide-react";
import type { DashboardTabs, TabHeading } from "@/features/delivery/tabs/types";
import { ParcelList } from "@/features/delivery/tabs/ParcelList";
import { Account } from "@/features/delivery/tabs/Account";
import { Notifications } from "@/features/delivery/tabs/Notifications";

export const courierTabs: DashboardTabs = {
  analytics: ({ workspace }) => <Analytics workspace={workspace} />,
  deliveries: ({ workspace }) => <ParcelList workspace={workspace} />,
  route: ({ workspace }) => <ParcelList workspace={workspace} route />,
  history: ({ workspace }) => <ParcelList workspace={workspace} history />,
  profile: ({ workspace }) => <Account workspace={workspace} />,
  notifications: ({ workspace }) => <Notifications workspace={workspace} />,
};

export const courierHeadings: Record<string, TabHeading> = {
  analytics: { title: "Analytics", description: "Explore delivery trends and compare performance over time.", Icon: BarChart3 },
  deliveries: { title: "Deliveries", description: "Review assignments and update delivery progress.", Icon: Truck },
  route: { title: "Active route", description: "Your pickups and deliveries for the day.", Icon: Route },
  history: { title: "Delivery history", description: "Review deliveries and download receipts.", Icon: History },
  profile: { title: "Your profile", description: "Manage contact details and delivery preferences.", Icon: UserRound },
  notifications: { title: "Notifications", description: "Keep track of important delivery updates.", Icon: Bell },
};
