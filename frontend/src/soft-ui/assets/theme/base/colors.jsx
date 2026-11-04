/**
=========================================================
* Soft UI Dashboard React - v3.1.0
=========================================================

* Product Page: https://www.creative-tim.com/product/soft-ui-dashboard-pro-react
* Copyright 2023 Creative Tim (https://www.creative-tim.com)

Coded by www.creative-tim.com

 =========================================================

* The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
*/

/**
 * The base colors for the Soft UI Dashboard React.
 * Customised for SwiftDrop:
 * - Brand Yellow (#FFCB05, #FFDB57) as energetic primary
 * - Logistics Blue (#2563EB, #11296B) for in-transit & necessary components
 * - Semantic colors (Green success, Red error, Amber warning) for standard status
 * - Middle-Gray (#EDEDED) surface for sleek, soothing dashboard backgrounds
 */

const brand = {
  navy: "#00274c",
  blue: "#11296b",
  yellow: "#ffcb05",
  yellowLight: "#ffdb57",
  surface: "#f8fafc",       // Very light grey similar to white (clean & airy)
  surfaceRaised: "#ffffff", // Pure white for cards to pop
  border: "#e2e8f0",
  text: "#1e293b",
  textMuted: "#64748b",
  focusRing: "#ffcb05",
};

const colors = {
  brand,

  background: {
    default: brand.surface,
  },

  text: {
    main: brand.textMuted,
    focus: brand.navy,
  },

  transparent: {
    main: "transparent",
  },

  white: {
    main: "#ffffff",
    focus: "#ffffff",
  },

  black: {
    light: "#141414",
    main: "#000000",
    focus: "#000000",
  },

  // ✦ Primary is now Vibrant Brand Yellow!
  primary: {
    main: brand.yellow,
    focus: "#e6b800",
  },

  secondary: {
    main: brand.blue,
    focus: brand.navy,
  },

  // ✦ Info is now Energetic Logistics Blue (used for in-transit & active status)
  info: {
    main: "#2563eb",
    focus: "#1d4ed8",
  },

  // ✦ Success is standard Green (delivered, active, verified)
  success: {
    main: "#16a34a",
    focus: "#15803d",
  },

  // ✦ Warning is standard Amber (awaiting, pending review)
  warning: {
    main: "#f59e0b",
    focus: "#d97706",
  },

  // ✦ Error is standard Red
  error: {
    main: "#dc2626",
    focus: "#b91c1c",
  },

  light: {
    main: "#e9ecef",
    focus: "#e9ecef",
  },

  dark: {
    main: brand.navy,
    focus: brand.blue,
  },

  grey: {
    100: "#f8f9fa",
    200: "#e9ecef",
    300: "#dee2e6",
    400: "#ced4da",
    500: "#adb5bd",
    600: "#6c757d",
    700: "#495057",
    800: "#343a40",
    900: "#212529",
  },

  gradients: {
    // ✦ Vibrant Golden Yellow Gradient for primary buttons, highlights & cards
    primary: {
      main: brand.yellow,
      state: brand.yellowLight,
    },

    secondary: {
      main: brand.blue,
      state: brand.navy,
    },

    // ✦ Vibrant Logistics Blue Gradient for in-transit, maps, route optimization
    info: {
      main: "#3b82f6",
      state: "#1d4ed8",
    },

    // ✦ Pure Green Gradient for success & delivered states
    success: {
      main: "#22c55e",
      state: "#15803d",
    },

    // ✦ Warm Amber Gradient for pending / warnings
    warning: {
      main: "#f59e0b",
      state: "#fbbf24",
    },

    // ✦ Red Gradient for errors / cancellations
    error: {
      main: "#ef4444",
      state: "#dc2626",
    },

    light: {
      main: "#ced4da",
      state: "#ebeff4",
    },

    dark: {
      main: brand.navy,
      state: brand.blue,
    },
  },

  socialMediaColors: {
    facebook: {
      main: "#3b5998",
      dark: "#344e86",
    },
    twitter: {
      main: "#55acee",
      dark: "#3ea1ec",
    },
    instagram: {
      main: "#125688",
      dark: "#0e456d",
    },
    linkedin: {
      main: "#0077b5",
      dark: "#00669c",
    },
    pinterest: {
      main: "#cc2127",
      dark: "#b21d22",
    },
    youtube: {
      main: "#e52d27",
      dark: "#d41f1a",
    },
    vimeo: {
      main: "#1ab7ea",
      dark: "#13a3d2",
    },
    slack: {
      main: "#3aaf85",
      dark: "#329874",
    },
    dribbble: {
      main: "#ea4c89",
      dark: "#e73177",
    },
    github: {
      main: "#24292e",
      dark: "#171a1d",
    },
    reddit: {
      main: "#ff4500",
      dark: "#e03d00",
    },
    tumblr: {
      main: "#35465c",
      dark: "#2a3749",
    },
  },

  alertColors: {
    primary: {
      main: brand.yellow,
      state: brand.yellowLight,
      border: brand.yellowLight,
    },

    secondary: {
      main: "#627594",
      state: "#8ca1cb",
      border: "#dadee6",
    },

    info: {
      main: "#2563eb",
      state: "#1d4ed8",
      border: "#93c5fd",
    },

    success: {
      main: "#16a34a",
      state: "#15803d",
      border: "#86efac",
    },

    warning: {
      main: "#f59e0b",
      state: "#d97706",
      border: "#fcd34d",
    },

    error: {
      main: "#dc2626",
      state: "#b91c1c",
      border: "#fca5a5",
    },

    light: {
      main: "#ced4da",
      state: "#d1dae6",
      border: "#f8f9fa",
    },

    dark: {
      main: brand.navy,
      state: brand.blue,
      border: brand.border,
    },
  },

  // ✦ Standard badge colors: Green for success, Amber for warning, Blue for info, Yellow for primary
  badgeColors: {
    primary: {
      background: "#fff9cc",
      text: brand.navy,
    },

    secondary: {
      background: brand.surface,
      text: brand.navy,
    },

    info: {
      background: "#dbeafe",
      text: "#1e40af",
    },

    success: {
      background: "#dcfce7",
      text: "#15803d",
    },

    warning: {
      background: "#fef3c7",
      text: "#b45309",
    },

    error: {
      background: "#fee2e2",
      text: "#b91c1c",
    },

    light: {
      background: "#ffffff",
      text: "#475569",
    },

    dark: {
      background: brand.navy,
      text: "#ffffff",
    },
  },

  inputColors: {
    borderColor: { main: brand.border, focus: brand.yellow },
    boxShadow: "rgba(0, 39, 76, 0.14)",
    error: "#fd5c70",
    success: "#66d432",
  },

  sliderColors: {
    thumb: { borderColor: "#d9d9d9" },
  },

  circleSliderColors: {
    background: "#d3d3d3",
  },

  tabs: {
    indicator: { boxShadow: "#ddd" },
  },
};

export default colors;
