import React from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Logo } from '../ui/Logo';
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
  ShieldCheck,
  Building2,
  Truck
} from 'lucide-react';

export const DashboardLayout: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const role = user?.roles[0] || 'CUSTOMER';

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
          { name: 'My Deliveries', path: '/customer/dashboard', icon: LayoutDashboard },
          { name: 'Send Parcel', path: '/customer/dashboard#book', icon: Send },
          { name: 'Live Tracking', path: '/customer/dashboard#track', icon: MapPin },
        ];
    }
  };

  const navItems = getNavigationItems();

  const getRoleBadgeColor = () => {
    switch (role) {
      case 'ADMIN': return 'bg-purple-100 text-purple-700 border-purple-200';
      case 'MERCHANT': return 'bg-blue-100 text-blue-700 border-blue-200';
      case 'COURIER': return 'bg-amber-100 text-amber-700 border-amber-200';
      default: return 'bg-teal-100 text-teal-700 border-teal-200';
    }
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col md:flex-row">
      {/* Sidebar (Desktop) */}
      <aside className="w-full md:w-64 bg-white border-r border-slate-200 flex flex-col shrink-0">
        {/* Brand header */}
        <div className="h-16 flex items-center px-6 border-b border-slate-200">
          <Link to="/">
            <Logo size="md" />
          </Link>
        </div>

        {/* User Card */}
        <div className="p-4 mx-3 my-4 rounded-xl bg-slate-50 border border-slate-100 flex items-center gap-3">
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
        </div>

        {/* Navigation items */}
        <nav className="flex-1 px-3 space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.name}
                to={item.path}
                className={`flex items-center justify-between px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-navy-900 text-white shadow-sm'
                    : 'text-slate-600 hover:text-navy-900 hover:bg-slate-50'
                }`}
              >
                <div className="flex items-center gap-3">
                  <Icon className={`w-4 h-4 ${isActive ? 'text-teal-400' : 'text-slate-400'}`} />
                  <span>{item.name}</span>
                </div>
                {isActive && <ChevronRight className="w-3.5 h-3.5 text-teal-400" />}
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
        <header className="h-16 bg-white border-b border-slate-200 px-6 flex items-center justify-between">
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
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              <span>Identity Service Connected</span>
            </div>
          </div>
        </header>

        {/* Dynamic Outlet */}
        <main className="flex-1 p-6 lg:p-8 max-w-7xl w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
