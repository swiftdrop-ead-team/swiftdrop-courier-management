import { afterEach, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from '../hooks/useAuth';
import { authService } from '../services/authService';
import { getAccessToken, setAccessToken } from '../services/api';
import type { User } from '../types/auth';

const user = { id: 'test-admin', fullName: 'Test Admin', email: 'admin@example.com', roles: ['ADMIN'], status: 'ACTIVE' } as User;
afterEach(() => { vi.restoreAllMocks(); setAccessToken(null); });
function Session() {
  const auth = useAuth();
  return <>
    <p>{auth.isLoading ? 'Restoring session' : auth.user?.fullName || 'Signed out'}</p>
    <button onClick={() => void auth.logout()}>Logout</button>
  </>;
}
it('restores a mock session and clears it when refresh expires', async () => {
  vi.spyOn(authService, 'getCurrentUser').mockResolvedValue(user);
  render(<AuthProvider><Session /></AuthProvider>);
  expect(await screen.findByText('Test Admin')).toBeInTheDocument();
  setAccessToken('test-token');
  act(() => { window.dispatchEvent(new Event('auth:expired')); });
  expect(screen.getByText('Signed out')).toBeInTheDocument();
  expect(getAccessToken()).toBeNull();
});
it('logs out an authenticated mock session', async () => {
  vi.spyOn(authService, 'getCurrentUser').mockResolvedValue(user);
  const logout = vi.spyOn(authService, 'logout').mockResolvedValue();
  render(<AuthProvider><Session /></AuthProvider>);
  await screen.findByText('Test Admin');
  fireEvent.click(screen.getByRole('button', { name: 'Logout' }));
  await waitFor(() => expect(screen.getByText('Signed out')).toBeInTheDocument());
  expect(logout).toHaveBeenCalledTimes(1);
});
it('finishes loading when no mock session exists', async () => {
  vi.spyOn(authService, 'getCurrentUser').mockRejectedValue(new Error('Unauthorized'));
  render(<AuthProvider><Session /></AuthProvider>);
  expect(await screen.findByText('Signed out')).toBeInTheDocument();
});
