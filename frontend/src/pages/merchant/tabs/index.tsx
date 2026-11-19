import { BarChart3 } from "lucide-react";
import { Analytics } from "@/features/delivery/tabs/Analytics";
import { Bell, ClipboardList, History, MapPin, Package, PackagePlus, Plug, Upload, UserRound } from "lucide-react";
import type { DashboardTabs, TabHeading } from "@/features/delivery/tabs/types";
import { ParcelList } from "@/features/delivery/tabs/ParcelList";
import { BookingForm } from "@/features/delivery/tabs/BookingForm";
import { BulkOrders } from "./BulkUpload";
import { TrackingView } from "@/features/delivery/tabs/TrackingView";
import { Account } from "@/features/delivery/tabs/Account";
import { MerchantReports } from "./Reports";
import { MerchantIntegration } from "./StoreConnections";
import { Notifications } from "@/features/delivery/tabs/Notifications";

export const merchantTabs: DashboardTabs = {
  analytics: ({ workspace }) => <Analytics workspace={workspace} />,
  shipments: ({ workspace }) => <ParcelList workspace={workspace} />,
  "create-shipment": ({ workspace, onBooked }) => (
    <BookingForm workspace={workspace} onBooked={onBooked} />
  ),
  "bulk-upload": ({ workspace }) => <BulkOrders workspace={workspace} />,
  tracking: ({ workspace, trackingNumber }) => (
    <TrackingView preview={workspace.preview} initialNumber={trackingNumber} />
  ),
  history: ({ workspace }) => <ParcelList workspace={workspace} history />,
  profile: ({ workspace }) => <Account workspace={workspace} />,
  reports: ({ workspace }) => <MerchantReports workspace={workspace} />,
  api: ({ workspace }) => <MerchantIntegration workspace={workspace} />,
  notifications: ({ workspace }) => <Notifications workspace={workspace} />,
};

export const merchantHeadings: Record<string, TabHeading> = {
  analytics: { title: "Analytics", description: "Explore delivery trends and compare performance over time.", Icon: BarChart3 },
  shipments: { title: "My shipments", description: "All your orders and their delivery states.", Icon: Package },
  "create-shipment": { title: "Send a parcel", description: "Create an order for your business.", Icon: PackagePlus },
  "bulk-upload": { title: "Upload orders", description: "Validate a CSV and create multiple delivery requests.", Icon: Upload },
  tracking: { title: "Track a parcel", description: "Follow delivery progress using a tracking number.", Icon: MapPin },
  history: { title: "Delivery history", description: "Review deliveries and download receipts.", Icon: History },
  profile: { title: "Your profile", description: "Manage contact details and delivery preferences.", Icon: UserRound },
  reports: { title: "Reports & COD", description: "Understand delivery volumes and outcomes.", Icon: ClipboardList },
  api: { title: "Store connections", description: "Optional: automate orders and delivery updates with API keys and webhooks.", Icon: Plug },
  notifications: { title: "Notifications", description: "Keep track of important delivery updates.", Icon: Bell },
};
