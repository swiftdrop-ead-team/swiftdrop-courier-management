import { z } from "zod";
import {
  estimate,
  bookingSchema,
  seedPreview,
  statuses,
  validateTransition,
  type Booking,
  type ParcelRecord,
  type PreviewState,
  type Quote,
  type ParcelStatus,
} from "./model";
import type { UserRole } from "../../types/auth";

const key = "swiftdrop:operations-preview:v1";
const listeners = new Set<() => void>();
const recordSchema = bookingSchema
  .omit({ pickupDate: true })
  .extend({
    pickupDate: z.string(),
    id: z.string(),
    trackingNumber: z.string(),
    ownerId: z.string(),
    ownerRole: z.enum(["CUSTOMER", "MERCHANT", "COURIER", "ADMIN"]),
    status: z.enum(statuses),
    amount: z.number(),
    createdAt: z.string(),
    history: z.array(
      z.object({
        status: z.enum(statuses),
        timestamp: z.string(),
        note: z.string(),
      }),
    ),
  })
  .passthrough();
const storedSchema = z.object({
  version: z.literal(1),
  parcels: z.array(recordSchema),
  couriers: z.array(
    z.object({
      id: z.string(),
      name: z.string(),
      available: z.boolean(),
      capacity: z.number().positive(),
    }),
  ),
  zones: z.array(
    z.object({
      id: z.string(),
      name: z.string(),
      base: z.number().nonnegative(),
      perKg: z.number().nonnegative(),
      expressMultiplier: z.number().min(1),
      active: z.boolean(),
    }),
  ),
  notices: z.array(
    z.object({
      id: z.string(),
      ownerId: z.string(),
      text: z.string(),
      read: z.boolean(),
      timestamp: z.string(),
    }),
  ),
  integrations: z.record(
    z.object({
      apiKey: z.string(),
      clientId: z.string(),
      webhookUrl: z.string(),
      enabled: z.boolean(),
    }),
  ),
  webhooks: z.record(
    z.array(
      z.object({
        id: z.string(),
        trackingNumber: z.string(),
        status: z.enum(statuses),
        timestamp: z.string(),
        result: z.enum(["PENDING", "DELIVERED", "FAILED"]),
        attempts: z.number(),
      }),
    ),
  ),
  profiles: z.record(
    z.object({
      fullName: z.string(),
      email: z.string(),
      phone: z.string(),
      businessName: z.string(),
      address: z.string(),
      emailNotifications: z.boolean(),
    }),
  ),
  addresses: z.record(
    z.array(
      z.object({ id: z.string(), label: z.string(), address: z.string() }),
    ),
  ),
});
let state: PreviewState | undefined;
let persistenceError = "";
export function getPreview(): PreviewState {
  if (state) return state;
  try {
    const value = sessionStorage.getItem(key);
    if (value) {
      const result = storedSchema.safeParse(JSON.parse(value));
      if (result.success) state = result.data as PreviewState;
      else
        persistenceError = "Saved preview data was invalid and has been reset.";
    }
  } catch {
    persistenceError =
      "Browser storage is unavailable. Preview changes will last until this page reloads.";
  }
  return (state ||= seedPreview());
}
export const previewStorageError = () => persistenceError;
export const subscribePreview = (listener: () => void) => {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
};
export function updatePreview(change: (draft: PreviewState) => void) {
  const draft: PreviewState = JSON.parse(JSON.stringify(getPreview()));
  change(draft);
  state = draft;
  try {
    sessionStorage.setItem(key, JSON.stringify(draft));
  } catch {
    persistenceError =
      "Browser storage is unavailable. Preview changes will last until this page reloads.";
  }
  listeners.forEach((listener) => listener());
}
export function resetPreview() {
  updatePreview((draft) => Object.assign(draft, seedPreview()));
  quotes.clear();
}
const quotes = new Map<string, { booking: string; quote: Quote }>();
export function previewQuote(booking: Booking): Quote {
  const quote = {
    id: crypto.randomUUID(),
    amount: estimate(booking, getPreview().zones),
    expiresAt: new Date(Date.now() + 300000).toISOString(),
  };
  quotes.set(quote.id, { booking: JSON.stringify(booking), quote });
  return quote;
}
export function previewBook(
  booking: Booking,
  quoteId: string,
  ownerId: string,
  ownerRole: UserRole,
): ParcelRecord {
  return previewBookMany([booking], [quoteId], ownerId, ownerRole)[0];
}
export function previewBookMany(
  bookings: Booking[],
  quoteIds: string[],
  ownerId: string,
  ownerRole: UserRole,
): ParcelRecord[] {
  if (
    bookings.length !== quoteIds.length ||
    new Set(quoteIds).size !== quoteIds.length
  )
    throw new Error("Each order must have its own delivery quote.");
  const parcels = bookings.map((booking, index) => {
    const quoteId = quoteIds[index];
    bookingSchema.parse(booking);
    const entry = quotes.get(quoteId);
    if (
      !entry ||
      entry.booking !== JSON.stringify(booking) ||
      new Date(entry.quote.expiresAt).getTime() < Date.now()
    )
      throw new Error(
        "The quote expired or the booking changed. Request a new estimate.",
      );
    const now = new Date().toISOString();
    const parcel: ParcelRecord = {
      ...booking,
      id: crypto.randomUUID(),
      trackingNumber: `SD-${crypto.randomUUID().replace(/-/g, "").slice(0, 12).toUpperCase()}`,
      ownerId,
      ownerRole,
      status: "CREATED",
      createdAt: now,
      amount: entry.quote.amount,
      history: [
        { status: "CREATED", timestamp: now, note: "Booking confirmed" },
      ],
    };
    return parcel;
  });
  updatePreview((draft) => {
    parcels.forEach((parcel) => {
      draft.parcels.unshift(parcel);
      draft.notices.unshift({
        id: crypto.randomUUID(),
        ownerId,
        text: `${parcel.trackingNumber}: Booking confirmed`,
        read: false,
        timestamp: parcel.createdAt,
      });
    });
  });
  quoteIds.forEach((id) => quotes.delete(id));
  return parcels;
}
export function previewStatus(
  id: string,
  status: ParcelStatus,
  note: string,
  proof: string,
  courierId?: string,
) {
  updatePreview((draft) => {
    const parcel = draft.parcels.find((p) => p.id === id);
    if (!parcel || (courierId && parcel.courierId !== courierId))
      throw new Error("This parcel is not assigned to your account.");
    validateTransition(parcel, status, note, proof);
    parcel.status = status;
    const timestamp = new Date().toISOString();
    parcel.history.push({ status, timestamp, note });
    if (status === "DELIVERED") parcel.recipientProof = proof.trim();
    if (status === "FAILED") parcel.failureReason = note.trim();
    draft.notices.unshift({
      id: crypto.randomUUID(),
      ownerId: parcel.ownerId,
      text: `${parcel.trackingNumber}: ${status.replace(/_/g, " ")}`,
      read: false,
      timestamp,
    });
    const integration = draft.integrations[parcel.ownerId];
    if (
      parcel.ownerRole === "MERCHANT" &&
      integration?.enabled &&
      integration.webhookUrl
    )
      (draft.webhooks[parcel.ownerId] ||= []).unshift({
        id: crypto.randomUUID(),
        trackingNumber: parcel.trackingNumber,
        status,
        timestamp,
        result: "PENDING",
        attempts: 0,
      });
  });
}
