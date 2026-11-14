import { api } from "../../services/api";
import { authService } from "../../services/authService";
import {
  day,
  type Booking,
  type Courier,
  type Integration,
  type Notice,
  type ParcelRecord,
  type ParcelStatus,
  type Profile,
  type Quote,
  type SavedAddress,
  type WebhookAttempt,
  type Zone,
} from "./model";

interface Envelope<T> {
  success: boolean;
  data: T;
  message?: string;
}
interface Page<T> {
  content: T[];
  totalPages: number;
}
export interface BackendParcel {
  pickupDate?: string;
  codAmount?: number;
  id: string;
  trackingNumber: string;
  senderId: string;
  senderType: "CUSTOMER" | "MERCHANT";
  senderName: string;
  senderPhone: string;
  senderEmail?: string;
  pickupAddress: string;
  recipientName: string;
  recipientPhone: string;
  deliveryAddress: string;
  deliveryCity: string;
  weightKg: number;
  lengthCm: number;
  widthCm: number;
  heightCm: number;
  description: string;
  deliveryType: string;
  status: string;
  deliveryFee: number;
  currency: string;
  assignedCourierId?: string;
  failureReason?: string;
  specialInstructions?: string;
  createdAt: string;
  statusHistory?: { newStatus: string; timestamp: string; notes?: string }[];
}
interface BackendCourier {
  id: number;
  userId: string;
  fullName: string;
  status: string;
  maxActiveDeliveries: number;
  availableForAssignment: boolean;
}
interface Assignment {
  id: number;
  parcelId: string;
  courierId: number;
  trackingNumber: string;
  status: string;
  assignedAt: string;
  failureReason?: string;
  notes?: string;
  proofOfDelivery?: { recipientName: string };
}
export const backendTime = (value: string) =>
  /(?:Z|[+-]\d{2}:\d{2})$/.test(value) ? value : `${value}+05:30`;
export function parcelStatus(value: string): ParcelStatus {
  if (value === "ASSIGNED") return "CREATED";
  if (value === "RECEIVED_AT_HUB" || value === "SORTED") return "IN_TRANSIT";
  if (
    ![
      "CREATED",
      "PICKED_UP",
      "IN_TRANSIT",
      "OUT_FOR_DELIVERY",
      "DELIVERED",
      "FAILED",
      "CANCELLED",
    ].includes(value)
  )
    throw new Error(`Unsupported delivery status: ${value}`);
  return value as ParcelStatus;
}
export function mapParcel(p: BackendParcel): ParcelRecord {
  return {
    id: p.id,
    trackingNumber: p.trackingNumber,
    ownerId: p.senderId,
    ownerRole: p.senderType,
    senderName: p.senderName,
    senderPhone: p.senderPhone || "",
    pickupAddress: p.pickupAddress,
    recipientName: p.recipientName,
    recipientPhone: p.recipientPhone || "",
    dropoffAddress: p.deliveryAddress,
    zoneId: p.deliveryCity || "",
    description: p.description || "",
    weight: Number(p.weightKg),
    length: Number(p.lengthCm),
    width: Number(p.widthCm),
    height: Number(p.heightCm),
    service: p.deliveryType === "EXPRESS" ? "EXPRESS" : "STANDARD",
    pickupDate: p.pickupDate || day(p.createdAt),
    instructions: p.specialInstructions || "",
    cod: Number(p.codAmount || 0),
    status: parcelStatus(p.status),
    amount: Number(p.deliveryFee),
    currency: p.currency,
    createdAt: p.createdAt,
    courierId: p.assignedCourierId,
    failureReason: p.failureReason,
    history: (p.statusHistory || []).map((h) => ({
      status: parcelStatus(h.newStatus),
      timestamp: h.timestamp,
      note: h.notes || "",
    })),
  };
}
function unwrap<T>(body: Envelope<T>): T {
  if (!body.success)
    throw new Error(body.message || "The service rejected the request.");
  return body.data;
}
function applyAssignment(
  parcel: ParcelRecord,
  assignment?: Assignment,
  couriers: BackendCourier[] = [],
): ParcelRecord {
  if (!assignment) return parcel;
  return {
    ...parcel,
    assignmentId: String(assignment.id),
    courierId:
      couriers.find((c) => c.id === assignment.courierId)?.userId ||
      String(assignment.courierId),
    assignedDate: day(backendTime(assignment.assignedAt)),
    status: parcelStatus(assignment.status),
    failureReason: assignment.failureReason || parcel.failureReason,
    recipientProof: assignment.proofOfDelivery?.recipientName,
  };
}
async function pages(
  path: string,
  signal?: AbortSignal,
): Promise<BackendParcel[]> {
  const records: BackendParcel[] = [];
  for (let page = 0; page < 1000; page++) {
    const response = (
      await api.get<Page<BackendParcel>>(path, {
        params: { page, size: 100 },
        signal,
      })
    ).data;
    if (!Array.isArray(response.content))
      throw new Error("The parcel service returned an invalid page.");
    records.push(...response.content);
    if (page + 1 >= response.totalPages) return records;
  }
  throw new Error(
    "There are too many deliveries to load. Please narrow your request.",
  );
}
export function bookingPayload(b: Booking) {
  return {
    pickupDate: b.pickupDate,
    codAmount: b.cod,
    senderName: b.senderName,
    senderPhone: b.senderPhone,
    pickupAddress: b.pickupAddress,
    recipientName: b.recipientName,
    recipientPhone: b.recipientPhone,
    deliveryAddress: b.dropoffAddress,
    deliveryCity: b.zoneId,
    weightKg: b.weight,
    lengthCm: b.length,
    widthCm: b.width,
    heightCm: b.height,
    description: b.description,
    deliveryType: b.service,
    isFragile: false,
    specialInstructions: b.instructions,
  };
}
export class UnavailableFeature extends Error {
  constructor(feature: string) {
    super(`${feature} is not supported by the current backend yet.`);
  }
}
const unavailable = async <T>(feature: string): Promise<T> => {
  throw new UnavailableFeature(feature);
};

interface MerchantHook {
  id: number;
  targetUrl: string;
  active: boolean;
}
async function merchantHooks(signal?: AbortSignal): Promise<MerchantHook[]> {
  return unwrap(
    (await api.get<Envelope<MerchantHook[]>>("/merchant/webhooks", { signal }))
      .data,
  );
}

export const deliveryApi = {
  registerCourier: async (
    userId: string,
    fullName: string,
    phoneNumber: string,
    maxActiveDeliveries: number,
    email?: string,
  ) => {
    await api.post("/dispatch/couriers", {
      userId,
      fullName,
      phoneNumber,
      maxActiveDeliveries,
      email,
      vehicleType: "MOTORBIKE",
    });
  },
  parcels: async (
    scope: string,
    signal?: AbortSignal,
  ): Promise<ParcelRecord[]> => {
    if (scope === "assigned") {
      const user = await authService.getCurrentUser();
      const courier = unwrap(
        (
          await api.get<Envelope<BackendCourier>>(
            `/dispatch/couriers/user/${encodeURIComponent(user.id)}`,
            { signal },
          )
        ).data,
      );
      const assignments = unwrap(
        (
          await api.get<Envelope<Assignment[]>>(
            `/dispatch/assignments/courier/${courier.id}`,
            { signal },
          )
        ).data,
      );
      return Promise.all(
        assignments.map(async (a) =>
          applyAssignment(
            mapParcel(
              (
                await api.get<BackendParcel>(`/parcels/${a.parcelId}`, {
                  signal,
                })
              ).data,
            ),
            a,
            [courier],
          ),
        ),
      );
    }
    const parcels = (
      await pages(scope === "all" ? "/parcels" : "/parcels/my-parcels", signal)
    ).map(mapParcel);
    if (scope !== "all") return parcels; // Customer/merchant privacy: never load a global dispatch list.
    const assignments = unwrap(
      (
        await api.get<Envelope<Assignment[]>>("/dispatch/assignments", {
          signal,
        })
      ).data,
    );
    const couriers = unwrap(
      (
        await api.get<Envelope<BackendCourier[]>>("/dispatch/couriers", {
          signal,
        })
      ).data,
    );
    return parcels.map((p) =>
      applyAssignment(
        p,
        assignments.find((a) => a.parcelId === p.id),
        couriers,
      ),
    );
  },
  quote: async (b: Booking): Promise<Quote> =>
    (await api.post<Quote>("/parcels/estimate", bookingPayload(b))).data,
  create: async (
    b: Booking,
    estimateId: string,
    idempotencyKey = estimateId,
  ): Promise<ParcelRecord> => {
    const user = await authService.getCurrentUser();
    if (user.roles.includes("MERCHANT")) {
      const order = unwrap(
        (
          await api.post<Envelope<{ parcelId: string }>>(
            "/merchant/orders",
            { ...bookingPayload(b), estimateId },
            { headers: { "Idempotency-Key": idempotencyKey } },
          )
        ).data,
      );
      return mapParcel(
        (await api.get<BackendParcel>(`/parcels/${order.parcelId}`)).data,
      );
    }
    return mapParcel(
      (
        await api.post<BackendParcel>("/parcels", {
          ...bookingPayload(b),
          estimateId,
        })
      ).data,
    );
  },
  cancel: async (id: string) => {
    await api.post(`/parcels/${encodeURIComponent(id)}/cancel`, {
      reason: "Cancelled by the sender",
    });
  },
  status: async (
    id: string,
    status: ParcelStatus,
    note: string,
    recipientName: string,
  ) => {
    const user = await authService.getCurrentUser();
    const courier = unwrap(
      (
        await api.get<Envelope<BackendCourier>>(
          `/dispatch/couriers/user/${user.id}`,
        )
      ).data,
    );
    const assignments = unwrap(
      (
        await api.get<Envelope<Assignment[]>>(
          `/dispatch/assignments/courier/${courier.id}`,
        )
      ).data,
    );
    const assignment = assignments.find((a) => a.parcelId === id);
    if (!assignment)
      throw new Error("This delivery is not assigned to your courier profile.");
    if (status === "DELIVERED") {
      if (!recipientName.trim()) throw new Error("Enter recipient proof.");
      await api.post(`/dispatch/assignments/${assignment.id}/proof`, {
        recipientName,
        deliveryNotes: note,
      });
    } else
      await api.patch(`/dispatch/assignments/${assignment.id}/status`, {
        status,
        notes: note,
        failureReason: status === "FAILED" ? note : undefined,
      });
  },
  couriers: async (signal?: AbortSignal): Promise<Courier[]> => {
    const user = await authService.getCurrentUser();
    const records = user.roles.includes("ADMIN")
      ? unwrap(
          (
            await api.get<Envelope<BackendCourier[]>>("/dispatch/couriers", {
              signal,
            })
          ).data,
        )
      : [
          unwrap(
            (
              await api.get<Envelope<BackendCourier>>(
                `/dispatch/couriers/user/${encodeURIComponent(user.id)}`,
                { signal },
              )
            ).data,
          ),
        ];
    return records
      .filter((c) => /^[0-9a-f]{8}-[0-9a-f-]{27}$/i.test(c.userId))
      .map((c) => ({
        id: c.userId,
        dispatchId: String(c.id),
        name: c.fullName,
        available: c.status !== "OFFLINE",
        capacity: c.maxActiveDeliveries,
      }));
  },
  availability: async (available: boolean) => {
    const user = await authService.getCurrentUser();
    const courier = unwrap(
      (
        await api.get<Envelope<BackendCourier>>(
          `/dispatch/couriers/user/${user.id}`,
        )
      ).data,
    );
    await api.patch(`/dispatch/couriers/${courier.id}/status`, {
      status: available ? "AVAILABLE" : "OFFLINE",
    });
  },
  assign: async (parcelId: string, courierId: string, assignedDate: string) => {
    if (assignedDate !== day())
      throw new Error(
        "The dispatch backend currently supports assignment for today only.",
      );
    const p = (
      await api.get<BackendParcel>(`/parcels/${encodeURIComponent(parcelId)}`)
    ).data;
    const courier = unwrap(
      (
        await api.get<Envelope<BackendCourier>>(
          `/dispatch/couriers/user/${encodeURIComponent(courierId)}`,
        )
      ).data,
    );
    await api.post("/dispatch/assignments", {
      parcelId,
      courierId: courier.id,
      trackingNumber: p.trackingNumber,
      pickupAddress: p.pickupAddress,
      deliveryAddress: p.deliveryAddress,
      recipientName: p.recipientName,
      recipientPhone: p.recipientPhone,
      notificationEmail: p.senderEmail,
      notificationName: p.senderName,
      notes: p.specialInstructions,
    });
  },
  profile: async (signal?: AbortSignal): Promise<Profile> => {
    const [profile, preferences] = await Promise.all([
      api.get<Profile>("/auth/profile", { signal }),
      api.get<{ enabled: boolean }>("/notifications/preferences", { signal }),
    ]);
    const user = await authService.getCurrentUser();
    const business = user.roles.includes("MERCHANT")
      ? unwrap(
          (
            await api.get<
              Envelope<{
                businessName: string;
                contactPerson: string;
                phoneNumber: string;
                businessAddress: string;
              }>
            >("/merchant/profile", { signal })
          ).data,
        )
      : null;
    return {
      ...profile.data,
      ...(business
        ? {
            businessName: business.businessName,
            fullName: business.contactPerson,
            phone: business.phoneNumber,
            address: business.businessAddress,
          }
        : {}),
      emailNotifications: preferences.data.enabled,
    };
  },
  saveProfile: async (profile: Profile) => {
    const user = await authService.getCurrentUser();
    if (user.roles.includes("MERCHANT"))
      await api.put("/merchant/profile", {
        businessName: profile.businessName,
        contactPerson: profile.fullName,
        phoneNumber: profile.phone,
        businessAddress: profile.address,
      });
    await api.put("/auth/profile", profile);
    await api.put("/notifications/preferences", {
      enabled: profile.emailNotifications,
    });
  },
  addresses: async (signal?: AbortSignal): Promise<SavedAddress[]> =>
    (await api.get<SavedAddress[]>("/auth/addresses", { signal })).data,
  saveAddresses: async (addresses: SavedAddress[]) => {
    await api.put("/auth/addresses", addresses);
  },
  zones: (_signal?: AbortSignal) => unavailable<Zone[]>("Zone management"),
  saveZone: (_zone: Zone) => unavailable<void>("Zone management"),
  notices: async (signal?: AbortSignal): Promise<Notice[]> => {
    const user = await authService.getCurrentUser();
    interface Log {
      id: number;
      subject: string;
      createdAt: string;
      readAt?: string;
    }
    return unwrap(
      (await api.get<Envelope<Log[]>>("/notifications/history", { signal }))
        .data,
    ).map((log) => ({
      id: String(log.id),
      ownerId: user.id,
      text: log.subject,
      read: Boolean(log.readAt),
      timestamp: backendTime(log.createdAt),
    }));
  },
  readNotice: async (id: string) => {
    await api.patch(`/notifications/${encodeURIComponent(id)}/read`);
  },
  integration: async (signal?: AbortSignal): Promise<Integration> => {
    const [profile, keys, hooks] = await Promise.all([
      api.get<Envelope<{ id: string }>>("/merchant/profile", { signal }),
      api.get<Envelope<{ revokedAt?: string }[]>>("/merchant/api-keys", {
        signal,
      }),
      merchantHooks(signal),
    ]);
    const hook = hooks.find((h) => h.active) || hooks[0];
    return {
      apiKey: "",
      hasKey: unwrap(keys.data).some((k) => !k.revokedAt),
      clientId: unwrap(profile.data).id,
      webhookUrl: hook?.targetUrl || "",
      enabled: hook?.active || false,
    };
  },
  rotateKey: async (): Promise<Integration> => {
    const key = unwrap(
      (
        await api.post<Envelope<{ key: string }>>("/merchant/api-keys/rotate", {
          scopes: ["orders:read", "orders:write", "dashboard:read"],
        })
      ).data,
    );
    return { ...(await deliveryApi.integration()), apiKey: key.key };
  },
  webhook: async (
    url: string,
    enabled: boolean,
  ): Promise<{ signingSecret?: string }> => {
    const hooks = await merchantHooks();
    const current = hooks.find((h) => h.active) || hooks[0];
    if (!enabled) {
      if (current) await api.delete(`/merchant/webhooks/${current.id}`);
      return {};
    }
    const body = { targetUrl: url, eventTypes: ["parcel.status.changed"] };
    return unwrap(
      (current
        ? await api.put<Envelope<{ signingSecret?: string }>>(
            `/merchant/webhooks/${current.id}`,
            body,
          )
        : await api.post<Envelope<{ signingSecret?: string }>>(
            "/merchant/webhooks",
            body,
          )
      ).data,
    );
  },
  attempts: async (signal?: AbortSignal): Promise<WebhookAttempt[]> => {
    const hooks = await merchantHooks(signal);
    const deliveries = await Promise.all(
      hooks.map(async (hook) =>
        unwrap(
          (
            await api.get<
              Envelope<
                {
                  id: number;
                  webhookId: number;
                  status: "PENDING" | "DELIVERED" | "FAILED";
                  trackingNumber: string;
                  parcelStatus: string;
                  createdAt: string;
                  attemptCount: number;
                }[]
              >
            >(`/merchant/webhooks/${hook.id}/deliveries`, { signal })
          ).data,
        ),
      ),
    );
    return deliveries
      .flat()
      .map((d) => ({
        id: `${d.webhookId}:${d.id}`,
        trackingNumber: d.trackingNumber || "Status update",
        status: d.parcelStatus as ParcelStatus,
        timestamp: backendTime(d.createdAt),
        result: d.status,
        attempts: d.attemptCount,
      }))
      .sort((a, b) => b.timestamp.localeCompare(a.timestamp));
  },
  retry: async (id: string) => {
    const [hook, delivery] = id.split(":");
    if (!/^\d+$/.test(hook) || !/^\d+$/.test(delivery))
      throw new Error("Invalid webhook delivery");
    await api.post(
      `/merchant/webhooks/${hook}/deliveries/${delivery}/redeliver`,
    );
  },
};
export function errorMessage(error: unknown) {
  const response = (
    error as { response?: { status?: number; data?: { message?: string } } }
  )?.response;
  if ([502, 503].includes(response?.status || 0))
    return "This service is temporarily unavailable. Please try again shortly.";
  if (response?.status === 403)
    return "You do not have permission to perform this action.";
  if (response?.status === 404)
    return "The requested record or courier profile was not found.";
  return (
    response?.data?.message ||
    (error instanceof Error
      ? error.message
      : "The request failed. Please try again.")
  );
}
