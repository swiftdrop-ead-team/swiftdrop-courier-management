import React from 'react';
import { Link, useLocation } from 'react-router-dom';

const dashboardLinks = [
  { key: 'admin', label: 'Admin', path: '/dashboard-preview/admin' },
  { key: 'merchant', label: 'Merchant', path: '/dashboard-preview/merchant' },
  { key: 'courier', label: 'Courier', path: '/dashboard-preview/courier' },
  { key: 'customer', label: 'Customer', path: '/customer/dashboard-preview/overview' },
];

const getCurrentDashboard = (pathname: string) => {
  if (pathname.startsWith('/admin/dashboard') || pathname.startsWith('/dashboard-preview/admin')) return 'admin';
  if (pathname.startsWith('/merchant/dashboard') || pathname.startsWith('/dashboard-preview/merchant')) return 'merchant';
  if (pathname.startsWith('/courier/dashboard') || pathname.startsWith('/dashboard-preview/courier')) return 'courier';
  if (pathname.startsWith('/customer/dashboard')) return 'customer';
  return null;
};

export const DashboardQuickLinks: React.FC = () => {
  const { pathname } = useLocation();
  const currentDashboard = getCurrentDashboard(pathname);
  if (!import.meta.env.DEV) return null;

  return (
    <nav aria-label="Temporary dashboard previews" className="flex flex-wrap items-center justify-end gap-1.5">
      <Link to="/dashboard-preview" className="px-2 text-xs font-semibold text-blue-700">Portal</Link>
      <span className="mr-1 text-[10px] font-semibold uppercase tracking-wider text-slate-400">
        Quick view
      </span>
      {dashboardLinks
        .filter((dashboard) => dashboard.key !== currentDashboard)
        .map((dashboard) => (
          <Link
            key={dashboard.key}
            to={dashboard.path}
            className="rounded-full border border-slate-200 bg-white px-2.5 py-1 text-[10px] font-semibold text-slate-600 transition-colors hover:border-blue-300 hover:bg-blue-50 hover:text-blue-800"
          >
            {dashboard.label}
          </Link>
        ))}
    </nav>
  );
};
