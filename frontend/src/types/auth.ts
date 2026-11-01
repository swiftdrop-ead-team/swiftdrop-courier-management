export type UserRole = 'CUSTOMER' | 'MERCHANT' | 'COURIER' | 'ADMIN';

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export type MerchantApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface User {
  id: string;
  email: string;
  fullName: string;
  phoneNumber?: string;
  roles: UserRole[];
  status: UserStatus;
  merchantApprovalStatus?: MerchantApprovalStatus | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface AuthResponse {
  success: boolean;
  message?: string;
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface PendingMerchant {
  id: string;
  userId: string;
  businessName: string;
  contactPerson: string;
  email: string;
  phoneNumber: string;
  businessAddress: string;
  approvalStatus: MerchantApprovalStatus;
  notes?: string;
  registeredAt: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}
