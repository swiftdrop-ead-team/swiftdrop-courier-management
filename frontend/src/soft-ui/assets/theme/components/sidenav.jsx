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

// Soft UI Dashboard React base styles
import colors from "assets/theme/base/colors";
import borders from "assets/theme/base/borders";

// Soft UI Dashboard React helper functions
import rgba from "assets/theme/functions/rgba";
import pxToRem from "assets/theme/functions/pxToRem";

const { white } = colors;
const { borderRadius } = borders;

const sidenav = {
  styleOverrides: {
    root: {
      width: pxToRem(270),
      whiteSpace: "nowrap",
      border: "none",
    },

    paper: {
      width: pxToRem(270),
      backgroundColor: "#001a35",
      height: "100vh",
      margin: 0,
      borderRadius: 0,
      border: "none",
      borderRight: "1px solid rgba(255, 255, 255, 0.08)",
      boxShadow: "none",
    },

    paperAnchorDockedLeft: {
      borderRight: "1px solid rgba(255, 255, 255, 0.08)",
    },
  },
};

export default sidenav;
