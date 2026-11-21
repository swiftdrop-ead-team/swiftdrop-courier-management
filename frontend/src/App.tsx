import React from 'react';
import { BrowserRouter, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './hooks/useAuth';
import { AppRoutes } from './routes/AppRoutes';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

function DesignScope({ children }: { children: React.ReactNode }) {
  const { pathname } = useLocation();
  return <div className={pathname.includes('/dashboard') ? undefined : 'soft-public'}>{children}</div>;
}

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <DesignScope><AppRoutes /></DesignScope>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
};
