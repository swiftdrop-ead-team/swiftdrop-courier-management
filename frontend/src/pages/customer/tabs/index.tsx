import { Bell, History, MapPin, PackagePlus, Settings } from "lucide-react";
import type { DashboardTabs, TabHeading } from "@/features/delivery/tabs/types";
import { BookingForm } from "@/features/delivery/tabs/BookingForm";
import { TrackingView } from "@/features/delivery/tabs/TrackingView";
import { ParcelList } from "@/features/delivery/tabs/ParcelList";
import { Account } from "@/features/delivery/tabs/Account";
import { Notifications } from "@/features/delivery/tabs/Notifications";

export const customerTabs: DashboardTabs = {
  send: ({ workspace, onBooked }) => (
    <BookingForm workspace={workspace} onBooked={onBooked} />
  ),
  track: ({ workspace, trackingNumber }) => (
    <TrackingView preview={workspace.preview} initialNumber={trackingNumber} />
  ),
  history: ({ workspace }) => <ParcelList workspace={workspace} />,
  settings: ({ workspace }) => <Account workspace={workspace} />,
  notifications: ({ workspace }) => <Notifications workspace={workspace} />,
};

export const customerHeadings: Record<string, TabHeading> = {
  send: { title: "Send a parcel", description: "Get an estimate and confirm your delivery.", Icon: PackagePlus },
  track: { title: "Track a shipment", description: "Follow your parcel through each delivery stage.", Icon: MapPin },
  history: { title: "Delivery history", description: "Review deliveries and download receipts.", Icon: History },
  settings: { title: "Account settings", description: "Your profile, preferences, and saved addresses.", Icon: Settings },
  notifications: { title: "Notifications", description: "Keep track of important delivery updates.", Icon: Bell },
};
