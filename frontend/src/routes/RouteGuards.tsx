import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { UserRole } from '../types/auth';
import { Loader2 } from 'lucide-react';

export const ProtectedRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#F8FAFC]">
        <Loader2 className="w-8 h-8 text-teal-600 animate-spin" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
};

interface RoleRouteProps {
  requiredRole: UserRole;
}

export const RoleRoute: React.FC<RoleRouteProps> = ({ requiredRole }) => {
  const { user, hasRole, getDefaultDashboardRoute, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#F8FAFC]">
        <Loader2 className="w-8 h-8 text-teal-600 animate-spin" />
      </div>
    );
  }

  if (!hasRole(requiredRole)) {
    // Redirect to the dashboard they actually belong to
    return <Navigate to={getDefaultDashboardRoute()} replace />;
  }

  return <Outlet />;
};

export const GuestRoute: React.FC = () => {
  const { isAuthenticated, isLoading, getDefaultDashboardRoute } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#F8FAFC]">
        <Loader2 className="w-8 h-8 text-teal-600 animate-spin" />
      </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to={getDefaultDashboardRoute()} replace />;
  }

  return <Outlet />;
};
