import { api, setAccessToken } from './api';
import { AuthResponse, User, PendingMerchant } from '../types/auth';

export interface LoginParams {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterCustomerParams {
  fullName: string;
  email: string;
  phoneNumber?: string;
  password: string;
  confirmPassword: string;
  acceptTerms: boolean;
}

export interface RegisterMerchantParams {
  businessName: string;
  contactPerson: string;
  email: string;
  businessAddress: string;
  phoneNumber: string;
  password: string;
  confirmPassword: string;
}

export const authService = {
  async login(params: LoginParams): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', params);
    if (response.data.accessToken) {
      setAccessToken(response.data.accessToken);
    }
    return response.data;
  },

  async registerCustomer(params: RegisterCustomerParams): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/register', params);
    if (response.data.accessToken) {
      setAccessToken(response.data.accessToken);
    }
    return response.data;
  },

  async registerMerchant(params: RegisterMerchantParams): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/merchant/register', params);
    if (response.data.accessToken) {
      setAccessToken(response.data.accessToken);
    }
    return response.data;
  },

  async getCurrentUser(): Promise<User> {
    const response = await api.get<User>('/auth/me');
    return response.data;
  },

  async refreshToken(): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/refresh');
    if (response.data.accessToken) {
      setAccessToken(response.data.accessToken);
    }
    return response.data;
  },

  async logout(): Promise<void> {
    try {
      await api.post('/auth/logout');
    } finally {
      setAccessToken(null);
    }
  },

  async forgotPassword(email: string): Promise<{ success: boolean; message: string }> {
    const response = await api.post<{ success: boolean; message: string }>('/auth/forgot-password', { email });
    return response.data;
  },

  async resetPassword(token: string, newPassword: string, confirmPassword: string): Promise<{ success: boolean; message: string }> {
    const response = await api.post<{ success: boolean; message: string }>('/auth/reset-password', {
      token,
      newPassword,
      confirmPassword,
    });
    return response.data;
  },
};

export const adminService = {
  async getAllUsers(): Promise<User[]> {
    const response = await api.get<User[]>('/admin/users');
    return response.data;
  },

  async getPendingMerchants(): Promise<PendingMerchant[]> {
    const response = await api.get<PendingMerchant[]>('/admin/merchants/pending');
    return response.data;
  },

  async approveMerchant(recordId: string, notes?: string): Promise<{ success: boolean; message: string }> {
    const response = await api.patch<{ success: boolean; message: string }>(`/admin/merchants/${recordId}/approve`, {
      notes,
    });
    return response.data;
  },

  async rejectMerchant(recordId: string, notes?: string): Promise<{ success: boolean; message: string }> {
    const response = await api.patch<{ success: boolean; message: string }>(`/admin/merchants/${recordId}/reject`, {
      notes,
    });
    return response.data;
  },
};
