import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { afterEach, expect, it, vi } from 'vitest';
import { api, getAccessToken, setAccessToken } from '../services/api';

const originalAdapter = api.defaults.adapter;
afterEach(() => {
  api.defaults.adapter = originalAdapter;
  setAccessToken(null);
  vi.restoreAllMocks();
});

function unauthorized(config: InternalAxiosRequestConfig) {
  return Promise.reject(new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, {
    config, status: 401, statusText: 'Unauthorized', headers: {}, data: {},
  }));
}

it('retries concurrent unauthorized requests only once and expires a rejected session', async () => {
  let attempts = 0;
  api.defaults.adapter = config => { attempts++; return unauthorized(config); };
  let finishRefresh!: (value: unknown) => void;
  const refresh = vi.spyOn(axios, 'post').mockImplementation(() => new Promise(resolve => {
    finishRefresh = resolve;
  }));
  const expired = vi.fn();
  window.addEventListener('auth:expired', expired);
  try {
    const pending = Promise.allSettled([api.get('/first'), api.get('/second')]);
    await vi.waitFor(() => expect(attempts).toBe(2));
    finishRefresh({ data: { accessToken: 'replacement-token' } });
    const results = await pending;
    expect(results.every(result => result.status === 'rejected')).toBe(true);
    expect(attempts).toBe(4);
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(refresh.mock.calls[0][2]).toMatchObject({ timeout: 15000, withCredentials: true });
    expect(expired).toHaveBeenCalled();
    expect(getAccessToken()).toBeNull();
  } finally {
    window.removeEventListener('auth:expired', expired);
  }
});

it('rejects a refresh response that has no token', async () => {
  api.defaults.adapter = unauthorized;
  vi.spyOn(axios, 'post').mockResolvedValue({ data: {} });
  await expect(api.get('/auth/me')).rejects.toThrow('Session refresh returned no access token');
  expect(getAccessToken()).toBeNull();
});

it('does not refresh the session while logging out', async () => {
  api.defaults.adapter = unauthorized;
  const refresh = vi.spyOn(axios, 'post');
  await expect(api.post('/auth/logout')).rejects.toThrow('Unauthorized');
  expect(refresh).not.toHaveBeenCalled();
});
