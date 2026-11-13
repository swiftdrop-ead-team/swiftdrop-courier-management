import { api } from "../../services/api";
import type {
  Booking,
  Courier,
  Integration,
  Notice,
  ParcelRecord,
  ParcelStatus,
  Profile,
  PublicTracking,
  Quote,
  SavedAddress,
  WebhookAttempt,
  Zone,
} from "./model";

// Proposed v1 contracts for the domain-service owners; see docs/api-contracts/frontend-delivery-v1.md.
export const deliveryApi = {
  parcels: async (scope: string, signal?: AbortSignal) =>
    (await api.get<ParcelRecord[]>("/parcels", { params: { scope }, signal }))
      .data,
  quote: async (booking: Booking) =>
    (await api.post<Quote>("/pricing/quotes", booking)).data,
  create: async (booking: Booking, quoteId: string) =>
    (
      await api.post<ParcelRecord>(
        "/parcels",
        { ...booking, quoteId },
        { headers: { "Idempotency-Key": quoteId } },
      )
    ).data,
  cancel: async (id: string) => {
    await api.patch(`/parcels/${encodeURIComponent(id)}/cancel`);
  },
  status: async (
    id: string,
    status: ParcelStatus,
    note: string,
    recipientName: string,
  ) => {
    await api.patch(`/parcels/${encodeURIComponent(id)}/status`, {
      status,
      note,
      recipientName,
    });
  },
  couriers: async (signal?: AbortSignal) =>
    (await api.get<Courier[]>("/dispatch/couriers", { signal })).data,
  availability: async (available: boolean) => {
    await api.patch("/dispatch/couriers/me/availability", { available });
  },
  assign: async (parcelId: string, courierId: string, assignedDate: string) => {
    await api.post("/dispatch/assignments", {
      parcelId,
      courierId,
      assignedDate,
    });
  },
  zones: async (signal?: AbortSignal) =>
    (await api.get<Zone[]>("/pricing/zones", { signal })).data,
  saveZone: async (zone: Zone) => {
    await api.put(`/pricing/zones/${encodeURIComponent(zone.id)}`, zone);
  },
  notices: async (signal?: AbortSignal) =>
    (await api.get<Notice[]>("/notifications", { signal })).data,
  readNotice: async (id: string) => {
    await api.patch(`/notifications/${encodeURIComponent(id)}/read`);
  },
  integration: async (signal?: AbortSignal) =>
    (await api.get<Integration>("/merchants/me/integration", { signal })).data,
  rotateKey: async () =>
    (await api.post<Integration>("/merchants/me/integration/rotate")).data,
  webhook: async (webhookUrl: string, enabled: boolean) => {
    await api.put("/merchants/me/webhook", { webhookUrl, enabled });
  },
  attempts: async (signal?: AbortSignal) =>
    (
      await api.get<WebhookAttempt[]>("/merchants/me/webhook/attempts", {
        signal,
      })
    ).data,
  retry: async (id: string) => {
    await api.post(
      `/merchants/me/webhook/attempts/${encodeURIComponent(id)}/retry`,
    );
  },
  profile: async (signal?: AbortSignal) =>
    (await api.get<Profile>("/auth/me/profile", { signal })).data,
  saveProfile: async (profile: Profile) => {
    await api.put("/auth/me/profile", profile);
  },
  addresses: async (signal?: AbortSignal) =>
    (await api.get<SavedAddress[]>("/auth/me/addresses", { signal })).data,
  saveAddresses: async (addresses: SavedAddress[]) => {
    await api.put("/auth/me/addresses", { addresses });
  },
  tracking: async (number: string, signal?: AbortSignal) =>
    (
      await api.get<PublicTracking>(`/tracking/${encodeURIComponent(number)}`, {
        signal,
      })
    ).data,
};
export function errorMessage(error: unknown) {
  const response = (
    error as { response?: { status?: number; data?: { message?: string } } }
  )?.response;
  if ([404, 502, 503].includes(response?.status || 0))
    return "This service is not available yet. Try again later or use the preview workspace.";
  if (response?.status === 403)
    return "You do not have permission to perform this action.";
  return (
    response?.data?.message ||
    (error instanceof Error
      ? error.message
      : "The request failed. Please try again.")
  );
}
