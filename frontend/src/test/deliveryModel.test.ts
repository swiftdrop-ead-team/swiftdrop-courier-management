import { beforeEach, describe, expect, it } from "vitest";
import {
  bookingSchema,
  day,
  emptyBooking,
  estimate,
  publicProjection,
  seedPreview,
} from "../features/delivery/model";
import {
  getPreview,
  previewBook,
  previewQuote,
  previewStatus,
  resetPreview,
  updatePreview,
} from "../features/delivery/preview";
import {
  parseCsv,
  parseOrders,
  csvHeaders,
} from "../features/delivery/BulkOrders";
import { csvDocument } from "../pages/admin/adminData";

const booking = () => ({
  ...emptyBooking("Alex Morgan"),
  senderPhone: "0771234567",
  pickupAddress: "24 Main Road, Colombo",
  recipientName: "Nimal Perera",
  recipientPhone: "0772345678",
  dropoffAddress: "18 Lake Road, Kandy",
  zoneId: "kandy",
  description: "Books",
});
beforeEach(() => {
  resetPreview();
});
describe("Delivery domain and shared preview", () => {
  it("validates addresses, weights, phone numbers, and pickup dates", () => {
    expect(bookingSchema.safeParse(booking()).success).toBe(true);
    for (const patch of [
      { pickupAddress: "a" },
      { weight: 0 },
      { weight: 31 },
      { senderPhone: "bad" },
      { senderPhone: "(((((((" },
      { pickupDate: "2099-02-31" },
      { pickupDate: "2000-01-01" },
    ])
      expect(bookingSchema.safeParse({ ...booking(), ...patch }).success).toBe(
        false,
      );
  });
  it("prices actual/volumetric weight and express services using active zones", () => {
    const zones = seedPreview().zones;
    expect(estimate(booking(), zones)).toBe(500);
    expect(
      estimate(
        { ...booking(), length: 50, width: 50, height: 50, service: "EXPRESS" },
        zones,
      ),
    ).toBe(4640);
    expect(() => estimate({ ...booking(), zoneId: "unknown" }, zones)).toThrow(
      "active delivery zone",
    );
  });
  it("requires an unchanged, unexpired quote and prevents duplicate confirmation", () => {
    const original = booking();
    const quote = previewQuote(original);
    expect(() =>
      previewBook(
        { ...original, weight: 2 },
        quote.id,
        "preview-customer",
        "CUSTOMER",
      ),
    ).toThrow("booking changed");
    const parcel = previewBook(
      original,
      quote.id,
      "preview-customer",
      "CUSTOMER",
    );
    expect(parcel.amount).toBe(quote.amount);
    expect(() =>
      previewBook(original, quote.id, "preview-customer", "CUSTOMER"),
    ).toThrow();
  });
  it("connects booking, dispatch, courier milestones, notifications, and webhook attempts", () => {
    const order = booking();
    const parcel = previewBook(
      order,
      previewQuote(order).id,
      "preview-merchant",
      "MERCHANT",
    );
    updatePreview((state) => {
      state.integrations["preview-merchant"] = {
        apiKey: "preview",
        clientId: "preview",
        webhookUrl: "https://store.example.com/hooks",
        enabled: true,
      };
      const record = state.parcels.find((p) => p.id === parcel.id)!;
      record.courierId = "preview-courier";
      record.assignedDate = day();
    });
    for (const status of [
      "PICKED_UP",
      "IN_TRANSIT",
      "OUT_FOR_DELIVERY",
      "DELIVERED",
    ] as const)
      previewStatus(
        parcel.id,
        status,
        "",
        status === "DELIVERED" ? "Nimal Perera" : "",
        "preview-courier",
      );
    const saved = getPreview().parcels.find((p) => p.id === parcel.id)!;
    expect(saved.status).toBe("DELIVERED");
    expect(saved.recipientProof).toBe("Nimal Perera");
    expect(
      getPreview().notices.filter((n) => n.ownerId === "preview-merchant"),
    ).toHaveLength(5);
    expect(getPreview().webhooks["preview-merchant"]).toHaveLength(4);
    expect(publicProjection(saved)).not.toHaveProperty("recipientPhone");
    expect(publicProjection(saved).history.every((entry) => !entry.note)).toBe(
      true,
    );
  });
  it("blocks skipped stages, another courier, missing proof, and missing failure reason", () => {
    const parcel = getPreview().parcels.find(
      (p) => p.status === "OUT_FOR_DELIVERY",
    )!;
    expect(() =>
      previewStatus(parcel.id, "DELIVERED", "", "Nimal", "another-courier"),
    ).toThrow("not assigned");
    expect(() =>
      previewStatus(parcel.id, "DELIVERED", "", "", "preview-courier"),
    ).toThrow("recipient name");
    expect(() =>
      previewStatus(parcel.id, "FAILED", "", "", "preview-courier"),
    ).toThrow("reason");
    const created = getPreview().parcels.find(
      (p) => p.status === "CREATED" && p.courierId,
    )!;
    expect(() =>
      previewStatus(created.id, "OUT_FOR_DELIVERY", "", "", "preview-courier"),
    ).toThrow("previous delivery stage");
  });
  it("records failed attempts and prevents changes to closed deliveries", () => {
    const parcel = getPreview().parcels.find(
      (p) => p.status === "OUT_FOR_DELIVERY",
    )!;
    previewStatus(
      parcel.id,
      "FAILED",
      "Recipient unavailable",
      "",
      "preview-courier",
    );
    expect(
      getPreview().parcels.find((p) => p.id === parcel.id)?.failureReason,
    ).toBe("Recipient unavailable");
    expect(() =>
      previewStatus(parcel.id, "DELIVERED", "", "Nimal", "preview-courier"),
    ).toThrow("already closed");
  });
  it("only cancels unassigned deliveries and keeps accepted fees when zone rates change", () => {
    const parcel = getPreview().parcels.find(
      (p) => p.status === "CREATED" && !p.courierId,
    )!;
    const originalFee = parcel.amount;
    updatePreview((state) => {
      state.zones[0].base = 9999;
    });
    expect(getPreview().parcels.find((p) => p.id === parcel.id)?.amount).toBe(
      originalFee,
    );
    previewStatus(parcel.id, "CANCELLED", "", "");
    const assigned = getPreview().parcels.find(
      (p) => p.status === "CREATED" && p.courierId,
    )!;
    expect(() => previewStatus(assigned.id, "CANCELLED", "", "")).toThrow(
      "unassigned",
    );
  });
});
describe("Bulk order validation", () => {
  it("parses quoted addresses, embedded newlines, and escaped quotes", () => {
    expect(
      parseCsv('name,address\r\n"A ""Store""","24 Main Road,\nColombo"'),
    ).toEqual([
      ["name", "address"],
      ['A "Store"', "24 Main Road,\nColombo"],
    ]);
    expect(() => parseCsv('a,"unclosed')).toThrow("unclosed");
  });
  it("validates every row and reports row numbers without creating invalid orders", () => {
    const order = booking();
    const validCsv = csvDocument([
      csvHeaders,
      csvHeaders.map((key) => order[key]),
    ]);
    expect(parseOrders(validCsv)).toEqual({ orders: [order], errors: [] });
    const invalidCsv = csvDocument([
      csvHeaders,
      csvHeaders.map((key) => (key === "weight" ? -1 : order[key])),
    ]);
    expect(parseOrders(invalidCsv).errors[0]).toContain("Row 2");
    expect(() => parseOrders("recipientName,weight\nA,1")).toThrow("template");
  });
});
