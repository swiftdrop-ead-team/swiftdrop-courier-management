import React, { useEffect, useState } from "react";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ThemeProvider, type Theme } from "@mui/material/styles";
import {
  Alert,
  Avatar,
  Box,
  Breadcrumbs,
  Drawer,
  IconButton,
  ScopedCssBaseline,
  useMediaQuery,
} from "@mui/material";
import {
  Home,
  LogOut,
  Menu as MenuIcon,
  Package,
  Search,
  X,
} from "lucide-react";
import theme from "assets/theme";
import { SoftUIControllerProvider } from "context";
import SoftTypography from "components/SoftTypography";
import SoftButton from "components/SoftButton";
import SoftInput from "components/SoftInput";
import TemplateDashboardLayout from "examples/LayoutContainers/DashboardLayout";
import { Sidebar, Menu, MenuItem, Logo } from "../ui/MuiSidebar";
import { useAuth } from "../../hooks/useAuth";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import "@fontsource/roboto/700.css";
import "@fontsource/material-icons";
import "../../soft-ui/fonts.css";
import "chart.js/auto";

export interface NavItem {
  tab: string;
  label: string;
  icon: React.ElementType;
  path: string;
  badge?: string;
}
export interface NavSection { heading: string; items: NavItem[] }
interface RoleDashboardLayoutProps {
  role: string;
  basePath: string;
  sidebarSubtitle: string;
  navigation: NavSection[];
  getActiveItem: (location: ReturnType<typeof useLocation>, navigation: NavSection[]) => NavItem | undefined;
  profileLabel: string;
  breadcrumbRole: string;
  searchPlaceholder: string;
}

function RoleShell({ role, basePath: base, sidebarSubtitle, navigation, getActiveItem, profileLabel, breadcrumbRole, searchPlaceholder }: RoleDashboardLayoutProps) {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const desktop = useMediaQuery(theme.breakpoints.up("lg"));
  const [menuOpen, setMenuOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [logoutError, setLogoutError] = useState("");
  const preview = location.pathname.includes('dashboard-preview');
  const active = getActiveItem(location, navigation) || navigation[0]?.items[0];
  const name = preview ? (role === 'CUSTOMER' ? 'Alex Morgan' : `${breadcrumbRole} Preview`) : user?.fullName || breadcrumbRole;
  const shipmentRole = role === 'CUSTOMER' || role === 'MERCHANT';
  const createPath = `${base}/${role === 'CUSTOMER' ? 'send' : 'create-shipment'}`;

  useEffect(() => {
    setMenuOpen(false);
    window.scrollTo({ top: 0 });
    if (location.hash) {
      const frame = requestAnimationFrame(() => document.getElementById(location.hash.slice(1))?.scrollIntoView({ behavior: 'smooth' }));
      return () => cancelAnimationFrame(frame);
    }
  }, [location.pathname, location.hash]);

  const signOut = async () => {
    try {
      await logout();
      navigate("/login");
    } catch {
      setLogoutError("Could not sign out. Please try again.");
    }
  };

  return (
    <ScopedCssBaseline
      className="customer-dashboard-shell"
      sx={() => ({
        minHeight: "100vh",
        backgroundColor: "#f8fafc", // Very light grey similar to white
        color: "#00274c",
      })}
    >
      <Box
        component="a"
        href="#dashboard-main"
        sx={(theme: Theme) => ({
          position: "absolute",
          left: -10000,
          "&:focus": {
            left: 16,
            top: 8,
            zIndex: 1500,
            background: theme.palette.brand.surfaceRaised,
            p: 1,
          },
        })}
      >
        Skip to content
      </Box>

      {/* MUI Sidebar Drawer (Fits the page with 0 margins and 0 border radius) */}
      <Drawer
        variant={desktop ? "permanent" : "temporary"}
        open={desktop || menuOpen}
        onClose={() => setMenuOpen(false)}
        PaperProps={{
          id: "dashboard-navigation",
          sx: () => ({
            width: 260,
            m: "0 !important",
            height: "100vh !important",
            borderRadius: "0 !important",
            border: "0 !important",
            borderRight: "1px solid rgba(255, 255, 255, 0.06) !important",
            background: "linear-gradient(180deg, #001a35 0%, #00274c 55%, #08213f 100%) !important",
            boxShadow: "none !important",
            overflow: "hidden",
          }),
        }}
      >
        <Sidebar
          width="100%"
          sx={{
            height: "100%",
            borderRight: "none",
            boxShadow: "none",
            background: "transparent",
          }}
        >
          {/* Logo Component with White text & Yellow Icon */}
          <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", pr: 1.5 }}>
            <Logo
              icon={<Package size={20} />}
              subtitle={sidebarSubtitle}
              link="/"
            >
              SwiftDrop
            </Logo>
            {!desktop && (
              <IconButton
                aria-label="Close navigation"
                onClick={() => setMenuOpen(false)}
                size="small"
                sx={{ color: "#ffffff" }}
              >
                <X size={18} />
              </IconButton>
            )}
          </Box>

          {navigation.map((section) => <Menu key={section.heading} subHeading={section.heading}>
            {section.items.map((item) => {
              const Icon = item.icon;
              return <MenuItem key={item.tab} link={item.path} icon={<Icon size={18} />} active={active?.tab === item.tab} badge={item.badge} badgeColor="primary">{item.label}</MenuItem>;
            })}
          </Menu>)}

          {/* Account Profile snippet (Matched to Blue Sidebar) */}
          <Box sx={{ px: 1.5, mt: 1 }}>
            <Box
              sx={{
                p: 1.25,
                borderRadius: "10px",
                bgcolor: "rgba(255, 255, 255, 0.04)",
                border: "1px solid rgba(255, 255, 255, 0.08)",
                display: "flex",
                alignItems: "center",
                gap: 1,
                transition: "all 200ms ease",
                "&:hover": {
                  bgcolor: "rgba(255, 255, 255, 0.06)",
                  border: "1px solid rgba(255, 255, 255, 0.12)",
                },
              }}
            >
              <Avatar
                sx={{
                  background: "linear-gradient(135deg, #ffcb05 0%, #ffdb57 100%)",
                  border: "none",
                  width: 34,
                  height: 34,
                  color: "#00274c",
                  fontWeight: "bold",
                  fontSize: "0.8rem",
                }}
              >
                {name.charAt(0)}
              </Avatar>
              <Box sx={{ minWidth: 0 }}>
                <SoftTypography
                  variant="button"
                  fontWeight="bold"
                  display="block"
                  noWrap
                  sx={{ fontSize: "0.82rem", color: "#ffffff !important" }}
                >
                  {name}
                </SoftTypography>
                <SoftTypography
                  variant="caption"
                  sx={{ fontSize: "0.68rem", color: "rgba(148, 163, 184, 0.7) !important" }}
                >
                  {preview ? "Preview account" : profileLabel}
                </SoftTypography>
              </Box>
            </Box>
          </Box>

          {/* Bottom Card & Action */}
          <Box sx={{ mt: "auto", p: 1.5 }}>
            <Box
              sx={{
                p: 1.75,
                borderRadius: "10px",
                background: "linear-gradient(135deg, rgba(255, 203, 5, 0.1) 0%, rgba(255, 219, 87, 0.03) 100%)",
                border: "1px solid rgba(255, 203, 5, 0.2)",
                mb: 1.5,
              }}
            >
              <SoftTypography
                variant="button"
                fontWeight="bold"
                display="block"
                sx={{ color: "#ffcb05 !important", fontSize: "0.82rem" }}
              >
                {shipmentRole ? "Ready to send?" : "Your workspace"}
              </SoftTypography>
              <SoftTypography
                variant="caption"
                sx={{ display: "block", mt: 0.25, color: "rgba(203, 213, 225, 0.7) !important", fontSize: "0.7rem" }}
              >
                {shipmentRole ? "A little package. A big connection." : "Everything you need, in one place."}
              </SoftTypography>
              <SoftButton
                fullWidth
                variant="gradient"
                color="primary"
                size="small"
                onClick={() => navigate(shipmentRole ? createPath : base)}
                sx={{
                  mt: 1.25,
                  py: 0.65,
                  fontSize: "0.75rem",
                  borderRadius: "8px",
                  color: "#00274c !important",
                  fontWeight: "bold",
                  background: "linear-gradient(135deg, #ffcb05 0%, #ffdb57 100%)",
                  boxShadow: "0 2px 8px rgba(255, 203, 5, 0.2)",
                  "&:hover": {
                    background: "linear-gradient(135deg, #ffdb57 0%, #ffe866 100%)",
                    boxShadow: "0 4px 12px rgba(255, 203, 5, 0.3)",
                  },
                }}
              >
                {shipmentRole ? "Create a shipment" : "View overview"}
              </SoftButton>
            </Box>

            {preview ? (
              <SoftButton
                component={Link}
                to="/login"
                fullWidth
                variant="gradient"
                color="primary"
                sx={{
                  color: "#00274c !important",
                  fontWeight: "bold",
                  background: "linear-gradient(135deg, #ffcb05 0%, #ffdb57 100%)",
                  boxShadow: "0 5px 14px rgba(0, 39, 76, 0.16)",
                  "&:hover": {
                    background: "linear-gradient(135deg, #ffdb57 0%, #ffe866 100%)",
                  },
                }}
              >
                Sign in to SwiftDrop
              </SoftButton>
            ) : (
              <SoftButton
                fullWidth
                variant="text"
                onClick={signOut}
                sx={{
                  color: "#fecaca !important",
                  fontWeight: "bold",
                  border: "1px solid rgba(248, 113, 113, 0.35)",
                  backgroundColor: "rgba(127, 29, 29, 0.28)",
                  "&:hover": {
                    backgroundColor: "rgba(220, 38, 38, 0.38)",
                    borderColor: "rgba(252, 165, 165, 0.55)",
                  },
                }}
              >
                <LogOut size={16} style={{ marginRight: 8 }} />
                Sign out
              </SoftButton>
            )}
            {logoutError && <Alert severity="error">{logoutError}</Alert>}
          </Box>
        </Sidebar>
      </Drawer>

      <TemplateDashboardLayout>
        <Box
          component="header"
          sx={() => ({
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: 2,
            flexWrap: "wrap",
            px: { xs: 1, md: 2 },
            pt: 2,
            pb: 2,
            borderBottom: "1px solid rgba(0, 39, 76, 0.06)",
            bgcolor: "rgba(255, 255, 255, 0.8)",
            backdropFilter: "blur(8px)",
          })}
        >
          <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
            {!desktop && (
              <IconButton
                aria-label="Open navigation"
                aria-controls="dashboard-navigation"
                aria-expanded={menuOpen}
                onClick={() => setMenuOpen(true)}
                sx={{
                  borderRadius: "8px",
                  bgcolor: "rgba(0, 39, 76, 0.04)",
                  "&:hover": { bgcolor: "rgba(0, 39, 76, 0.08)" },
                }}
              >
                <MenuIcon size={20} />
              </IconButton>
            )}
            <Box>
              <Breadcrumbs
                aria-label="breadcrumb"
                sx={{
                  fontSize: "0.75rem",
                  mb: 0.25,
                  color: "text.secondary",
                  "& .MuiBreadcrumbs-separator": { mx: 0.75 },
                }}
              >
                <Link
                  to="/"
                  aria-label="SwiftDrop home"
                  style={{ textDecoration: "none", display: "inline-flex", alignItems: "center", gap: 0.3 }}
                >
                  <Home size={13} />
                  <span>Home</span>
                </Link>
                <span>{breadcrumbRole}</span>
                <Box component="span" sx={{ color: "text.primary", fontWeight: 600 }}>{active?.label}</Box>
              </Breadcrumbs>
              <SoftTypography
                variant="h6"
                fontWeight="bold"
                sx={{ fontSize: "1.15rem", letterSpacing: "-0.02em", lineHeight: 1.3 }}
              >
                {active?.label}
              </SoftTypography>
            </Box>
          </Box>

          {(role === "CUSTOMER" || role === "MERCHANT") && <Box
            component="form"
            onSubmit={(event: React.FormEvent) => {
              event.preventDefault();
              navigate(
                `${base}/${role === "MERCHANT" ? "shipments" : "history"}?q=${encodeURIComponent(search.trim())}`,
              );
            }}
            sx={{
              display: "flex",
              gap: 0.5,
              width: { xs: "100%", sm: "auto" },
              maxWidth: 360,
            }}
          >
            <SoftInput
              placeholder={searchPlaceholder}
              value={search}
              onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                setSearch(event.target.value)
              }
              inputProps={{ "aria-label": "Search shipments" }}
              sx={{
                "& .MuiInputBase-root": {
                  borderRadius: "8px",
                  bgcolor: "rgba(0, 39, 76, 0.03)",
                  border: "1px solid rgba(0, 39, 76, 0.1)",
                  "&:hover": { borderColor: "rgba(0, 39, 76, 0.2)" },
                },
              }}
            />
            <IconButton
              type="submit"
              aria-label="Search shipments"
              sx={{
                borderRadius: "8px",
                bgcolor: "rgba(0, 39, 76, 0.06)",
                "&:hover": { bgcolor: "rgba(0, 39, 76, 0.12)" },
              }}
            >
              <Search size={18} />
            </IconButton>
          </Box>}
        </Box>
        <Box component="main" id="dashboard-main" sx={{ minWidth: 0 }}>
          <Outlet key={preview ? "preview" : user?.id || "customer"} />
        </Box>
        <Box
          component="footer"
          sx={{
            display: "flex",
            justifyContent: "space-between",
            flexWrap: "wrap",
            gap: 1,
            py: 2,
            px: { xs: 1, md: 2 },
            borderTop: "1px solid rgba(0, 39, 76, 0.06)",
            mt: 2,
          }}
        >
          <SoftTypography variant="caption" color="text" sx={{ fontSize: "0.72rem" }}>
            SwiftDrop · A simpler way to send.
          </SoftTypography>
          <SoftTypography variant="caption" color="text" sx={{ fontSize: "0.72rem" }}>
            SwiftDrop Logistics
          </SoftTypography>
        </Box>
      </TemplateDashboardLayout>
    </ScopedCssBaseline>
  );
}

export const RoleDashboardLayout = (props: RoleDashboardLayoutProps) => (
  <ThemeProvider theme={theme}>
    <SoftUIControllerProvider>
      <RoleShell {...props} />
    </SoftUIControllerProvider>
  </ThemeProvider>
);
