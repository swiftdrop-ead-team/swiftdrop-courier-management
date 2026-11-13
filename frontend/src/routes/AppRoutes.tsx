import React, { lazy, Suspense } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { MainLayout } from "../components/layout/MainLayout";
import { UnifiedDashboardLayout } from "../components/layout/UnifiedDashboardLayout";
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
import {
  AdminWorkspace,
  DeliveryDashboard,
} from "../features/delivery/DeliveryDashboard";
import { PublicTrackingPage } from "../features/delivery/TrackingView";
import { DashboardPreviewPortal } from "../pages/DashboardPreviewPortal";

const CustomerDashboardLayout = lazy(() =>
  import("../components/layout/CustomerDashboardLayout").then((module) => ({
    default: module.CustomerDashboardLayout,
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
      {import.meta.env.DEV && (
        <>
          <Route
            path="/dashboard-preview"
            element={<DashboardPreviewPortal />}
          />
          <Route
            element={
              <UnifiedDashboardLayout
                role="ADMIN"
                basePath="/dashboard-preview/admin"
              />
            }
          >
            <Route
              path="/dashboard-preview/admin/:tab?"
              element={<AdminWorkspace previewMode />}
            />
          </Route>
          <Route
            element={
              <UnifiedDashboardLayout
                role="COURIER"
                basePath="/dashboard-preview/courier"
              />
            }
          >
            <Route
              path="/dashboard-preview/courier/:tab?"
              element={<DeliveryDashboard role="COURIER" previewMode />}
            />
          </Route>
          <Route
            element={
              <UnifiedDashboardLayout
                role="MERCHANT"
                basePath="/dashboard-preview/merchant"
              />
            }
          >
            <Route
              path="/dashboard-preview/merchant/:tab?/:id?"
              element={<DeliveryDashboard role="MERCHANT" previewMode />}
            />
          </Route>
          <Route element={customerLayout}>
            <Route
              path="/customer/dashboard-preview"
              element={
                <Navigate to="/customer/dashboard-preview/overview" replace />
              }
            />
            <Route
              path="/customer/dashboard-preview/:tab"
              element={<DeliveryDashboard role="CUSTOMER" previewMode />}
            />
          </Route>
          <Route element={<MainLayout />}>
            <Route
              path="/tracking-preview/:trackingNumber?"
              element={<PublicTrackingPage preview />}
            />
          </Route>
        </>
      )}
      {/* Public Pages with Main Navigation and Footer */}
      <Route element={<MainLayout />}>
        <Route path="/" element={<LandingPage />} />
        <Route
          path="/track/:trackingNumber?"
          element={<PublicTrackingPage />}
        />
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
        <Route element={customerLayout}>
          {/* Customer Dashboard */}
          <Route element={<RoleRoute requiredRole="CUSTOMER" />}>
            <Route
              path="/customer/dashboard"
              element={<Navigate to="/customer/dashboard/overview" replace />}
            />
            <Route
              path="/customer/dashboard/:tab"
              element={<DeliveryDashboard role="CUSTOMER" />}
            />
          </Route>
        </Route>
        {/* Merchant Dashboard */}
        <Route element={<RoleRoute requiredRole="MERCHANT" />}>
          <Route
            element={
              <UnifiedDashboardLayout
                role="MERCHANT"
                basePath="/merchant/dashboard"
              />
            }
          >
            <Route
              path="/merchant/dashboard/:tab?/:id?"
              element={<DeliveryDashboard role="MERCHANT" />}
            />
          </Route>
        </Route>

        {/* Courier Dashboard */}
        <Route element={<RoleRoute requiredRole="COURIER" />}>
          <Route
            element={
              <UnifiedDashboardLayout
                role="COURIER"
                basePath="/courier/dashboard"
              />
            }
          >
            <Route
              path="/courier/dashboard/:tab?"
              element={<DeliveryDashboard role="COURIER" />}
            />
          </Route>
        </Route>

        {/* Admin Dashboard */}
        <Route element={<RoleRoute requiredRole="ADMIN" />}>
          <Route
            element={
              <UnifiedDashboardLayout
                role="ADMIN"
                basePath="/admin/dashboard"
              />
            }
          >
            <Route path="/admin/dashboard/:tab?" element={<AdminWorkspace />} />
          </Route>
        </Route>
      </Route>

      {/* Catch-all fallback */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};
