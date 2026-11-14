import React, { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  Box,
  Collapse,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Chip,
} from "@mui/material";
import { ChevronDown, ChevronRight } from "lucide-react";

/* ================================================================
   1. Sidebar Container (Fits the page with Blue Background)
   ================================================================ */
interface SidebarProps {
  width?: string | number;
  children: React.ReactNode;
  className?: string;
  sx?: object;
}

export const Sidebar: React.FC<SidebarProps> = ({
  width = "270px",
  children,
  className = "",
  sx = {},
}) => {
  return (
    <Box
      component="aside"
      className={`mui-sidebar ${className}`}
      sx={{
        width,
        minWidth: width,
        maxWidth: width,
        height: "100%",
        display: "flex",
        flexDirection: "column",
        /* Deep Blue Brand Background (#00274C / #001a35) */
        background: "linear-gradient(180deg, #001a35 0%, #00274c 55%, #08213f 100%)",
        borderRight: "1px solid rgba(255, 255, 255, 0.08)",
        color: "#ffffff",
        overflowY: "auto",
        overflowX: "hidden",
        "&::-webkit-scrollbar": { display: "none" },
        msOverflowStyle: "none" as string,
        scrollbarWidth: "none" as string,
        ...sx,
      }}
    >
      {children}
    </Box>
  );
};

/* ================================================================
  2. Logo Component (Clean White Typography + Blue Icon)
   ================================================================ */
interface LogoProps {
  img?: string;
  icon?: React.ReactNode;
  children?: React.ReactNode;
  subtitle?: string;
  link?: string;
}

export const Logo: React.FC<LogoProps> = ({
  img,
  icon,
  children,
  subtitle,
  link = "/",
}) => {
  const content = (
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        gap: 1.5,
        px: 3,
        py: 2.75,
        borderBottom: "1px solid rgba(255, 255, 255, 0.08)",
        textDecoration: "none",
        color: "inherit",
      }}
    >
      {img ? (
        <Box
          component="img"
          src={img}
          alt="Logo"
          sx={{ width: 36, height: 36, objectFit: "contain", borderRadius: 1.5 }}
        />
      ) : icon ? (
        <Box
          sx={{
            width: 36,
            height: 36,
            borderRadius: 2,
            background: "linear-gradient(135deg, #ffcb05 0%, #ffdb57 100%)",
            color: "#00274c",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            boxShadow: "0 3px 10px rgba(0, 39, 76, 0.18)",
            flexShrink: 0,
          }}
        >
          {icon}
        </Box>
      ) : null}

      <Box sx={{ minWidth: 0 }}>
        {typeof children === "string" ? (
          <Typography
            variant="h6"
            sx={{
              fontWeight: 800,
              fontSize: "1.1rem",
              lineHeight: 1.2,
              color: "#ffffff",
              letterSpacing: "-0.01em",
            }}
          >
            {children}
          </Typography>
        ) : (
          children
        )}
        {subtitle && (
          <Typography
            variant="caption"
            sx={{
              display: "block",
              fontSize: "0.72rem",
              color: "#94a3b8",
              fontWeight: 500,
              mt: 0.25,
            }}
          >
            {subtitle}
          </Typography>
        )}
      </Box>
    </Box>
  );

  return link ? (
    <Link to={link} style={{ textDecoration: "none", color: "inherit" }}>
      {content}
    </Link>
  ) : (
    content
  );
};

/* ================================================================
   3. Menu Group with subHeading (Soft Blue Subheading)
   ================================================================ */
interface MenuProps {
  subHeading?: string;
  children: React.ReactNode;
}

export const Menu: React.FC<MenuProps> = ({ subHeading, children }) => {
  return (
    <Box sx={{ px: 1.5, pt: 1.5, pb: 0.5 }}>
      {subHeading && (
        <Typography
          variant="overline"
          sx={{
            display: "block",
            px: 1,
            pb: 0.5,
            pt: 0.5,
            fontSize: "0.65rem",
            fontWeight: 700,
            letterSpacing: "0.1em",
            color: "rgba(138, 182, 226, 0.7)",
            textTransform: "uppercase",
          }}
        >
          {subHeading}
        </Typography>
      )}
      <List disablePadding sx={{ display: "flex", flexDirection: "column", gap: 0.25 }}>
        {children}
      </List>
    </Box>
  );
};

/* ================================================================
  4. MenuItem Component (Pill-Style: Blue on Active, Light Slate on Inactive)
   ================================================================ */
interface MenuItemProps {
  link?: string;
  icon?: React.ReactNode;
  children: React.ReactNode;
  active?: boolean;
  badge?: string | boolean | number;
  badgeColor?: "primary" | "secondary" | "success" | "warning" | "error" | "info";
  onClick?: () => void;
}

export const MenuItem: React.FC<MenuItemProps> = ({
  link,
  icon,
  children,
  active,
  badge,
  badgeColor = "primary",
  onClick,
}) => {
  const location = useLocation();
  const isCurrentActive =
    active !== undefined ? active : link ? location.pathname === link : false;

  const itemContent = (
    <ListItem disablePadding sx={{ display: "block", px: 0.5 }}>
      <ListItemButton
        component={link ? Link : "button"}
        to={link}
        aria-current={isCurrentActive && link ? "page" : undefined}
        onClick={onClick}
        selected={isCurrentActive}
        sx={{
          minHeight: 40,
          borderRadius: "8px",
          px: 1.25,
          py: 0.75,
          transition: "all 200ms cubic-bezier(0.4, 0, 0.2, 1)",
          alignItems: "center",
          color: isCurrentActive ? "#00274c" : "rgba(226, 232, 240, 0.85)",
          fontWeight: isCurrentActive ? 700 : 400,
          background: isCurrentActive
            ? "linear-gradient(135deg, #ffcb05 0%, #ffdb57 100%)"
            : "transparent",
          boxShadow: isCurrentActive
            ? "0 2px 8px rgba(255, 203, 5, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.2)"
            : "none",
          border: isCurrentActive ? "none" : "1px solid transparent",
          "&:hover": {
            backgroundColor: isCurrentActive
              ? "transparent"
              : "rgba(255, 255, 255, 0.06)",
            color: isCurrentActive ? "#00274c" : "#ffffff",
            border: isCurrentActive ? "none" : "1px solid rgba(255, 255, 255, 0.08)",
          },
          "&:active": {
            transform: "scale(0.98)",
          },
        }}
      >
        {icon && (
          <ListItemIcon
            sx={{
              minWidth: 0,
              mr: 1.5,
              width: 24,
              color: isCurrentActive ? "#00274c" : "rgba(147, 197, 253, 0.7)",
              display: "flex",
              alignItems: "center",
              transition: "color 200ms ease",
            }}
          >
            {icon}
          </ListItemIcon>
        )}
        <ListItemText
          primary={children}
          primaryTypographyProps={{
            fontSize: "0.82rem",
            fontWeight: isCurrentActive ? 700 : 400,
            lineHeight: 1.3,
            letterSpacing: "0.01em",
          }}
        />
        {badge && (
          <Chip
            size="small"
            label={badge === true ? "New" : badge}
            sx={{
              height: 18,
              fontSize: "0.62rem",
              fontWeight: 700,
              ml: 0.75,
              borderRadius: "4px",
              backgroundColor: isCurrentActive ? "rgba(0, 39, 76, 0.15)" : "rgba(219, 234, 254, 0.15)",
              color: isCurrentActive ? "#00274c" : "#93c5fd",
              border: isCurrentActive ? "none" : "1px solid rgba(147, 197, 253, 0.2)",
              boxShadow: "none",
            }}
          />
        )}
      </ListItemButton>
    </ListItem>
  );

  return itemContent;
};

/* ================================================================
   5. Submenu Component (Collapsible Menu Items)
   ================================================================ */
interface SubmenuProps {
  subHeading: string;
  icon?: React.ReactNode;
  children: React.ReactNode;
  defaultOpen?: boolean;
}

export const Submenu: React.FC<SubmenuProps> = ({
  subHeading,
  icon,
  children,
  defaultOpen = false,
}) => {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <Box sx={{ mb: 0.5 }}>
      <ListItemButton
        onClick={() => setOpen(!open)}
        sx={{
          minHeight: 40,
          borderRadius: "10px",
          px: 1.75,
          py: 0.75,
          color: "#e2e8f0",
          "&:hover": {
            backgroundColor: "rgba(255, 255, 255, 0.08)",
            color: "#ffffff",
          },
        }}
      >
        {icon && (
          <ListItemIcon sx={{ minWidth: 32, color: "#93c5fd" }}>
            {icon}
          </ListItemIcon>
        )}
        <ListItemText
          primary={subHeading}
          primaryTypographyProps={{
            fontSize: "0.85rem",
            fontWeight: 600,
          }}
        />
        {open ? (
          <ChevronDown size={16} color="#8ab6e2" />
        ) : (
          <ChevronRight size={16} color="#8ab6e2" />
        )}
      </ListItemButton>
      <Collapse in={open} timeout="auto" unmountOnExit>
        <List disablePadding sx={{ pl: 2, pt: 0.5, display: "flex", flexDirection: "column", gap: 0.25 }}>
          {children}
        </List>
      </Collapse>
    </Box>
  );
};

export default Sidebar;
