import React, { lazy, Suspense } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { MainLayout } from "../components/layout/MainLayout";
import { DashboardLayout } from "../components/layout/DashboardLayout";
import { ProtectedRoute, RoleRoute, GuestRoute } from "./RouteGuards";

// Pages
import { LandingPage } from "../pages/LandingPage";
import { LoginPage } from "../pages/auth/LoginPage";
import { RegisterPage } from "../pages/auth/RegisterPage";
import { MerchantRegisterPage } from "../pages/auth/MerchantRegisterPage";
import { ForgotPasswordPage } from "../pages/auth/ForgotPasswordPage";
import { ResetPasswordPage } from "../pages/auth/ResetPasswordPage";
import { AuthCallbackPage } from "../pages/auth/AuthCallbackPage";

// Dashboards
import { MerchantDashboard } from "../pages/merchant/MerchantDashboard";
import { CourierDashboard } from "../pages/courier/CourierDashboard";
import { AdminDashboard } from "../pages/admin/AdminDashboard";

const CustomerDashboardLayout = lazy(() =>
  import("../components/layout/CustomerDashboardLayout").then((module) => ({
    default: module.CustomerDashboardLayout,
  })),
);
const CustomerDashboard = lazy(() =>
  import("../pages/customer/CustomerDashboard").then((module) => ({
    default: module.CustomerDashboard,
  })),
);
const customerLayout = (
  <Suspense
    fallback={
      <div role="status" className="p-8">
        Loading your workspace...
      </div>
    }
  >
    <CustomerDashboardLayout />
  </Suspense>
);

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

      {/* Temporary frontend-only customer dashboard preview */}
      <Route element={customerLayout}>
        <Route
          path="/customer/dashboard-preview"
          element={
            <Navigate to="/customer/dashboard-preview/overview" replace />
          }
        />
        <Route
          path="/customer/dashboard-preview/:tab"
          element={<CustomerDashboard previewMode />}
        />
      </Route>

      {/* Authenticated Dashboard Routes */}
      <Route element={<ProtectedRoute />}>
        <Route element={customerLayout}>
          {/* Customer Dashboard */}
          <Route element={<RoleRoute requiredRole="CUSTOMER" />}>
            <Route
              path="/customer/dashboard"
              element={<Navigate to="/customer/dashboard/overview" replace />}
            />
            <Route
              path="/customer/dashboard/:tab"
              element={<CustomerDashboard />}
            />
          </Route>
        </Route>
        <Route element={<DashboardLayout />}>
          {/* Temporary cross-role dashboard previews for local development */}
          <Route path="/dashboard-preview/admin" element={<AdminDashboard />} />
          <Route path="/dashboard-preview/merchant" element={<MerchantDashboard />} />
          <Route path="/dashboard-preview/courier" element={<CourierDashboard />} />

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
