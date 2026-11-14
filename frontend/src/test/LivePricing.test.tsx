import React from "react";
import { afterEach, expect, it, vi } from "vitest";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { ThemeProvider } from "@mui/material/styles";
import theme from "assets/theme";
import { LivePricing } from "../features/delivery/LivePricing";
import { api } from "../services/api";

afterEach(() => vi.restoreAllMocks());

it("loads persisted rates and submits the edited service without preview zone fields", async () => {
  vi.spyOn(api, "get").mockResolvedValue({
    data: [
      {
        serviceType: "STANDARD",
        baseFare: 250,
        baseWeightKg: 1,
        perKgRate: 80,
        perKmRate: 0,
        volumetricDivisor: 5000,
        urgencyMultiplier: 1,
        isActive: true,
      },
    ],
  });
  const put = vi.spyOn(api, "put").mockResolvedValue({});
  render(
    <ThemeProvider theme={theme}>
      <LivePricing />
    </ThemeProvider>,
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Edit standard rates" }),
  );
  const dialog = within(screen.getByRole("dialog"));
  fireEvent.change(
    dialog.getByRole("spinbutton", { name: "Base fare (LKR)" }),
    { target: { value: "350" } },
  );
  fireEvent.click(dialog.getByRole("button", { name: "Save rates" }));
  await waitFor(() =>
    expect(put).toHaveBeenCalledWith(
      "/pricing/rules/STANDARD",
      expect.objectContaining({ baseFare: 350, active: true }),
    ),
  );
  await waitFor(() =>
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
  );
});

it("reports a live pricing failure without inventing sample rates", async () => {
  vi.spyOn(api, "get").mockRejectedValue(
    new Error("Pricing service unavailable"),
  );
  render(
    <ThemeProvider theme={theme}>
      <LivePricing />
    </ThemeProvider>,
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Pricing service unavailable",
  );
  expect(
    screen.queryByRole("button", { name: /Edit standard/ }),
  ).not.toBeInTheDocument();
});
