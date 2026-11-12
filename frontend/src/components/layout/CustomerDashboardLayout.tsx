import React from 'react';
import { useLocation } from 'react-router-dom';
import { UnifiedDashboardLayout } from './UnifiedDashboardLayout';

export const CustomerDashboardLayout = () => {
  const { pathname } = useLocation();
  const basePath = pathname.startsWith('/customer/dashboard-preview') ? '/customer/dashboard-preview' : '/customer/dashboard';
  return <UnifiedDashboardLayout role="CUSTOMER" basePath={basePath} />;
};
