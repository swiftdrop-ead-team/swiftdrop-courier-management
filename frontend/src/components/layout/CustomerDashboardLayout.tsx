import React, { useEffect, useState } from "react";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ThemeProvider } from "@mui/material/styles";
import {
  Alert,
  Avatar,
  Box,
  Breadcrumbs,
  Divider,
  Drawer,
  IconButton,
  List,
  ScopedCssBaseline,
  useMediaQuery,
} from "@mui/material";
import {
  Home,
  LayoutDashboard,
  LogOut,
  MapPin,
  Menu,
  Package,
  Search,
  Send,
  Settings,
  X,
} from "lucide-react";
import theme from "assets/theme";
import { SoftUIControllerProvider } from "context";
import SoftBox from "components/SoftBox";
import SoftTypography from "components/SoftTypography";
import SoftButton from "components/SoftButton";
import SoftInput from "components/SoftInput";
import SidenavCollapse from "examples/Sidenav/SidenavCollapse";
import TemplateDashboardLayout from "examples/LayoutContainers/DashboardLayout";
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
  const desktop = useMediaQuery(theme.breakpoints.up("xl"));
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
      sx={{ minHeight: "100vh", backgroundColor: "#f8f9fa", color: "#344767" }}
    >
      <Box
        component="a"
        href="#customer-main"
        sx={{
          position: "absolute",
          left: -10000,
          "&:focus": {
            left: 16,
            top: 8,
            zIndex: 1500,
            background: "#fff",
            p: 1,
          },
        }}
      >
        Skip to content
      </Box>
      <Drawer
        variant={desktop ? "permanent" : "temporary"}
        open={desktop || menuOpen}
        onClose={() => setMenuOpen(false)}
        PaperProps={{
          id: "customer-navigation",
          sx: {
            width: 250,
            m: desktop ? 2 : 0,
            height: desktop ? "calc(100% - 32px)" : "100%",
            border: 0,
            borderRadius: desktop ? 4 : 0,
            background: desktop ? "transparent" : "#fff",
            boxShadow: "none",
          },
        }}
      >
        <Box
          sx={{ px: 3, py: 3, display: "flex", alignItems: "center", gap: 1.5 }}
        >
          <SoftBox
            variant="gradient"
            bgColor="info"
            borderRadius="lg"
            color="white"
            p={1}
            display="flex"
          >
            <Package size={23} />
          </SoftBox>
          <Box component={Link} to="/" sx={{ textDecoration: "none" }}>
            <SoftTypography variant="h6" fontWeight="bold">
              SwiftDrop
            </SoftTypography>
            <SoftTypography variant="caption" color="text">
              Customer workspace
            </SoftTypography>
          </Box>
          {!desktop && (
            <IconButton
              aria-label="Close navigation"
              onClick={() => setMenuOpen(false)}
              sx={{ ml: "auto" }}
            >
              <X size={18} />
            </IconButton>
          )}
        </Box>
        <Divider />
        <Box component="nav" aria-label="Customer navigation">
          <List sx={{ py: 1 }}>
            {navigation.map(({ tab, label, icon: Icon }) => (
              <SidenavCollapse
                key={tab}
                component={Link}
                to={`${base}/${tab}`}
                name={label}
                icon={<Icon size={17} />}
                color="info"
                active={active.tab === tab}
                aria-current={active.tab === tab ? "page" : undefined}
              />
            ))}
          </List>
        </Box>
        <Box sx={{ px: 3, mt: 3 }}>
          <SoftTypography variant="caption" fontWeight="bold" color="text">
            YOUR ACCOUNT
          </SoftTypography>
          <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mt: 2 }}>
            <Avatar
              sx={{
                background: "linear-gradient(310deg, #141727, #3a416f)",
                width: 38,
                height: 38,
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
              >
                {name}
              </SoftTypography>
              <SoftTypography variant="caption" color="text">
                {preview ? "Preview account" : "Personal account"}
              </SoftTypography>
            </Box>
          </Box>
        </Box>
        <Box sx={{ mt: "auto", p: 2 }}>
          <SoftBox
            variant="gradient"
            bgColor="dark"
            borderRadius="lg"
            p={2.5}
            mb={2}
          >
            <SoftTypography variant="h6" color="white" fontWeight="bold">
              Ready to send?
            </SoftTypography>
            <SoftTypography variant="caption" color="white" opacity={0.8}>
              A little package. A big connection.
            </SoftTypography>
            <SoftButton
              fullWidth
              color="white"
              size="small"
              onClick={() => navigate(`${base}/send`)}
              sx={{ mt: 2 }}
            >
              Create a shipment
            </SoftButton>
          </SoftBox>
          {preview ? (
            <SoftButton
              component={Link}
              to="/login"
              fullWidth
              variant="gradient"
              color="info"
            >
              Sign in to SwiftDrop
            </SoftButton>
          ) : (
            <SoftButton fullWidth variant="text" color="dark" onClick={signOut}>
              <LogOut size={16} style={{ marginRight: 8 }} />
              Sign out
            </SoftButton>
          )}
          {logoutError && <Alert severity="error">{logoutError}</Alert>}
        </Box>
      </Drawer>
      <TemplateDashboardLayout>
        <Box
          component="header"
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: 2,
            flexWrap: "wrap",
            px: 1,
            pb: 3,
          }}
        >
          <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
            {!desktop && (
              <IconButton
                aria-label="Open navigation"
                aria-controls="customer-navigation"
                aria-expanded={menuOpen}
                onClick={() => setMenuOpen(true)}
              >
                <Menu size={21} />
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
            Design system: Soft UI Dashboard by Creative Tim
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
