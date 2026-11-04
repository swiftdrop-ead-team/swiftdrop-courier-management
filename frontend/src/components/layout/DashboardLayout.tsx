import React, { useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Sidebar, Menu, MenuItem, Logo } from '../ui/MuiSidebar';
import { 
  LayoutDashboard, 
  Package, 
  Send, 
  MapPin, 
  Users, 
  CheckCircle2, 
  Clock, 
  LogOut, 
  Settings,
  Building2,
  Truck
} from 'lucide-react';
import { Box } from '@mui/material';

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
      document.getElementById(targetId)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
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

  const getMenuSections = () => {
    switch (role) {
      case 'ADMIN':
        return [
          {
            heading: 'ADMINISTRATION',
            items: [
              { name: 'Admin Overview', path: '/admin/dashboard', icon: LayoutDashboard },
              { name: 'Pending Approvals', path: '/admin/dashboard#merchants', icon: Clock, badge: 'Live' },
              { name: 'User Management', path: '/admin/dashboard#users', icon: Users },
            ],
          },
        ];
      case 'MERCHANT':
        return [
          {
            heading: 'MERCHANT PORTAL',
            items: [
              { name: 'Merchant Portal', path: '/merchant/dashboard', icon: LayoutDashboard },
              { name: 'Parcel Orders', path: '/merchant/dashboard#orders', icon: Package },
              { name: 'Business Profile', path: '/merchant/dashboard#profile', icon: Building2 },
            ],
          },
        ];
      case 'COURIER':
        return [
          {
            heading: 'DISPATCH CONSOLE',
            items: [
              { name: 'Courier Console', path: '/courier/dashboard', icon: LayoutDashboard },
              { name: 'Assigned Drops', path: '/courier/dashboard#deliveries', icon: Truck },
              { name: 'Active Route', path: '/courier/dashboard#route', icon: MapPin },
            ],
          },
        ];
      case 'CUSTOMER':
      default:
        return [
          {
            heading: 'HOME',
            items: [
              { name: 'Overview', path: `${customerDashboardPath}/overview`, icon: LayoutDashboard },
            ],
          },
          {
            heading: 'DELIVERIES',
            items: [
              { name: 'Send Parcel', path: `${customerDashboardPath}/send`, icon: Send, badge: 'New' },
              { name: 'Track Shipment', path: `${customerDashboardPath}/track`, icon: MapPin },
              { name: 'Delivery History', path: `${customerDashboardPath}/history`, icon: CheckCircle2 },
            ],
          },
          {
            heading: 'ACCOUNT',
            items: [
              { name: 'Profile & Settings', path: `${customerDashboardPath}/settings`, icon: Settings },
            ],
          },
        ];
    }
  };

  const menuSections = getMenuSections();

  const getRoleBadgeColor = () => {
    switch (role) {
      case 'ADMIN':    return 'bg-purple-100 text-purple-800 border border-purple-200';
      case 'MERCHANT': return 'bg-blue-100   text-blue-800   border border-blue-200';
      case 'COURIER':  return 'bg-amber-100  text-amber-800  border border-amber-200';
      default:         return 'bg-blue-100 text-blue-900 border border-blue-200';
    }
  };

  return (
    <div className="soft-ui-shell min-h-screen flex flex-col md:flex-row bg-[#f8fafc]">

      {/* ── Material UI Sidebar Container (270px, fits the page flush) ── */}
      <Sidebar
        width="270px"
        sx={{
          m: 0,
          height: '100vh',
          borderRadius: 0,
          border: 0,
          borderRight: '1px solid rgba(255, 255, 255, 0.08)',
          boxShadow: 'none',
        }}
      >
        {/* Logo Component */}
        <Logo
          icon={<Package size={20} />}
          subtitle="Logistics Platform"
          link="/"
        >
          SwiftDrop
        </Logo>

        {/* User Card */}
        <Box sx={{ p: 1.5, mx: 2, my: 1.5, borderRadius: 2.5, bgcolor: 'rgba(255, 255, 255, 0.05)', border: '1px solid rgba(255, 255, 255, 0.1)', display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <div className="w-9 h-9 rounded-full bg-gradient-to-br from-blue-200 to-blue-400 text-navy-950 font-extrabold text-xs flex items-center justify-center shrink-0 shadow-md">
            {user?.fullName.charAt(0)}
          </div>
          <div className="overflow-hidden min-w-0">
            <h4 className="text-xs font-bold text-white truncate">{user?.fullName}</h4>
            <span className={`mt-0.5 inline-block text-[9px] font-bold px-1.5 py-0.2 rounded-full ${getRoleBadgeColor()}`}>
              {role}
            </span>
          </div>
        </Box>

        {/* Dynamic MUI Menu Sections */}
        <Box sx={{ flex: 1, py: 1 }}>
          {menuSections.map((section) => (
            <Menu key={section.heading} subHeading={section.heading}>
              {section.items.map((item) => {
                const Icon = item.icon;
                const [itemPath, itemHash] = item.path.split('#');
                const isActive =
                  location.pathname === itemPath &&
                  (!itemHash || location.hash === `#${itemHash}`);

                return (
                  <MenuItem
                    key={item.name}
                    link={item.path}
                    icon={<Icon size={18} />}
                    active={isActive}
                    badge={item.badge}
                    badgeColor="primary"
                  >
                    {item.name}
                  </MenuItem>
                );
              })}
            </Menu>
          ))}
        </Box>

        {/* Logout at bottom */}
        <Box sx={{ p: 2, mt: 'auto', borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 px-3.5 py-2.5 rounded-xl border border-red-300/30 bg-red-950/25 text-xs font-bold text-red-200 hover:bg-red-600/35 hover:border-red-200/50 transition-colors"
          >
            <LogOut className="w-4 h-4" />
            <span>Sign Out</span>
          </button>
        </Box>
      </Sidebar>

      {/* ── Main Content Area ── */}
      <div className="flex-1 flex flex-col min-w-0">

        {/* Top Header bar */}
        <header className="soft-ui-header h-16 px-6 flex items-center justify-between bg-white border-b border-slate-200">
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              SwiftDrop Logistics Platform
            </span>
            <h1 className="text-lg font-bold text-navy-900 capitalize">
              {role.toLowerCase()} Portal
            </h1>
          </div>

          <div className="flex items-center gap-4">
            <div className="hidden sm:flex items-center gap-2 text-xs bg-green-50 text-green-700 px-3 py-1.5 rounded-full border border-green-200 font-medium">
              <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
              <span>Identity Service Connected</span>
            </div>
            <div className="flex items-center gap-1.5 text-xs bg-blue-50 text-blue-900 px-3 py-1.5 rounded-full border border-blue-200 font-semibold">
              <span className="w-2 h-2 rounded-full bg-blue-500" />
              <span>SwiftDrop</span>
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
