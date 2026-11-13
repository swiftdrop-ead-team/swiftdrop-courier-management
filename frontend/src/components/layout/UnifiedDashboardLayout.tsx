import React from "react";
import { type Location } from "react-router-dom";
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
    case "CUSTOMER":
      return [
        { heading: 'HOME', items: [{ tab: 'overview', label: 'Dashboard', icon: LayoutDashboard, path: `${base}/overview` }] },
        { heading: 'DELIVERIES', items: [
          { tab: 'send', label: 'Send a parcel', icon: Send, path: `${base}/send`, badge: 'New' },
          { tab: 'track', label: 'Track shipment', icon: MapPin, path: `${base}/track` },
          { tab: 'history', label: 'Shipment history', icon: Package, path: `${base}/history` },
          { tab: 'notifications', label: 'Notifications', icon: Bell, path: `${base}/notifications` },
        ] },
        { heading: 'ACCOUNT', items: [{ tab: 'settings', label: 'Profile & addresses', icon: Settings, path: `${base}/settings` }] },
      ];
    case "ADMIN":
      return [
        {
          heading: "ADMINISTRATION",
          items: [
            { tab: "overview", label: "Admin Overview", icon: LayoutDashboard, path: `${base}/overview` },
            { tab: "merchants", label: "Merchant Approvals", icon: Clock, path: `${base}/merchants` },
            { tab: "users", label: "User Management", icon: Users, path: `${base}/users` },
            { tab: "activity", label: "Review Activity", icon: FileSpreadsheet, path: `${base}/activity` },
            { tab: 'dispatch', label: 'Dispatch & workloads', icon: Truck, path: `${base}/dispatch` },
            { tab: 'deliveries', label: 'All deliveries', icon: Package, path: `${base}/deliveries` },
            { tab: 'pricing', label: 'Zones & pricing', icon: MapPin, path: `${base}/pricing` },
            { tab: 'reports', label: 'Delivery reports', icon: FileSpreadsheet, path: `${base}/reports` },
            { tab: 'access', label: 'Manage user access', icon: Users, path: `${base}/access` },
          ],
        },
        { heading: 'ACCOUNT', items: [{ tab: 'profile', label: 'Admin Profile', icon: Settings, path: `${base}/profile` }] },
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
            { tab: "overview", label: "Overview", icon: LayoutDashboard, path: `${base}/overview` },
            { tab: "deliveries", label: "Assigned Deliveries", icon: Truck, path: `${base}/deliveries` },
            { tab: "route", label: "Active Route", icon: MapPin, path: `${base}/route` },
            { tab: "history", label: "Delivery History", icon: Clock, path: `${base}/history` },
            { tab: 'notifications', label: 'Notifications', icon: Bell, path: `${base}/notifications` },
          ],
        },
        { heading: 'ACCOUNT', items: [{ tab: 'profile', label: 'Courier Profile', icon: Users, path: `${base}/profile` }] },
      ];
    default:
      return [];
  }
}

function getRoleConfig(role: string) {
  switch (role) {
    case "CUSTOMER":
      return { subtitle: 'Customer workspace', profileLabel: 'Personal account', breadcrumbRole: 'Customer' };
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
  return [...allItems].sort((a, b) => b.path.length - a.path.length).find(({ path }) => {
    const [itemPath, itemHash] = path.split("#");
    if (location.pathname.includes("/shipment-detail/") && path.endsWith("/shipments")) return true;
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
