export type DeliveryStatus = 'Assigned' | 'Picked up' | 'Out for delivery' | 'Delivered' | 'Failed';
export interface CourierDelivery {
  id: string; recipient: string; address: string; city: string; window: string;
  description: string; weight: number; cod: number; instructions: string;
  status: DeliveryStatus; proof?: string; completedAt?: string;
}
export const previewDeliveries: CourierDelivery[] = [
  { id: 'DEMO-1001', recipient: 'Nimal Perera', address: '24 Sample Lane', city: 'Colombo 03', window: '09:00 - 11:00', description: 'Clothing / 1 parcel', weight: 1.2, cod: 0, instructions: 'Ask for the recipient at reception.', status: 'Assigned' },
  { id: 'DEMO-1002', recipient: 'Asha Silva', address: '18 Example Road', city: 'Colombo 05', window: '11:00 - 13:00', description: 'Home accessories / 2 parcels', weight: 2.5, cod: 3500, instructions: 'Collect payment before handover.', status: 'Assigned' },
  { id: 'DEMO-1003', recipient: 'Ravi Fernando', address: '6 Demo Avenue', city: 'Nugegoda', window: '13:00 - 15:00', description: 'Books / 1 parcel', weight: 0.8, cod: 0, instructions: 'Do not leave the parcel unattended.', status: 'Picked up' },
];
export const isCompleted = (item: CourierDelivery) => ['Delivered', 'Failed'].includes(item.status);
export const lkr = (value: number) => new Intl.NumberFormat('en-LK', { style: 'currency', currency: 'LKR', maximumFractionDigits: 0 }).format(value);
