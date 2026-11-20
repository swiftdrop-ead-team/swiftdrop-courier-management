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
import { TrackingView } from "@/features/delivery/tabs/TrackingView";
import {
  getPreview,
  resetPreview,
  previewStatus,
  updatePreview,
} from "../features/delivery/preview";
import { api } from "../services/api";
import { deliveryApi } from "../features/delivery/service";
import { adminService } from "../services/authService";
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
  it("uses live capacity and reloads the queue after the dispatch service accepts an assignment", async () => {
    const parcel = getPreview().parcels.find((p) => !p.courierId && p.status === "CREATED")!;
    const queue = vi.spyOn(deliveryApi, "parcels").mockResolvedValueOnce([parcel]).mockResolvedValue([]);
    const driver = { id: "linked", name: "Available driver", capacity: 6, available: true,
      activeDeliveries: 4, accountLinked: true, availableForAssignment: true };
    vi.spyOn(deliveryApi, "couriers").mockResolvedValue([
      driver,
      { ...driver, id: "busy", name: "Busy driver", availableForAssignment: false },
      { ...driver, id: "legacy", name: "Unlinked driver", accountLinked: false },
    ]);
    const assign = vi.spyOn(deliveryApi, "assign").mockResolvedValue();
    workspace("ADMIN", "dispatch", false);
    const form = await screen.findByRole("form", { name: "Assign a parcel" });
    fireEvent.change(within(form).getByRole("combobox", { name: "Unassigned parcel" }), { target: { value: parcel.recipientName } });
    fireEvent.click(await screen.findByRole("option", { name: new RegExp(parcel.trackingNumber) }));
    const picker = screen.getByRole("combobox", { name: "Available courier" });
    expect(within(picker).getAllByRole("option")).toHaveLength(2);
    expect(within(picker).getByRole("option", { name: "Available driver · 2 slots open" })).toBeInTheDocument();
    fireEvent.change(picker, { target: { value: driver.id } });
    fireEvent.click(screen.getByRole("button", { name: "Confirm assignment" }));
    expect(assign).toHaveBeenCalledWith(parcel.id, driver.id, expect.any(String));
    expect(await screen.findByText(/No parcels are waiting for assignment/)).toBeInTheDocument();
    expect(queue).toHaveBeenCalledTimes(2);
  });
  it("books for the selected customer and sends that owner through estimate and confirmation", async () => {
    vi.spyOn(adminService, "getAllUsers").mockResolvedValue([{ id: "selected-customer", fullName: "Booking Customer", email: "booking@example.com", phoneNumber: "0771234567", roles: ["CUSTOMER"], status: "ACTIVE" }] as any);
    const quote = vi.spyOn(deliveryApi, "quote").mockResolvedValue({ id: "estimate", amount: 250, currency: "LKR", expiresAt: new Date(Date.now()+60000).toISOString() });
    const create = vi.spyOn(deliveryApi, "create").mockResolvedValue({ ...getPreview().parcels[0], trackingNumber: "SD-ADMIN-BOOKED" });
    vi.spyOn(deliveryApi, "parcels").mockResolvedValue([]);
    vi.spyOn(deliveryApi, "couriers").mockResolvedValue([]);
    workspace("ADMIN", "create-booking", false);
    await screen.findByLabelText(/Customer account/);
    fireEvent.mouseDown(await screen.findByLabelText(/Customer account/));
    fireEvent.click(await screen.findByRole("option", { name: "Booking Customer - booking@example.com" }));
    for (const [label,value] of [["Pickup address","24 Main Road Colombo"],["Recipient name","Recipient"],["Recipient phone","0772345678"],["Drop-off address","18 Main Road Kandy"],["Delivery city","Kandy"],["Parcel contents","Books"]])
      fireEvent.change(screen.getByRole("textbox", { name: label }), { target: { value } });
    fireEvent.click(screen.getByRole("button", { name: "Get delivery estimate" }));
    const confirm = await screen.findByRole("button", { name: "Confirm delivery request" });
    expect(quote).toHaveBeenCalledWith(expect.objectContaining({senderName:"Booking Customer"}), "selected-customer");
    fireEvent.click(screen.getByRole("checkbox", { name: /I confirm the addresses/ }));
    fireEvent.click(confirm);
    await waitFor(() => expect(create).toHaveBeenCalledWith(expect.anything(), "estimate", "estimate", "selected-customer"));
  });
  it("reassigns the selected parcel using its existing assignment ID and refreshes", async () => {
    const parcel = {...getPreview().parcels[0], status:"CREATED" as const, assignmentId:"7", courierId:"old"};
    const load = vi.spyOn(deliveryApi,"parcels").mockResolvedValue([parcel]);
    vi.spyOn(deliveryApi,"couriers").mockResolvedValue([{id:"new",dispatchId:"4",name:"Replacement driver",capacity:5,available:true,activeDeliveries:0,accountLinked:true,availableForAssignment:true}]);
    const reassign=vi.spyOn(deliveryApi,"reassign").mockResolvedValue();
    workspace("ADMIN","dispatch",false);
    await screen.findByLabelText("Assigned parcel");
    await choose("Assigned parcel",parcel.trackingNumber);
    await choose("New courier","Replacement driver");
    fireEvent.click(screen.getByRole("button",{name:"Reassign courier"}));
    await waitFor(() => expect(reassign).toHaveBeenCalledWith("7","4"));
    await waitFor(() => expect(load).toHaveBeenCalledTimes(2));
  });
  it("clears previously loaded parcels when a refresh fails", async () => {
    const get = vi.spyOn(api, "get").mockResolvedValue({ data: {
      content: [{ id: "persisted-1", trackingNumber: "SD-PERSISTED-1", senderId: "live-user",
        senderType: "CUSTOMER", senderName: "Live User", recipientName: "Recipient",
        pickupAddress: "Colombo", deliveryAddress: "Kandy", deliveryCity: "Kandy",
        weightKg: 1, lengthCm: 10, widthCm: 10, heightCm: 10,
        deliveryType: "STANDARD", status: "CREATED", deliveryFee: 350, currency: "LKR",
        createdAt: new Date().toISOString() }], totalPages: 1,
    } });
    workspace("CUSTOMER", "overview", false);
    expect(await screen.findByText("SD-PERSISTED-1")).toBeInTheDocument();
    get.mockRejectedValue({ response: { status: 503 } });
    fireEvent.click(screen.getByRole("button", { name: "Refresh data" }));
    expect(await screen.findByText(/parcels: This service is temporarily unavailable/)).toBeInTheDocument();
    expect(screen.queryByText("SD-PERSISTED-1")).not.toBeInTheDocument();
    expect(screen.queryByText("SD-DEMO-1001")).not.toBeInTheDocument();
  });
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
  it("shows the selected delivery service and submits the selected value", async () => {
    workspace("CUSTOMER", "send");
    const standard = screen.getByRole("radio", { name: "Standard" });
    const express = screen.getByRole("radio", { name: "Express" });
    expect(standard).toBeChecked();
    expect(express).not.toBeChecked();

    fireEvent.click(express);

    expect(express).toBeChecked();
    expect(standard).not.toBeChecked();
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
    fireEvent.click(
      await screen.findByRole("checkbox", {
        name: /I confirm the addresses/,
      }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Confirm delivery request" }),
    );
    await waitFor(() =>
      expect(getPreview().parcels[0].service).toBe("EXPRESS"),
    );
  });
  it("assigns an unassigned delivery and shares the assignment with the courier view", async () => {
    const rendered = workspace("ADMIN", "dispatch");
    fireEvent.change(screen.getByRole("combobox", { name: "Unassigned parcel" }), { target: { value: "SD-DEMO-1001" } });
    fireEvent.click(await screen.findByRole("option", { name: /SD-DEMO-1001/ }));
    const picker = screen.getByRole("combobox", { name: "Available courier" });
    fireEvent.change(picker, { target: { value: "preview-courier" } });
    expect(picker).toHaveValue("preview-courier");
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
      await screen.findByText(/parcels: This service is temporarily unavailable/),
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
    ["ADMIN", "dispatch", "Dispatch workspace"],
    ["ADMIN", "deliveries", "All deliveries"],
    ["ADMIN", "pricing", "Delivery pricing"],
    ["ADMIN", "reports", "Delivery reports"],
    ["ADMIN", "access", "Manage user access"],
    ["ADMIN", "profile", "Admin profile"],
    ["MERCHANT", "shipments", "My shipments"],
    ["MERCHANT", "create-shipment", "Send a parcel"],
    ["MERCHANT", "bulk-upload", "Upload orders"],
    ["MERCHANT", "tracking", "Track a parcel"],
    ["MERCHANT", "history", "Delivery history"],
    ["MERCHANT", "profile", "Your profile"],
    ["MERCHANT", "reports", "Reports & COD"],
    ["MERCHANT", "api", "Store connections"],
    ["MERCHANT", "notifications", "Notifications"],
    ["COURIER", "deliveries", "Deliveries"],
    ["COURIER", "route", "Active route"],
    ["COURIER", "history", "Delivery history"],
    ["COURIER", "profile", "Your profile"],
    ["COURIER", "notifications", "Notifications"],
    ["CUSTOMER", "send", "Send a parcel"],
    ["CUSTOMER", "track", "Track a shipment"],
    ["CUSTOMER", "history", "Delivery history"],
    ["CUSTOMER", "settings", "Account settings"],
    ["CUSTOMER", "notifications", "Notifications"],
  ])("renders the %s %s workspace", (role, tab, title) => {
    workspace(role, tab);
    expect(
      screen.getByRole("heading", { level: 1, name: title }),
    ).toBeInTheDocument();
  });
  it("rejects a non-CSV file in the merchant bulk uploader", async () => {
    workspace("MERCHANT", "bulk-upload");
    const file = new File(["not a csv"], "orders.txt", {
      type: "text/plain",
    });

    fireEvent.change(screen.getByLabelText("Choose orders CSV"), {
      target: { files: [file] },
    });

    expect(await screen.findByText("Choose a .csv file.")).toBeInTheDocument();
    expect(
      screen.getByRole("button", {
        name: "3. Estimate all delivery fees",
      }),
    ).toBeDisabled();
  });
});


describe("Merchant operations", () => {
  it("opens the failure queue from its status URL", async () => {
    workspace("MERCHANT", "shipments?status=FAILED");
    expect(screen.getByText("SD-DEMO-1008")).toBeInTheDocument();
    expect(screen.queryByText("SD-DEMO-1002")).not.toBeInTheDocument();
  });
  it("shows COD reporting and rejects reversed date ranges", async () => {
    workspace("MERCHANT", "reports");
    expect(screen.getByText("COD on delivered parcels")).toBeInTheDocument();
    expect(screen.getByRole("table", { name: "Merchant delivery outcomes" })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("From booking date"), { target: { value: "2027-02-01" } });
    fireEvent.change(screen.getByLabelText("To booking date"), { target: { value: "2027-01-01" } });
    expect(screen.getByRole("button", { name: "Export report CSV" })).toBeDisabled();
    expect(screen.getByText("The end date must be on or after the start date.")).toBeInTheDocument();
  });
  it("saves a merchant pickup location for future bookings", async () => {
    workspace("MERCHANT", "profile");
    fireEvent.change(screen.getByLabelText("Address label", { exact: false }), { target: { value: "Warehouse" } });
    fireEvent.change(screen.getByLabelText("Complete address", { exact: false }), { target: { value: "12 Warehouse Road, Colombo" } });
    fireEvent.click(screen.getByRole("button", { name: "Save address" }));
    await waitFor(() => expect(getPreview().addresses["preview-merchant"]).toEqual([expect.objectContaining({ label: "Warehouse" })]));
  });
});
