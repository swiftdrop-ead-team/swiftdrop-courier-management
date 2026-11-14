import type { Contact, Delivery, Notification, Parcel, Shipment } from '../pages/merchant/merchantData';

export interface CreateShipmentPayload {
  receiver: Contact;
  parcel: Parcel;
  delivery: Delivery;
}

// These backend services are placeholders in this repository. Fail explicitly
// until their API contracts are implemented rather than simulate successful work.
export const merchantUnavailableMessage = 'Merchant operations are not available yet. The backend service has not been implemented.';
async function unavailable<T>(): Promise<T> {
  throw new Error(merchantUnavailableMessage);
}

export const shipmentService = {
  listShipments: () => unavailable<Shipment[]>(),
  createShipment: (_payload: CreateShipmentPayload) => unavailable<Shipment>(),
  cancelShipment: (_id: string) => unavailable<void>(),
};
export const trackingService = {
  trackShipment: (_trackingNumber: string) => unavailable<Shipment>(),
};
export const notificationService = {
  getNotifications: () => unavailable<Notification[]>(),
};
export const integrationService = {
  getCredentials: () => unavailable<{ apiKey: string; clientId: string }>(),
};
export const bulkUploadService = {
  uploadManifest: (_file: File) => unavailable<{ validRecords: number; invalidRecords: number }>(),
};
