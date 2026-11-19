/**
=========================================================
* Soft UI Dashboard React - v4.0.1
=========================================================

* Product Page: https://www.creative-tim.com/product/soft-ui-dashboard-react
* Copyright 2023 Creative Tim (https://www.creative-tim.com)

Coded by www.creative-tim.com

 =========================================================

* The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
*/

import { useMemo } from "react";

// prop-types is a library for typechecking of props
import PropTypes from "prop-types";

// uuid is a library for generating unique id
import { v4 as uuidv4 } from "uuid";

// @mui material components
import { Table as MuiTable } from "@mui/material";
import TableBody from "@mui/material/TableBody";
import TableContainer from "@mui/material/TableContainer";
import TableRow from "@mui/material/TableRow";

// Soft UI Dashboard React components
import SoftBox from "components/SoftBox";
import SoftAvatar from "components/SoftAvatar";
import SoftTypography from "components/SoftTypography";

// Soft UI Dashboard React base styles
import colors from "assets/theme/base/colors";
import typography from "assets/theme/base/typography";
import borders from "assets/theme/base/borders";

function Table({ columns = [], rows = [{}], "aria-label": ariaLabel }) {
  const { light } = colors;
  const { size, fontWeightBold } = typography;
  const { borderWidth } = borders;

  const renderColumns = columns.map(({ name, align, width }, key) => {
    let pl;
    let pr;

    if (key === 0) {
      pl = 3;
      pr = 3;
    } else if (key === columns.length - 1) {
      pl = 3;
      pr = 3;
    } else {
      pl = 1;
      pr = 1;
    }

    return (
      <SoftBox
        key={name}
        component="th"
        scope="col"
        width={width || "auto"}
        pt={1.5}
        pb={1.25}
        pl={align === "left" ? pl : 3}
        pr={align === "right" ? pr : 3}
        textAlign={align}
        fontSize={size.xxs}
        fontWeight={fontWeightBold}
        color="secondary"
        opacity={0.7}
        borderBottom={`${borderWidth[1]} solid ${light.main}`}
        sx={{
          background: "linear-gradient(180deg, #f8f9fa 0%, #f1f3f5 100%)",
          letterSpacing: "0.08em",
          textTransform: "uppercase",
          whiteSpace: "nowrap",
        }}
      >
        {name.toUpperCase()}
      </SoftBox>
    );
  });

  const renderRows = rows.map((row, key) => {
    const rowKey = row.id || `row-${key}`;

    const tableRow = columns.map(({ name, align }) => {
      let template;

      if (Array.isArray(row[name])) {
        template = (
          <SoftBox
            key={name}
            data-column={name}
            component="td"
            p={1}
            borderBottom={row.hasBorder ? `${borderWidth[1]} solid ${light.main}` : null}
            sx={{
              transition: "background 150ms ease",
            }}
          >
            <SoftBox display="flex" alignItems="center" py={0.5} px={1}>
              <SoftBox mr={2}>
                <SoftAvatar src={row[name][0]} name={row[name][1]} variant="rounded" size="sm" />
              </SoftBox>
              <SoftTypography variant="button" fontWeight="medium" sx={{ width: "max-content" }}>
                {row[name][1]}
              </SoftTypography>
            </SoftBox>
          </SoftBox>
        );
      } else {
        template = (
          <SoftBox
            key={name}
            data-column={name}
            component="td"
            p={1}
            textAlign={align}
            borderBottom={row.hasBorder ? `${borderWidth[1]} solid ${light.main}` : null}
            sx={{
              transition: "background 150ms ease",
            }}
          >
            <SoftTypography
              variant="button"
              fontWeight="regular"
              color="secondary"
              sx={{ display: "inline-block", width: "max-content" }}
            >
              {row[name]}
            </SoftTypography>
          </SoftBox>
        );
      }

      return template;
    });

    return (
      <TableRow
        key={rowKey}
        sx={{
          transition: "all 150ms ease",
          "&:hover, &:focus-within": {
            background: "linear-gradient(90deg, #f0f7ff 0%, #f8f9fa 100%)",
          },
        }}
      >
        {tableRow}
      </TableRow>
    );
  });

  return useMemo(
    () => (
      <TableContainer
        sx={{
          borderRadius: "1rem",
          border: "1px solid #e9ecef",
          boxShadow: "0 20px 27px 0 rgba(0, 0, 0, 0.05)",
          background: "#ffffff",
          overflow: "hidden",
          transition: "box-shadow 200ms ease",
          "&:hover": {
            boxShadow: "0 8px 24px rgba(0, 39, 76, 0.08), 0 2px 6px rgba(0, 39, 76, 0.04)",
          },
        }}
      >
        <MuiTable aria-label={ariaLabel}>
          <SoftBox component="thead">
            <TableRow>{renderColumns}</TableRow>
          </SoftBox>
          <TableBody>{renderRows}</TableBody>
        </MuiTable>
      </TableContainer>
    ),
    [columns, rows, ariaLabel]
  );
}

// Typechecking props for the Table
Table.propTypes = {
  "aria-label": PropTypes.string,
  columns: PropTypes.arrayOf(PropTypes.object),
  rows: PropTypes.arrayOf(PropTypes.object),
};

export default Table;
