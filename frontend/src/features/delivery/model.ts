import { z } from "zod";
import type { UserRole } from "../../types/auth";

export const statuses = [
  "CREATED",
  "PICKED_UP",
  "IN_TRANSIT",
  "OUT_FOR_DELIVERY",
  "DELIVERED",
  "FAILED",
  "CANCELLED",
] as const;
export type ParcelStatus = (typeof statuses)[number];
export const labels: Record<ParcelStatus, string> = {
  CREATED: "Created",
  PICKED_UP: "Picked up",
  IN_TRANSIT: "In transit",
  OUT_FOR_DELIVERY: "Out for delivery",
  DELIVERED: "Delivered",
  FAILED: "Failed",
  CANCELLED: "Cancelled",
};
export const terminal = (status: ParcelStatus) =>
  ["DELIVERED", "FAILED", "CANCELLED"].includes(status);
export const money = (value: number) =>
  new Intl.NumberFormat("en-LK", { style: "currency", currency: "LKR" }).format(
    value,
  );
export const day = (value = new Date().toISOString()) =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Colombo",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(value));
export const time = (value: string) =>
  new Date(value).toLocaleString("en-LK", {
    timeZone: "Asia/Colombo",
    dateStyle: "medium",
    timeStyle: "short",
  });
export const phoneSchema = z
  .string()
  .trim()
  .regex(/^\+?[\d\s()-]{7,20}$/, "Enter a valid phone number")
  .refine((value) => {
    const digits = value.replace(/\D/g, "").length;
    return digits >= 7 && digits <= 15;
  }, "Enter a phone number with 7–15 digits");
export const bookingSchema = z.object({
  senderName: z.string().trim().min(2, "Enter the sender name"),
  senderPhone: phoneSchema,
  pickupAddress: z.string().trim().min(5, "Enter a complete pickup address"),
  recipientName: z.string().trim().min(2, "Enter the recipient name"),
  recipientPhone: phoneSchema,
  dropoffAddress: z.string().trim().min(5, "Enter a complete delivery address"),
  zoneId: z.string().min(1, "Select a delivery zone"),
  description: z.string().trim().min(2, "Describe the parcel"),
  weight: z.coerce
    .number()
    .positive("Weight must be positive")
    .max(30, "Maximum weight is 30 kg"),
  length: z.coerce.number().positive().max(200),
  width: z.coerce.number().positive().max(200),
  height: z.coerce.number().positive().max(200),
  service: z.enum(["STANDARD", "EXPRESS"]),
  pickupDate: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/)
    .refine((value) => {
      const parsed = new Date(`${value}T00:00:00Z`);
      return (
        !Number.isNaN(parsed.getTime()) &&
        parsed.toISOString().slice(0, 10) === value
      );
    }, "Enter a valid pickup date")
    .refine((value) => value >= day(), "Pickup cannot be in the past"),
  instructions: z.string().trim().max(500),
  cod: z.coerce.number().min(0).max(100000),
});
export type Booking = z.infer<typeof bookingSchema>;
export interface HistoryEntry {
  status: ParcelStatus;
  timestamp: string;
  note: string;
}
export interface ParcelRecord extends Booking {
  id: string;
  trackingNumber: string;
  ownerId: string;
  ownerRole: UserRole;
  status: ParcelStatus;
  amount: number;
  createdAt: string;
  courierId?: string;
  assignedDate?: string;
  recipientProof?: string;
  failureReason?: string;
  history: HistoryEntry[];
}
export interface Courier {
  id: string;
  name: string;
  available: boolean;
  capacity: number;
}
export interface Zone {
  id: string;
  name: string;
  base: number;
  perKg: number;
  expressMultiplier: number;
  active: boolean;
}
export interface Notice {
  id: string;
  ownerId: string;
  text: string;
  read: boolean;
  timestamp: string;
}
export interface Integration {
  apiKey: string;
  hasKey?: boolean;
  clientId: string;
  webhookUrl: string;
  enabled: boolean;
}
export interface WebhookAttempt {
  id: string;
  trackingNumber: string;
  status: ParcelStatus;
  timestamp: string;
  result: "PENDING" | "DELIVERED" | "FAILED";
  attempts: number;
}
export interface Profile {
  fullName: string;
  email: string;
  phone: string;
  businessName: string;
  address: string;
  emailNotifications: boolean;
}
export interface SavedAddress {
  id: string;
  label: string;
  address: string;
}
export interface Quote {
  id: string;
  amount: number;
  expiresAt: string;
}
export interface PublicTracking {
  trackingNumber: string;
  status: ParcelStatus;
  updatedAt: string;
  history: HistoryEntry[];
}
export interface PreviewState {
  version: 1;
  parcels: ParcelRecord[];
  couriers: Courier[];
  zones: Zone[];
  notices: Notice[];
  integrations: Record<string, Integration>;
  webhooks: Record<string, WebhookAttempt[]>;
  profiles: Record<string, Profile>;
  addresses: Record<string, SavedAddress[]>;
}
export const previewOwner = (role: UserRole) => `preview-${role.toLowerCase()}`;
export const emptyBooking = (name = ""): Booking => ({
  senderName: name,
  senderPhone: "",
  pickupAddress: "",
  recipientName: "",
  recipientPhone: "",
  dropoffAddress: "",
  zoneId: "",
  description: "",
  weight: 1,
  length: 20,
  width: 15,
  height: 10,
  service: "STANDARD",
  pickupDate: day(),
  instructions: "",
  cod: 0,
});
export const nextStatus: Partial<Record<ParcelStatus, ParcelStatus>> = {
  CREATED: "PICKED_UP",
  PICKED_UP: "IN_TRANSIT",
  IN_TRANSIT: "OUT_FOR_DELIVERY",
  OUT_FOR_DELIVERY: "DELIVERED",
};
export function validateTransition(
  parcel: ParcelRecord,
  status: ParcelStatus,
  note: string,
  proof: string,
) {
  if (terminal(parcel.status))
    throw new Error("This delivery is already closed.");
  if (status === "CANCELLED") {
    if (parcel.status !== "CREATED" || parcel.courierId)
      throw new Error("Only unassigned bookings can be cancelled.");
    return;
  }
  if (!parcel.courierId)
    throw new Error("Assign a courier before updating delivery progress.");
  if (status === "FAILED") {
    if (!note.trim())
      throw new Error("Enter a reason for the failed delivery.");
    return;
  }
  if (nextStatus[parcel.status] !== status)
    throw new Error("Complete the previous delivery stage first.");
  if (status === "DELIVERED" && !proof.trim())
    throw new Error("Enter the recipient name as proof of delivery.");
}
export function estimate(booking: Booking, zones: Zone[]) {
  const zone = zones.find((item) => item.id === booking.zoneId && item.active);
  if (!zone) throw new Error("Choose an active delivery zone.");
  const chargeableWeight = Math.max(
    booking.weight,
    (booking.length * booking.width * booking.height) / 5000,
  );
  return (
    Math.round(
      (zone.base + zone.perKg * chargeableWeight) *
        (booking.service === "EXPRESS" ? zone.expressMultiplier : 1) *
        100,
    ) / 100
  );
}
export function publicProjection(parcel: ParcelRecord): PublicTracking {
  // Public responses must never contain personal addresses, phones, or handover proof.
  return {
    trackingNumber: parcel.trackingNumber,
    status: parcel.status,
    updatedAt:
      parcel.history[parcel.history.length - 1]?.timestamp || parcel.createdAt,
    history: parcel.history.map(({ status, timestamp }) => ({
      status,
      timestamp,
      note: "",
    })),
  };
}
export function seedPreview(): PreviewState {
  const zones: Zone[] = [
    {
      id: "colombo",
      name: "Colombo",
      base: 250,
      perKg: 80,
      expressMultiplier: 1.5,
      active: true,
    },
    {
      id: "kandy",
      name: "Kandy",
      base: 400,
      perKg: 100,
      expressMultiplier: 1.6,
      active: true,
    },
    {
      id: "galle",
      name: "Galle",
      base: 350,
      perKg: 90,
      expressMultiplier: 1.5,
      active: true,
    },
  ];
  const now = new Date().toISOString();
  const parcels: ParcelRecord[] = Array.from({ length: 8 }, (_, i) => {
    const status: ParcelStatus =
      i < 3
        ? "CREATED"
        : i === 3
          ? "PICKED_UP"
          : i === 4
            ? "IN_TRANSIT"
            : i === 5
              ? "OUT_FOR_DELIVERY"
              : i === 6
                ? "DELIVERED"
                : "FAILED";
    const booking: Booking = {
      ...emptyBooking("Alex Morgan"),
      senderPhone: "0771234567",
      pickupAddress: "24 Sample Road, Colombo 03",
      recipientName: ["Nimal Perera", "Asha Silva", "Ravi Fernando"][i % 3],
      recipientPhone: "0772345678",
      dropoffAddress: `18 Sample Lane, ${zones[i % 3].name}`,
      zoneId: zones[i % 3].id,
      description: "Clothing and accessories",
      cod: i === 5 ? 2500 : 0,
    };
    const history: HistoryEntry[] = [
      { status: "CREATED", timestamp: now, note: "Booking confirmed" },
    ];
    if (status !== "CREATED") {
      for (const stage of [
        "PICKED_UP",
        "IN_TRANSIT",
        "OUT_FOR_DELIVERY",
        "DELIVERED",
      ] as ParcelStatus[]) {
        if (status === "FAILED") break;
        history.push({ status: stage, timestamp: now, note: "" });
        if (stage === status) break;
      }
      if (status === "FAILED")
        history.push({ status, timestamp: now, note: "Recipient unavailable" });
    }
    return {
      ...booking,
      id: `sample-${i + 1}`,
      trackingNumber: `SD-DEMO-${1001 + i}`,
      ownerId: i % 2 ? "preview-merchant" : "preview-customer",
      ownerRole: i % 2 ? "MERCHANT" : "CUSTOMER",
      status,
      amount: estimate(booking, zones),
      createdAt: now,
      courierId: i >= 2 ? "preview-courier" : undefined,
      assignedDate: i >= 2 ? day() : undefined,
      history,
      recipientProof:
        status === "DELIVERED" ? booking.recipientName : undefined,
      failureReason: status === "FAILED" ? "Recipient unavailable" : undefined,
    };
  });
  return {
    version: 1,
    parcels,
    zones,
    couriers: [
      {
        id: "preview-courier",
        name: "Courier Preview",
        available: true,
        capacity: 12,
      },
      {
        id: "preview-courier-2",
        name: "Kavindi Jayasuriya",
        available: true,
        capacity: 8,
      },
    ],
    notices: [],
    integrations: {},
    webhooks: {},
    profiles: {},
    addresses: {},
  };
}
