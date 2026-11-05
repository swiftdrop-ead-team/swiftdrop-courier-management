import React from "react";
import { useLocation, useNavigate, type Location } from "react-router-dom";
import {
  Bell,
  Building2,
  Clock,
  FileSpreadsheet,
  Key,
  LayoutDashboard,
  MapPin,
  Package,
  Send,
  Settings,
  Truck,
  Users,
} from "lucide-react";
import { RoleDashboardLayout } from "./RoleDashboardLayout";
import type { NavItem, NavSection } from "./RoleDashboardLayout";

/* ================================================================
   Role-based navigation configuration
   ================================================================ */

function buildNavigation(role: string, base: string): NavSection[] {
  switch (role) {
    case "ADMIN":
      return [
        {
          heading: "ADMINISTRATION",
          items: [
            { tab: "overview", label: "Admin Overview", icon: LayoutDashboard, path: base },
            { tab: "merchants", label: "Pending Approvals", icon: Clock, path: `${base}#merchants`, badge: "Live" },
            { tab: "users", label: "User Management", icon: Users, path: `${base}#users` },
          ],
        },
      ];
    case "MERCHANT":
      return [
        {
          heading: "OVERVIEW",
          items: [
            { tab: "overview", label: "Dashboard", icon: LayoutDashboard, path: `${base}/overview` },
          ],
        },
        {
          heading: "SHIPMENTS",
          items: [
            { tab: "shipments", label: "My Shipments", icon: Package, path: `${base}/shipments` },
            { tab: "create-shipment", label: "Send a parcel", icon: Send, path: `${base}/create-shipment`, badge: "New" },
            { tab: "bulk-upload", label: "Upload many", icon: FileSpreadsheet, path: `${base}/bulk-upload` },
            { tab: "tracking", label: "Track a parcel", icon: MapPin, path: `${base}/tracking` },
            { tab: "history", label: "Past deliveries", icon: Clock, path: `${base}/history` },
          ],
        },
        {
          heading: "ACCOUNT",
          items: [
            { tab: "profile", label: "Business Profile", icon: Building2, path: `${base}/profile` },
            { tab: "api", label: "API Integration", icon: Key, path: `${base}/api` },
            { tab: "notifications", label: "Notifications", icon: Bell, path: `${base}/notifications` },
            { tab: "settings", label: "Settings", icon: Settings, path: `${base}/settings` },
          ],
        },
      ];
    case "COURIER":
      return [
        {
          heading: "DISPATCH CONSOLE",
          items: [
            { tab: "overview", label: "Courier Console", icon: LayoutDashboard, path: base },
            { tab: "deliveries", label: "Assigned Drops", icon: Truck, path: `${base}#deliveries` },
            { tab: "route", label: "Active Route", icon: MapPin, path: `${base}#route` },
          ],
        },
      ];
    default:
      return [];
  }
}

function getRoleConfig(role: string) {
  switch (role) {
    case "ADMIN":
      return { subtitle: "Administration", profileLabel: "Admin account", breadcrumbRole: "Admin" };
    case "MERCHANT":
      return { subtitle: "Merchant workspace", profileLabel: "Business account", breadcrumbRole: "Merchant" };
    case "COURIER":
      return { subtitle: "Dispatch console", profileLabel: "Courier account", breadcrumbRole: "Courier" };
    default:
      return { subtitle: "Logistics Platform", profileLabel: "Account", breadcrumbRole: role };
  }
}

function getActiveItem(location: Location<any>, navigation: NavSection[]): NavItem | undefined {
  const allItems = navigation.flatMap((s) => s.items);
  return allItems.find(({ path }) => {
    const [itemPath, itemHash] = path.split("#");
    // For hash-based nav, match exactly
    if (itemHash) {
      return location.pathname === itemPath && location.hash === `#${itemHash}`;
    }
    // For path-based nav, check if the pathname starts with the item path
    // This handles /merchant/dashboard/shipment-detail/123 matching /merchant/dashboard/shipment-detail
    return location.pathname === itemPath || location.pathname.startsWith(`${itemPath}/`);
  }) || allItems[0];
}

/* ================================================================
   Unified Dashboard Layout — uses shared RoleDashboardLayout
   ================================================================ */

export const UnifiedDashboardLayout: React.FC<{ role: string; basePath: string }> = ({ role, basePath }) => {
  const location = useLocation();

  const navigation = buildNavigation(role, basePath);
  const config = getRoleConfig(role);

  const searchPlaceholder = role === "MERCHANT" ? "Search shipments..." : "Search...";

  return (
    <RoleDashboardLayout
      role={role}
      basePath={basePath}
      sidebarSubtitle={config.subtitle}
      navigation={navigation}
      getActiveItem={getActiveItem}
      profileLabel={config.profileLabel}
      breadcrumbRole={config.breadcrumbRole}
      searchPlaceholder={searchPlaceholder}
    />
  );
};
