import React, { lazy, Suspense } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { ProtectedRoute, RoleRoute, GuestRoute } from "./RouteGuards";

const MainLayout = lazy(() =>
  import("../components/layout/MainLayout").then((module) => ({
    default: module.MainLayout,
  })),
);
const UnifiedDashboardLayout = lazy(() =>
  import("../components/layout/UnifiedDashboardLayout").then((module) => ({
    default: module.UnifiedDashboardLayout,
  })),
);
const LandingPage = lazy(() => import("../pages/LandingPage").then((module) => ({
  default: module.LandingPage,
})));
const LoginPage = lazy(() => import("../pages/auth/LoginPage").then((module) => ({
  default: module.LoginPage,
})));
const RegisterPage = lazy(() => import("../pages/auth/RegisterPage").then((module) => ({
  default: module.RegisterPage,
})));
const MerchantRegisterPage = lazy(() => import("../pages/auth/MerchantRegisterPage").then((module) => ({
  default: module.MerchantRegisterPage,
})));
const ForgotPasswordPage = lazy(() => import("../pages/auth/ForgotPasswordPage").then((module) => ({
  default: module.ForgotPasswordPage,
})));
const ResetPasswordPage = lazy(() => import("../pages/auth/ResetPasswordPage").then((module) => ({
  default: module.ResetPasswordPage,
})));
const AuthCallbackPage = lazy(() => import("../pages/auth/AuthCallbackPage").then((module) => ({
  default: module.AuthCallbackPage,
})));

// Dashboards
const DeliveryDashboard = lazy(() =>
  import("../features/delivery/DeliveryDashboard").then((m) => ({
    default: m.DeliveryDashboard,
  })),
);
const AdminWorkspace = lazy(() =>
  import("../features/delivery/DeliveryDashboard").then((m) => ({
    default: m.AdminWorkspace,
  })),
);
const PublicTrackingPage = lazy(() =>
  import("../features/delivery/TrackingView").then((module) => ({
    default: module.PublicTrackingPage,
  })),
);


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
    <Suspense
      fallback={
        <div role="status" className="p-8">
          Loading your workspace...
        </div>
      }
    >
      <Routes>
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
              <Route
                path="/admin/dashboard/:tab?"
                element={<AdminWorkspace />}
              />
            </Route>
          </Route>
        </Route>

        {/* Catch-all fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
};
