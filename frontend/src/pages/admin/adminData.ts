import type { PendingMerchant, User } from '../../types/auth';

export interface AdminActivity {
  id: string;
  merchant: string;
  decision: 'APPROVED' | 'REJECTED';
  notes: string;
  timestamp: string;
  actor: string;
}

export function createAdminPreview() {
  const users: User[] = [
    { id: 'demo-admin', fullName: 'Admin Preview', email: 'admin@example.com', roles: ['ADMIN'], status: 'ACTIVE', createdAt: '2026-10-01T04:00:00Z' },
    { id: 'demo-merchant-1', fullName: 'Asha Silva', email: 'asha@example.com', phoneNumber: '0771234501', roles: ['MERCHANT'], status: 'ACTIVE', merchantApprovalStatus: 'PENDING', createdAt: '2026-11-10T04:00:00Z' },
    { id: 'demo-merchant-2', fullName: 'Nimal Perera', email: 'nimal@example.com', roles: ['MERCHANT'], status: 'ACTIVE', merchantApprovalStatus: 'PENDING', createdAt: '2026-11-11T04:00:00Z' },
    { id: 'demo-merchant-3', fullName: 'Ravi Fernando', email: 'ravi@example.com', roles: ['MERCHANT'], status: 'INACTIVE', merchantApprovalStatus: 'PENDING', createdAt: '2026-11-11T06:00:00Z' },
    { id: 'demo-courier', fullName: 'Kavindi Jayasuriya', email: 'kavindi@example.com', roles: ['COURIER'], status: 'ACTIVE', createdAt: '2026-10-15T04:00:00Z' },
    { id: 'demo-customer', fullName: 'Dilan Senanayake', email: 'dilan@example.com', roles: ['CUSTOMER'], status: 'SUSPENDED', createdAt: '2026-10-20T04:00:00Z' },
  ];
  const merchants: PendingMerchant[] = [
    { id: 'demo-application-1', userId: 'demo-merchant-1', businessName: 'Lanka Craft Studio', contactPerson: 'Asha Silva', email: 'asha@example.com', phoneNumber: '0771234501', businessAddress: '24 Sample Lane, Colombo 03', approvalStatus: 'PENDING', registeredAt: '2026-11-10T04:00:00Z' },
    { id: 'demo-application-2', userId: 'demo-merchant-2', businessName: 'Green Basket', contactPerson: 'Nimal Perera', email: 'nimal@example.com', phoneNumber: '0771234502', businessAddress: '18 Example Road, Nugegoda', approvalStatus: 'PENDING', registeredAt: '2026-11-11T04:00:00Z' },
    { id: 'demo-application-3', userId: 'demo-merchant-3', businessName: 'Paper & Parcel', contactPerson: 'Ravi Fernando', email: 'ravi@example.com', phoneNumber: '0771234503', businessAddress: '6 Demo Avenue, Kandy', approvalStatus: 'PENDING', registeredAt: '2026-11-11T06:00:00Z' },
  ];
  return { users, merchants };
}

export function dateTime(value?: string) {
  if (!value || Number.isNaN(new Date(value).getTime())) return 'Not available';
  return new Date(value).toLocaleString('en-LK', { timeZone: 'Asia/Colombo', dateStyle: 'medium', timeStyle: 'short' });
}

export function csvDocument(rows: (string | number)[][]) {
  return '\uFEFF' + rows.map((row) => row.map((value) => {
    let text = String(value);
    if (/^\s*[=+@-]/.test(text)) text = `'${text}`;
    return `"${text.replace(/"/g, '""')}"`;
  }).join(',')).join('\r\n');
}

export function downloadCsv(filename: string, rows: (string | number)[][]) {
  const url = URL.createObjectURL(new Blob([csvDocument(rows)], { type: 'text/csv;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url; link.download = filename; link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
