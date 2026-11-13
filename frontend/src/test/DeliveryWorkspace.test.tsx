import React from "react";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { ThemeProvider } from "@mui/material/styles";
import theme from "assets/theme";
import { DeliveryDashboard } from "../features/delivery/DeliveryDashboard";
import { TrackingView } from "../features/delivery/TrackingView";
import {
  getPreview,
  resetPreview,
  previewStatus,
  updatePreview,
} from "../features/delivery/preview";
import { api } from "../services/api";
import type { UserRole } from "../types/auth";

vi.mock("../hooks/useAuth", () => ({
  useAuth: () => ({
    user: {
      id: "live-user",
      fullName: "Live User",
      email: "user@example.com",
      roles: ["CUSTOMER"],
      status: "ACTIVE",
      merchantApprovalStatus: "APPROVED",
    },
  }),
}));
beforeEach(() => {
  vi.restoreAllMocks();
  resetPreview();
});
function workspace(role: UserRole, tab: string, preview = true) {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[`/workspace/${tab}`]}>
        <Routes>
          <Route
            path="/workspace/:tab"
            element={<DeliveryDashboard role={role} previewMode={preview} />}
          />
          <Route
            path="*"
            element={<DeliveryDashboard role={role} previewMode={preview} />}
          />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  );
}
async function choose(label: string, value: string) {
  fireEvent.mouseDown(screen.getByLabelText(label));
  fireEvent.click(await screen.findByRole("option", { name: value }));
}
describe("Assignment frontend workflows", () => {
  it("quotes a customer booking and only creates it after fee confirmation", async () => {
    const initial = getPreview().parcels.length;
    workspace("CUSTOMER", "send");
    for (const [label, value] of [
      ["Sender phone", "0771234567"],
      ["Pickup address", "24 Main Road, Colombo"],
      ["Recipient name", "Nimal Perera"],
      ["Recipient phone", "0772345678"],
      ["Drop-off address", "18 Lake Road, Kandy"],
      ["Parcel contents", "Books"],
    ])
      fireEvent.change(screen.getByRole("textbox", { name: label }), {
        target: { value },
      });
    await choose("Delivery zone", "Kandy");
    fireEvent.click(
      screen.getByRole("button", { name: "Get delivery estimate" }),
    );
    expect(
      await screen.findByRole("button", { name: "Confirm delivery request" }),
    ).toBeDisabled();
    expect(getPreview().parcels).toHaveLength(initial);
    fireEvent.click(
      screen.getByRole("checkbox", { name: /I confirm the addresses/ }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Confirm delivery request" }),
    );
    await waitFor(() => expect(getPreview().parcels).toHaveLength(initial + 1));
    expect(getPreview().parcels[0].ownerId).toBe("preview-customer");
  });
  it("assigns an unassigned delivery and shares the assignment with the courier view", async () => {
    const rendered = workspace("ADMIN", "dispatch");
    fireEvent.click(
      within(
        screen.getByText("SD-DEMO-1001").parentElement!.parentElement!,
      ).getByRole("button", { name: "Select" }),
    );
    await choose("Available courier", "Courier Preview");
    fireEvent.click(screen.getByRole("button", { name: "Confirm assignment" }));
    await waitFor(() =>
      expect(
        getPreview().parcels.find((p) => p.trackingNumber === "SD-DEMO-1001")
          ?.courierId,
      ).toBe("preview-courier"),
    );
    rendered.unmount();
    workspace("COURIER", "deliveries");
    expect(await screen.findByText("SD-DEMO-1001")).toBeInTheDocument();
  });
  it("requires recipient proof and COD confirmation before completing a delivery", async () => {
    workspace("COURIER", "deliveries");
    fireEvent.click(screen.getByRole("button", { name: "View SD-DEMO-1006" }));
    const dialog = within(screen.getByRole("dialog"));
    fireEvent.click(dialog.getByRole("button", { name: "Mark delivered" }));
    fireEvent.click(dialog.getByRole("button", { name: "Confirm update" }));
    expect(
      dialog.getByText(
        "Enter the recipient name and confirm any cash collection.",
      ),
    ).toBeInTheDocument();
    fireEvent.change(
      dialog.getByRole("textbox", {
        name: "Recipient name (proof of delivery)",
      }),
      { target: { value: "Nimal Perera" } },
    );
    fireEvent.click(dialog.getByRole("checkbox", { name: /I collected/ }));
    fireEvent.click(dialog.getByRole("button", { name: "Confirm update" }));
    await waitFor(() =>
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
    );
    expect(
      getPreview().parcels.find((p) => p.trackingNumber === "SD-DEMO-1006")
        ?.status,
    ).toBe("DELIVERED");
  });
  it("shows public progress without exposing addresses, phone numbers, notes, or proof", () => {
    updatePreview((state) => {
      state.parcels[0].history[0].note = "Private address and phone";
    });
    render(
      <ThemeProvider theme={theme}>
        <TrackingView preview initialNumber="SD-DEMO-1001" />
      </ThemeProvider>,
    );
    expect(screen.getByText("SD-DEMO-1001")).toBeInTheDocument();
    expect(screen.queryByText("0772345678")).not.toBeInTheDocument();
    expect(
      screen.queryByText("Private address and phone"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText(/24 Sample Road/)).not.toBeInTheDocument();
  });
  it("reports unavailable live APIs and never substitutes preview shipments", async () => {
    vi.spyOn(api, "get").mockRejectedValue({ response: { status: 503 } });
    workspace("CUSTOMER", "history", false);
    expect(
      await screen.findByText(/parcels: This service is not available yet/),
    ).toBeInTheDocument();
    expect(screen.queryByText("SD-DEMO-1001")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Refresh data" })).toBeEnabled();
  });
  it("saves zone rates and rejects deactivating your own administrator account", async () => {
    const rendered = workspace("ADMIN", "pricing");
    fireEvent.click(screen.getByRole("button", { name: "Edit Colombo" }));
    const dialog = within(screen.getByRole("dialog"));
    fireEvent.change(
      dialog.getByRole("spinbutton", { name: "Base fee (LKR)" }),
      { target: { value: "600" } },
    );
    fireEvent.click(dialog.getByRole("button", { name: "Save zone" }));
    await waitFor(() =>
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
    );
    expect(getPreview().zones.find((zone) => zone.id === "colombo")?.base).toBe(
      600,
    );
    rendered.unmount();
    workspace("ADMIN", "access");
    fireEvent.click(screen.getByRole("button", { name: "Edit Admin Preview" }));
    await choose("Account status", "SUSPENDED");
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "Confirm access changes",
      }),
    );
    expect(
      within(screen.getByRole("dialog")).getByText(
        /cannot remove your own administrator access/,
      ),
    ).toBeInTheDocument();
  });
  it("saves a webhook, shows generated status attempts, and simulates a retry", async () => {
    workspace("MERCHANT", "api");
    fireEvent.change(
      screen.getByRole("textbox", { name: "Webhook endpoint URL" }),
      { target: { value: "https://store.example.com/webhooks" } },
    );
    fireEvent.click(
      screen.getByRole("checkbox", { name: "Enable status webhooks" }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Save webhook settings" }),
    );
    await waitFor(() =>
      expect(getPreview().integrations["preview-merchant"]?.enabled).toBe(true),
    );
    const { act } = await import("@testing-library/react");
    act(() =>
      previewStatus(
        "sample-6",
        "FAILED",
        "Recipient unavailable",
        "",
        "preview-courier",
      ),
    );
    expect(
      await screen.findByText("SD-DEMO-1006 · FAILED"),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Simulate retry" }));
    await waitFor(() =>
      expect(getPreview().webhooks["preview-merchant"][0].result).toBe(
        "DELIVERED",
      ),
    );
  });
  it("saves profile preferences and an address for the booking form", async () => {
    workspace("CUSTOMER", "settings");
    fireEvent.click(screen.getByRole("button", { name: "Edit profile" }));
    fireEvent.change(screen.getByRole("textbox", { name: "Full name" }), {
      target: { value: "Alex Perera" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save profile" }));
    await waitFor(() =>
      expect(getPreview().profiles["preview-customer"]?.fullName).toBe(
        "Alex Perera",
      ),
    );
    fireEvent.change(screen.getByRole("textbox", { name: "Address label" }), {
      target: { value: "Home" },
    });
    fireEvent.change(
      screen.getByRole("textbox", { name: "Complete address" }),
      { target: { value: "24 Main Road, Colombo" } },
    );
    fireEvent.click(screen.getByRole("button", { name: "Save address" }));
    await waitFor(() =>
      expect(getPreview().addresses["preview-customer"][0].label).toBe("Home"),
    );
  });
  it.each<[UserRole, string, string]>([
    ["CUSTOMER", "overview", "Your deliveries, connected."],
    ["MERCHANT", "bulk-upload", "Upload orders"],
    ["MERCHANT", "shipments", "My shipments"],
    ["COURIER", "route", "Active route"],
    ["COURIER", "notifications", "Notifications"],
    ["ADMIN", "reports", "Delivery reports"],
  ])("renders the %s %s workspace", (role, tab, title) => {
    workspace(role, tab);
    expect(
      screen.getByRole("heading", { level: 1, name: title }),
    ).toBeInTheDocument();
  });
});
