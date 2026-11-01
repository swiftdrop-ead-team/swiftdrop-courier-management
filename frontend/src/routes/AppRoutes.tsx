import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { MainLayout } from '../components/layout/MainLayout';
import { DashboardLayout } from '../components/layout/DashboardLayout';
import { ProtectedRoute, RoleRoute, GuestRoute } from './RouteGuards';

// Pages
import { LandingPage } from '../pages/LandingPage';
import { LoginPage } from '../pages/auth/LoginPage';
import { RegisterPage } from '../pages/auth/RegisterPage';
import { MerchantRegisterPage } from '../pages/auth/MerchantRegisterPage';
import { ForgotPasswordPage } from '../pages/auth/ForgotPasswordPage';
import { ResetPasswordPage } from '../pages/auth/ResetPasswordPage';
import { AuthCallbackPage } from '../pages/auth/AuthCallbackPage';

// Dashboards
import { CustomerDashboard } from '../pages/customer/CustomerDashboard';
import { MerchantDashboard } from '../pages/merchant/MerchantDashboard';
import { CourierDashboard } from '../pages/courier/CourierDashboard';
import { AdminDashboard } from '../pages/admin/AdminDashboard';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Public Pages with Main Navigation and Footer */}
      <Route element={<MainLayout />}>
        <Route path="/" element={<LandingPage />} />
      </Route>

      {/* Guest-Only Auth Routes (Redirects if already signed in) */}
      <Route element={<GuestRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/merchant/register" element={<MerchantRegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
      </Route>

      {/* OAuth2 Callback Handler */}
      <Route path="/auth/callback" element={<AuthCallbackPage />} />

      {/* Authenticated Dashboard Routes */}
      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          {/* Customer Dashboard */}
          <Route element={<RoleRoute requiredRole="CUSTOMER" />}>
            <Route path="/customer/dashboard" element={<CustomerDashboard />} />
          </Route>

          {/* Merchant Dashboard */}
          <Route element={<RoleRoute requiredRole="MERCHANT" />}>
            <Route path="/merchant/dashboard" element={<MerchantDashboard />} />
          </Route>

          {/* Courier Dashboard */}
          <Route element={<RoleRoute requiredRole="COURIER" />}>
            <Route path="/courier/dashboard" element={<CourierDashboard />} />
          </Route>

          {/* Admin Dashboard */}
          <Route element={<RoleRoute requiredRole="ADMIN" />}>
            <Route path="/admin/dashboard" element={<AdminDashboard />} />
          </Route>
        </Route>
      </Route>

      {/* Catch-all fallback */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};
