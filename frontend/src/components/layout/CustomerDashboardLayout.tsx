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
  LayoutDashboard,
  LogOut,
  MapPin,
  Menu as MenuIcon,
  Package,
  Search,
  Send,
  Settings,
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

const navigation = [
  { tab: "overview", label: "Dashboard", icon: LayoutDashboard },
  { tab: "send", label: "Send a parcel", icon: Send },
  { tab: "track", label: "Track shipment", icon: MapPin },
  { tab: "history", label: "Shipment history", icon: Package },
  { tab: "settings", label: "Profile & addresses", icon: Settings },
];

function CustomerShell() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const desktop = useMediaQuery(theme.breakpoints.up("lg"));
  const [menuOpen, setMenuOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [logoutError, setLogoutError] = useState("");
  const preview = location.pathname.startsWith("/customer/dashboard-preview");
  const base = preview ? "/customer/dashboard-preview" : "/customer/dashboard";
  const active =
    navigation.find(({ tab }) => location.pathname === `${base}/${tab}`) ||
    navigation[0];
  const name = preview ? "Alex Morgan" : user?.fullName || "Customer";

  useEffect(() => {
    setMenuOpen(false);
    window.scrollTo({ top: 0 });
  }, [location.pathname]);

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
        href="#customer-main"
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
          id: "customer-navigation",
          sx: () => ({
            width: 270,
            m: "0 !important", // Fits page without margins
            height: "100vh !important",
            borderRadius: "0 !important", // No card look, flush with page edge
            border: "0 !important",
            borderRight: "1px solid rgba(255, 255, 255, 0.08) !important",
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
              subtitle="Customer workspace"
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

          {/* MUI Menu Section: HOME */}
          <Menu subHeading="HOME">
            <MenuItem
              link={`${base}/overview`}
              icon={<LayoutDashboard size={18} />}
              active={active.tab === "overview"}
            >
              Dashboard
            </MenuItem>
          </Menu>

          {/* MUI Menu Section: DELIVERIES */}
          <Menu subHeading="DELIVERIES">
            <MenuItem
              link={`${base}/send`}
              icon={<Send size={18} />}
              active={active.tab === "send"}
              badge="New"
              badgeColor="primary"
            >
              Send a parcel
            </MenuItem>
            <MenuItem
              link={`${base}/track`}
              icon={<MapPin size={18} />}
              active={active.tab === "track"}
            >
              Track shipment
            </MenuItem>
            <MenuItem
              link={`${base}/history`}
              icon={<Package size={18} />}
              active={active.tab === "history"}
            >
              Shipment history
            </MenuItem>
          </Menu>

          {/* MUI Menu Section: ACCOUNT */}
          <Menu subHeading="ACCOUNT">
            <MenuItem
              link={`${base}/settings`}
              icon={<Settings size={18} />}
              active={active.tab === "settings"}
            >
              Profile & addresses
            </MenuItem>
          </Menu>

          {/* Account Profile snippet (Matched to Blue Sidebar) */}
          <Box sx={{ px: 2, mt: 2 }}>
            <Box
              sx={{
                p: 1.5,
                borderRadius: 2.5,
                bgcolor: "rgba(255, 255, 255, 0.05)",
                border: "1px solid rgba(255, 255, 255, 0.1)",
                display: "flex",
                alignItems: "center",
                gap: 1.5,
              }}
            >
              <Avatar
                sx={{
                  background: "#001a35",
                  border: "2px solid #ffcb05",
                  width: 36,
                  height: 36,
                  color: "#ffcb05",
                  fontWeight: "bold",
                  fontSize: "0.85rem",
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
                  sx={{ fontSize: "0.85rem", color: "#ffffff !important" }}
                >
                  {name}
                </SoftTypography>
                <SoftTypography
                  variant="caption"
                  sx={{ fontSize: "0.72rem", color: "#94a3b8 !important" }}
                >
                  {preview ? "Preview account" : "Personal account"}
                </SoftTypography>
              </Box>
            </Box>
          </Box>

          {/* Bottom Card & Action */}
          <Box sx={{ mt: "auto", p: 2 }}>
            <Box
              sx={{
                p: 2,
                borderRadius: 2.5,
                background: "linear-gradient(135deg, rgba(255, 203, 5, 0.14) 0%, rgba(255, 219, 87, 0.04) 100%)",
                border: "1px solid rgba(255, 203, 5, 0.35)",
                mb: 2,
              }}
            >
              <SoftTypography
                variant="button"
                fontWeight="bold"
                display="block"
                sx={{ color: "#ffcb05 !important" }}
              >
                Ready to send?
              </SoftTypography>
              <SoftTypography
                variant="caption"
                sx={{ display: "block", mt: 0.25, color: "#cbd5e1 !important" }}
              >
                A little package. A big connection.
              </SoftTypography>
              <SoftButton
                fullWidth
                variant="gradient"
                color="primary"
                size="small"
                onClick={() => navigate(`${base}/send`)}
                sx={{
                  mt: 1.5,
                  py: 0.75,
                  fontSize: "0.75rem",
                  color: "#00274c !important",
                  fontWeight: "bold",
                  background: "linear-gradient(135deg, #ffcb05 0%, #ffdb57 100%)",
                  boxShadow: "0 3px 10px rgba(0, 39, 76, 0.14)",
                  "&:hover": {
                    background: "linear-gradient(135deg, #ffdb57 0%, #ffe866 100%)",
                  },
                }}
              >
                Create a shipment
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
            px: 1,
            pb: 3,
            borderBottom: "1px solid rgba(0, 39, 76, 0.08)",
          })}
        >
          <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
            {!desktop && (
              <IconButton
                aria-label="Open navigation"
                aria-controls="customer-navigation"
                aria-expanded={menuOpen}
                onClick={() => setMenuOpen(true)}
              >
                <MenuIcon size={21} />
              </IconButton>
            )}
            <Box>
              <Breadcrumbs sx={{ fontSize: 13, mb: 0.5 }}>
                <Link to="/" aria-label="SwiftDrop home">
                  <Home size={14} />
                </Link>
                <span>Customer</span>
                <span>{active.label}</span>
              </Breadcrumbs>
              <SoftTypography variant="h6" fontWeight="bold">
                {active.label}
              </SoftTypography>
            </Box>
          </Box>
          <Box
            component="form"
            onSubmit={(event: React.FormEvent) => {
              event.preventDefault();
              navigate(
                `${base}/history?q=${encodeURIComponent(search.trim())}`,
              );
            }}
            sx={{ display: "flex", gap: 1, width: { xs: "100%", sm: "auto" } }}
          >
            <SoftInput
              placeholder="Search shipments..."
              value={search}
              onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                setSearch(event.target.value)
              }
              inputProps={{ "aria-label": "Search shipments" }}
            />
            <IconButton type="submit" aria-label="Search shipment history">
              <Search size={19} />
            </IconButton>
          </Box>
        </Box>
        <Box component="main" id="customer-main" sx={{ minWidth: 0 }}>
          <Outlet key={preview ? "preview" : user?.id || "customer"} />
        </Box>
        <Box
          component="footer"
          sx={{
            display: "flex",
            justifyContent: "space-between",
            flexWrap: "wrap",
            gap: 1,
            py: 3,
            px: 1,
          }}
        >
          <SoftTypography variant="caption" color="text">
            SwiftDrop · A simpler way to send.
          </SoftTypography>
          <SoftTypography variant="caption" color="text">
            Material UI React Dashboard
          </SoftTypography>
        </Box>
      </TemplateDashboardLayout>
    </ScopedCssBaseline>
  );
}

export const CustomerDashboardLayout = () => (
  <ThemeProvider theme={theme}>
    <SoftUIControllerProvider>
      <CustomerShell />
    </SoftUIControllerProvider>
  </ThemeProvider>
);
