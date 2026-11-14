export type ShipmentStatus = 'Pending' | 'Assigned' | 'Picked up' | 'In transit' | 'Out for delivery' | 'Delivered' | 'Failed' | 'Cancelled' | 'Returned';

export interface Contact {
  name: string;
  phone: string;
  email?: string;
  address: string;
  city: string;
  postalCode: string;
}

export interface Parcel {
  description: string;
  category: string;
  quantity: number;
  weight: number;
  length?: number;
  width?: number;
  height?: number;
  specialHandling?: string;
}

export interface Delivery {
  pickupDate: string;
  preferredTime: string;
  deliveryType: 'Standard' | 'Express';
  instructions: string;
}

export interface Shipment {
  id: string;
  trackingNumber: string;
  status: ShipmentStatus;
  sender: Contact;
  receiver: Contact;
  parcel: Parcel;
  delivery: Delivery;
  amount: number;
  createdAt: string;
  estimatedDelivery: string;
  statusHistory: { status: ShipmentStatus; timestamp: string; note?: string }[];
}

export interface ManifestItem {
  trackingNumber: string;
  recipient: string;
  destination: string;
  status: ShipmentStatus;
  bookedAt: string;
  amount: number;
}

export interface ManifestOrder {
  id: string;
  fileName: string;
  uploadedAt: string;
  status: string;
  processedCount: number;
  totalParcels: number;
}

export interface Notification {
  id: string;
  title: string;
  message: string;
  category: 'delivery' | 'account' | 'system';
  read: boolean;
  createdAt: string;
}

// No fabricated merchant records while the backend is reserved for a future phase.
export const demoManifestItems: ManifestItem[] = [];
export const demoManifestOrders: ManifestOrder[] = [];

export const currency = (amount: number) => new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR' }).format(amount);
export function dateLabel(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString('en-LK');
}
export function dateTimeLabel(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('en-LK');
}

type StatusColor = 'warning' | 'info' | 'success' | 'error' | 'secondary';
export function shipmentStatusColor(status: ShipmentStatus): StatusColor {
  if (status === 'Delivered') return 'success';
  if (['Failed', 'Cancelled', 'Returned'].includes(status)) return 'error';
  return status === 'Pending' ? 'warning' : 'info';
}
export function manifestOrderStatusColor(status: string): StatusColor {
  if (status === 'Completed' || status === 'Processed') return 'success';
  if (status === 'Failed') return 'error';
  return 'info';
}
export const trackingSteps: { status: ShipmentStatus; label: string }[] = ['Pending', 'Assigned', 'Picked up', 'In transit', 'Out for delivery', 'Delivered'].map((status) => ({ status: status as ShipmentStatus, label: status }));
export const statusIndex = (status: ShipmentStatus) => trackingSteps.findIndex((step) => step.status === status);
