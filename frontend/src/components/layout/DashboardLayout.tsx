import React, { useEffect } from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Logo } from '../ui/Logo';
import { SoftCard } from '../ui/SoftUI';
import { 
  LayoutDashboard, 
  Package, 
  Send, 
  MapPin, 
  Users, 
  CheckCircle2, 
  Clock, 
  LogOut, 
  Bell, 
  Settings,
  ChevronRight,
  Building2,
  Truck
} from 'lucide-react';

export const DashboardLayout: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const targetId = location.hash.slice(1);
    if (!targetId) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }

    const scrollToTarget = () => {
      document.getElementById(targetId)?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      });
    };

    const frame = window.requestAnimationFrame(scrollToTarget);
    return () => window.cancelAnimationFrame(frame);
  }, [location.pathname, location.hash]);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const role = user?.roles[0] || 'CUSTOMER';
  const isCustomerPreview = location.pathname.startsWith('/customer/dashboard-preview');
  const customerDashboardPath = isCustomerPreview ? '/customer/dashboard-preview' : '/customer/dashboard';

  const getNavigationItems = () => {
    switch (role) {
      case 'ADMIN':
        return [
          { name: 'Admin Overview', path: '/admin/dashboard', icon: LayoutDashboard },
          { name: 'Pending Approvals', path: '/admin/dashboard#merchants', icon: Clock },
          { name: 'User Management', path: '/admin/dashboard#users', icon: Users },
        ];
      case 'MERCHANT':
        return [
          { name: 'Merchant Portal', path: '/merchant/dashboard', icon: LayoutDashboard },
          { name: 'Parcel Orders', path: '/merchant/dashboard#orders', icon: Package },
          { name: 'Business Profile', path: '/merchant/dashboard#profile', icon: Building2 },
        ];
      case 'COURIER':
        return [
          { name: 'Courier Console', path: '/courier/dashboard', icon: LayoutDashboard },
          { name: 'Assigned Drops', path: '/courier/dashboard#deliveries', icon: Truck },
          { name: 'Active Route', path: '/courier/dashboard#route', icon: MapPin },
        ];
      case 'CUSTOMER':
      default:
        return [
          { name: 'Overview', path: `${customerDashboardPath}/overview`, icon: LayoutDashboard },
          { name: 'Send Parcel', path: `${customerDashboardPath}/send`, icon: Send },
          { name: 'Track Shipment', path: `${customerDashboardPath}/track`, icon: MapPin },
          { name: 'Delivery History', path: `${customerDashboardPath}/history`, icon: CheckCircle2 },
          { name: 'Profile & Settings', path: `${customerDashboardPath}/settings`, icon: Settings },
        ];
    }
  };

  const navItems = getNavigationItems();

  const getRoleBadgeColor = () => {
    switch (role) {
      case 'ADMIN': return 'bg-purple-100 text-purple-700 border-purple-200';
      case 'MERCHANT': return 'bg-blue-100 text-blue-700 border-blue-200';
      case 'COURIER': return 'bg-amber-100 text-amber-700 border-amber-200';
      default: return 'bg-yellow-400/20 text-yellow-700 border-yellow-500/30';
    }
  };

  return (
    <div className="soft-ui-shell min-h-screen flex flex-col md:flex-row">
      {/* Sidebar (Desktop) */}
      <aside className="soft-ui-sidebar w-full md:w-64 flex flex-col shrink-0">
        {/* Brand header */}
        <div className="h-16 flex items-center px-6 border-b border-slate-200/70">
          <Link to="/">
            <Logo size="md" />
          </Link>
        </div>

        {/* User Card */}
        <SoftCard className="p-4 mx-3 my-4 flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-navy-900 text-white font-bold text-sm flex items-center justify-center shrink-0">
            {user?.fullName.charAt(0)}
          </div>
          <div className="overflow-hidden">
            <h4 className="text-sm font-semibold text-navy-900 truncate">{user?.fullName}</h4>
            <div className="flex items-center gap-1.5 mt-0.5">
              <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${getRoleBadgeColor()}`}>
                {role}
              </span>
            </div>
          </div>
        </SoftCard>

        {/* Navigation items */}
        <nav className="flex-1 px-3 space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const [itemPath, itemHash] = item.path.split('#');
            const isActive = location.pathname === itemPath && (!itemHash || location.hash === `#${itemHash}`);
            return (
              <Link
                key={item.name}
                to={item.path}
                aria-current={isActive ? 'page' : undefined}
                className={`soft-ui-nav-item flex items-center justify-between px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-navy-900 text-white shadow-sm'
                    : 'text-slate-600 hover:text-navy-900 hover:bg-slate-50'
                }`}
              >
                <div className="flex items-center gap-3">
                  <Icon className={`w-4 h-4 ${isActive ? 'text-yellow-400' : 'text-slate-400'}`} />
                  <span>{item.name}</span>
                </div>
                {isActive && <ChevronRight className="w-3.5 h-3.5 text-yellow-400" />}
              </Link>
            );
          })}
        </nav>

        {/* Bottom logout action */}
        <div className="p-4 border-t border-slate-200">
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-2.5 px-3.5 py-2.5 rounded-lg text-sm font-medium text-red-600 hover:bg-red-50 transition-colors"
          >
            <LogOut className="w-4 h-4" />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Top Header bar */}
        <header className="soft-ui-header h-16 px-6 flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              SwiftDrop Logistics Platform
            </span>
            <h1 className="text-lg font-bold text-navy-900 capitalize">
              {role.toLowerCase()} Portal
            </h1>
          </div>

          <div className="flex items-center gap-4">
            <div className="hidden sm:flex items-center gap-2 text-xs text-slate-500 bg-slate-50 px-3 py-1.5 rounded-full border border-slate-200">
              <span className="w-2 h-2 rounded-full bg-yellow-500 animate-pulse"></span>
              <span>Identity Service Connected</span>
            </div>
          </div>
        </header>

        {/* Dynamic Outlet */}
        <main className="soft-ui-dashboard flex-1 p-6 lg:p-8 max-w-7xl w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
