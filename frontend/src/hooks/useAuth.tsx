import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { User, UserRole, MerchantApprovalStatus } from '../types/auth';
import { authService, LoginParams, RegisterCustomerParams, RegisterMerchantParams } from '../services/authService';
import { setAccessToken } from '../services/api';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (params: LoginParams) => Promise<User>;
  registerCustomer: (params: RegisterCustomerParams) => Promise<User>;
  registerMerchant: (params: RegisterMerchantParams) => Promise<User>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  hasRole: (role: UserRole) => boolean;
  getDefaultDashboardRoute: (roles?: UserRole[]) => string;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const getDefaultDashboardRoute = useCallback((roles?: UserRole[]): string => {
    const userRoles = roles || user?.roles || [];
    if (userRoles.includes('ADMIN')) return '/admin/dashboard';
    if (userRoles.includes('MERCHANT')) return '/merchant/dashboard';
    if (userRoles.includes('COURIER')) return '/courier/dashboard';
    return '/customer/dashboard';
  }, [user]);

  const refreshUser = useCallback(async () => {
    try {
      const currentUser = await authService.getCurrentUser();
      setUser(currentUser);
    } catch {
      setUser(null);
      setAccessToken(null);
    }
  }, []);

  useEffect(() => {
    // Attempt silent session restore on load
    const initAuth = async () => {
      try {
        await refreshUser();
      } finally {
        setIsLoading(false);
      }
    };

    initAuth();
  }, [refreshUser]);

  const login = async (params: LoginParams): Promise<User> => {
    setIsLoading(true);
    try {
      const response = await authService.login(params);
      setUser(response.user);
      return response.user;
    } finally {
      setIsLoading(false);
    }
  };

  const registerCustomer = async (params: RegisterCustomerParams): Promise<User> => {
    setIsLoading(true);
    try {
      const response = await authService.registerCustomer(params);
      setUser(response.user);
      return response.user;
    } finally {
      setIsLoading(false);
    }
  };

  const registerMerchant = async (params: RegisterMerchantParams): Promise<User> => {
    setIsLoading(true);
    try {
      const response = await authService.registerMerchant(params);
      setUser(response.user);
      return response.user;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    try {
      await authService.logout();
    } finally {
      setUser(null);
    }
  };

  const hasRole = (role: UserRole): boolean => {
    return user?.roles?.includes(role) ?? false;
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        registerCustomer,
        registerMerchant,
        logout,
        refreshUser,
        hasRole,
        getDefaultDashboardRoute,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
