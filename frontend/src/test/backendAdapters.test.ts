import { afterEach, expect, it, vi } from "vitest";
import { api } from "../services/api";
import { authService } from "../services/authService";
import { deliveryApi, type BackendParcel } from "../features/delivery/service";
import { day, emptyBooking } from "../features/delivery/model";
import { normalizeTracking } from "../features/delivery/TrackingView";
const parcel: BackendParcel = {
  id: "parcel-1",
  trackingNumber: "SD-1001-LK",
  senderId: "customer-1",
  senderType: "CUSTOMER",
  senderName: "Sender",
  senderPhone: "0771234567",
  pickupAddress: "24 Main Road",
  recipientName: "Recipient",
  recipientPhone: "0772345678",
  deliveryAddress: "18 Lake Road",
  deliveryCity: "Kandy",
  weightKg: 2,
  lengthCm: 20,
  widthCm: 15,
  heightCm: 10,
  description: "Books",
  deliveryType: "STANDARD",
  status: "CREATED",
  deliveryFee: 10,
  currency: "USD",
  pickupDate: "2027-01-20",
  codAmount: 2500.50,
  createdAt: new Date().toISOString(),
  statusHistory: [
    { newStatus: "CREATED", timestamp: new Date().toISOString() },
  ],
};
afterEach(() => vi.restoreAllMocks());
it("loads every backend page from the owner endpoint and converts actual parcel fields", async () => {
  const get = vi
    .spyOn(api, "get")
    .mockResolvedValueOnce({ data: { content: [parcel], totalPages: 2 } })
    .mockResolvedValueOnce({
      data: { content: [{ ...parcel, id: "parcel-2" }], totalPages: 2 },
    });
  const records = await deliveryApi.parcels("mine");
  expect(records).toHaveLength(2);
  expect(records[0]).toMatchObject({
    dropoffAddress: "18 Lake Road",
    weight: 2,
    amount: 10,
    currency: "USD",
    pickupDate: "2027-01-20",
    cod: 2500.50,
  });
  expect(get.mock.calls.every(([url]) => url === "/parcels/my-parcels")).toBe(
    true,
  );
});
it("sends the parcel backend DTO and binds confirmation to the returned server estimate", async () => {
  vi.spyOn(authService, "getCurrentUser").mockResolvedValue({
    id: "customer-1",
    roles: ["CUSTOMER"],
    fullName: "Customer",
    email: "customer@example.test",
    status: "ACTIVE",
  });
  const booking = {
    ...emptyBooking("Sender"),
    senderPhone: "0771234567",
    pickupAddress: "24 Main Road",
    recipientName: "Recipient",
    recipientPhone: "0772345678",
    dropoffAddress: "18 Lake Road",
    zoneId: "Kandy",
    description: "Books",
    pickupDate: "2027-01-20",
    cod: 2500.50,
  };
  const post = vi
    .spyOn(api, "post")
    .mockResolvedValueOnce({
      data: {
        id: "estimate-1",
        amount: 10,
        currency: "USD",
        expiresAt: new Date(Date.now() + 300000).toISOString(),
      },
    })
    .mockResolvedValueOnce({ data: parcel });
  const estimate = await deliveryApi.quote(booking);
  await deliveryApi.create(booking, estimate.id);
  expect(post.mock.calls[0][0]).toBe("/parcels/estimate");
  expect(post.mock.calls[1][1]).toMatchObject({
    estimateId: "estimate-1",
    pickupDate: "2027-01-20",
    codAmount: 2500.50,
    deliveryAddress: "18 Lake Road",
    weightKg: 1,
    deliveryType: "STANDARD",
  });
  expect(post.mock.calls[1][1]).not.toHaveProperty("dropoffAddress");
});
it("maps identity UUIDs to numeric dispatch IDs and includes the required assignment details", async () => {
  vi.spyOn(api, "get")
    .mockResolvedValueOnce({ data: parcel })
    .mockResolvedValueOnce({
      data: { success: true, data: { id: 17, userId: "courier-user" } },
    });
  const post = vi.spyOn(api, "post").mockResolvedValue({});
  await deliveryApi.assign(parcel.id, "courier-user", day());
  expect(post).toHaveBeenCalledWith(
    "/dispatch/assignments",
    expect.objectContaining({
      courierId: 17,
      parcelId: parcel.id,
      trackingNumber: parcel.trackingNumber,
      deliveryAddress: parcel.deliveryAddress,
    }),
  );
});
it("uses the assignment proof endpoint rather than a speculative parcel status payload", async () => {
  vi.spyOn(authService, "getCurrentUser").mockResolvedValue({
    id: "courier-user",
    roles: ["COURIER"],
    fullName: "Courier",
    email: "courier@example.com",
    status: "ACTIVE",
  });
  vi.spyOn(api, "get")
    .mockResolvedValueOnce({ data: { success: true, data: { id: 17 } } })
    .mockResolvedValueOnce({
      data: { success: true, data: [{ id: 42, parcelId: "parcel-1" }] },
    });
  const post = vi.spyOn(api, "post").mockResolvedValue({});
  await deliveryApi.status(
    "parcel-1",
    "DELIVERED",
    "At reception",
    "Recipient",
  );
  expect(post).toHaveBeenCalledWith("/dispatch/assignments/42/proof", {
    recipientName: "Recipient",
    deliveryNotes: "At reception",
  });
});
it("normalizes tracking service timestamps and excludes private event fields", () => {
  const result = normalizeTracking({
    trackingNumber: "SD-1001-LK",
    currentStatus: "IN_TRANSIT",
    lastUpdatedAt: "2026-11-13T12:00:00",
    timeline: [
      {
        newStatus: "IN_TRANSIT",
        occurredAt: "2026-11-13T12:00:00",
        description: "Private note",
      },
    ],
  });
  expect(result.updatedAt).toBe("2026-11-13T06:30:00.000Z");
  expect(result.history[0].note).toBe("");
  expect(result).not.toHaveProperty("timeline");
});

it("routes merchant bookings through order intake with a stable store reference", async () => {
  vi.spyOn(authService, "getCurrentUser").mockResolvedValue({
    id: "merchant-1",
    roles: ["MERCHANT"],
    fullName: "Merchant",
    email: "merchant@example.test",
    status: "ACTIVE",
  });
  const post = vi
    .spyOn(api, "post")
    .mockResolvedValue({
      data: { success: true, data: { parcelId: parcel.id } },
    });
  vi.spyOn(api, "get").mockResolvedValue({ data: parcel });
  await deliveryApi.create(
    { ...emptyBooking("Store"), pickupDate: day() },
    "quote-1",
    "batch-row-2",
  );
  expect(post).toHaveBeenCalledWith(
    "/merchant/orders",
    expect.objectContaining({
      estimateId: "quote-1",
      deliveryType: "STANDARD",
    }),
    { headers: { "Idempotency-Key": "batch-row-2" } },
  );
});
it("reads credential metadata without inventing a recoverable secret", async () => {
  vi.spyOn(api, "get").mockImplementation(async (url) => ({
    data: {
      success: true,
      data:
        url === "/merchant/profile"
          ? { id: "merchant-profile" }
          : url === "/merchant/api-keys"
            ? [{ id: 1 }]
            : [
                {
                  id: 2,
                  targetUrl: "https://store.example/hook",
                  active: true,
                },
              ],
    },
  }));
  expect(await deliveryApi.integration()).toEqual({
    apiKey: "",
    hasKey: true,
    clientId: "merchant-profile",
    webhookUrl: "https://store.example/hook",
    enabled: true,
  });
});
