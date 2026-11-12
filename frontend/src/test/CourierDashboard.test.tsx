import React from 'react';
import { fireEvent, render, screen, within, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { CourierDashboard } from '../pages/courier/CourierDashboard';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { UnifiedDashboardLayout } from '../components/layout/UnifiedDashboardLayout';

vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: true, addListener: vi.fn(), removeListener: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn() })));
vi.stubGlobal('scrollTo', vi.fn());
function workspace(previewMode: boolean, tab = '') {
  const base = previewMode ? '/dashboard-preview/courier' : '/courier/dashboard';
  return render(<MemoryRouter initialEntries={[tab ? `${base}/${tab}` : base]}>
    <Routes><Route element={<UnifiedDashboardLayout role="COURIER" basePath={base} />}>
      <Route path={`${base}/:tab?`} element={<CourierDashboard previewMode={previewMode} />} />
    </Route></Routes>
  </MemoryRouter>);
}


vi.mock('../hooks/useAuth', () => ({ useAuth: () => ({ user: { fullName: 'Test Courier' } }) }));

describe('Courier overview availability', () => {
  it('allows a preview shift to switch availability in both directions', () => {
    workspace(true);
    const toggle = screen.getByRole('switch', { name: 'Courier availability' });
    expect(toggle).toHaveAttribute('aria-checked', 'false');
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-checked', 'true');
    expect(screen.getByText('Available for assignments')).toBeInTheDocument();
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-checked', 'false');
  });

  it('does not present local availability changes as live dispatch updates', () => {
    workspace(false);
    expect(screen.getByRole('switch', { name: 'Courier availability' })).toBeDisabled();
    expect(screen.getByText(/Dispatch is not connected/)).toBeInTheDocument();
    expect(screen.getByText('Welcome, Test Courier.')).toBeInTheDocument();
  });
});

describe('Courier workspace tabs', () => {
  const preview = (tab = 'deliveries') => workspace(true, tab);

  it('searches assignments and supports direct route access', () => {
    preview();
    fireEvent.change(screen.getByRole('textbox', { name: 'Search deliveries' }), { target: { value: 'Asha' } });
    expect(screen.getByText('DEMO-1002')).toBeInTheDocument();
    expect(screen.queryByText('DEMO-1001')).not.toBeInTheDocument();
    fireEvent.change(screen.getByRole('textbox', { name: 'Search deliveries' }), { target: { value: '' } });
    fireEvent.change(screen.getByRole('combobox', { name: 'Filter delivery status' }), { target: { value: 'Picked up' } });
    expect(screen.getByText('DEMO-1003')).toBeInTheDocument();
    expect(screen.queryByText('DEMO-1002')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('link', { name: /Active Route/i }));
    expect(screen.getByRole('heading', { name: 'Stop sequence' })).toBeInTheDocument();
    expect(screen.getByText('Nimal Perera')).toBeInTheDocument();
  });

  it('requires handover details and COD confirmation, then updates history across tabs', async () => {
    preview();
    fireEvent.click(screen.getByRole('button', { name: 'View DEMO-1002' }));
    const dialog = within(screen.getByRole('dialog'));
    fireEvent.click(dialog.getByRole('button', { name: 'Confirm pickup' }));
    fireEvent.click(dialog.getByRole('button', { name: 'Start delivery' }));
    fireEvent.click(dialog.getByRole('button', { name: 'Mark delivered' }));
    fireEvent.click(dialog.getByRole('button', { name: 'Confirm delivery' }));
    expect(dialog.getByText('Enter the recipient name or handover note.')).toBeInTheDocument();
    fireEvent.change(dialog.getByRole('textbox'), { target: { value: 'Received by Asha' } });
    fireEvent.click(dialog.getByRole('button', { name: 'Confirm delivery' }));
    expect(dialog.getByText('Confirm that the cash on delivery payment was collected.')).toBeInTheDocument();
    fireEvent.click(dialog.getByRole('checkbox'));
    fireEvent.click(dialog.getByRole('button', { name: 'Confirm delivery' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.queryByText('DEMO-1002')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('link', { name: /Delivery History/i }));
    expect(screen.getByText('DEMO-1002')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Export CSV' })).toBeEnabled();
    fireEvent.click(screen.getByRole('button', { name: 'View DEMO-1002' }));
    expect(within(screen.getByRole('dialog')).getByText('Received by Asha')).toBeInTheDocument();
  });

  it('records failed attempts and resets the preview shift from profile', async () => {
    preview();
    fireEvent.click(screen.getByRole('button', { name: 'View DEMO-1001' }));
    const dialog = within(screen.getByRole('dialog'));
    fireEvent.click(dialog.getByRole('button', { name: 'Report issue' }));
    fireEvent.click(dialog.getByRole('button', { name: 'Confirm failed attempt' }));
    expect(dialog.getByText('Explain why the delivery could not be completed.')).toBeInTheDocument();
    fireEvent.change(dialog.getByRole('textbox'), { target: { value: 'Recipient unavailable' } });
    fireEvent.click(dialog.getByRole('button', { name: 'Confirm failed attempt' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    fireEvent.click(screen.getByRole('link', { name: /Delivery History/i }));
    expect(within(screen.getByRole('table')).getByText('Failed')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('link', { name: 'Courier Profile' }));
    fireEvent.click(screen.getByRole('button', { name: 'Reset preview shift' }));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Reset shift' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    fireEvent.click(screen.getByRole('link', { name: /Delivery History/i }));
    expect(screen.getByText('No delivery history yet')).toBeInTheDocument();
  });

  it('keeps the signed-in dashboard free of sample assignments', () => {
    workspace(false, 'deliveries');
    expect(screen.getByText('No assignments to show')).toBeInTheDocument();
    expect(screen.queryByText('DEMO-1001')).not.toBeInTheDocument();
  });

  it('saves preview profile details and supports cancelling an edit', () => {
    preview('profile');
    fireEvent.click(screen.getByRole('button', { name: 'Edit details' }));
    fireEvent.change(screen.getByRole('textbox', { name: 'Full name' }), { target: { value: '  Nimal Perera  ' } });
    fireEvent.change(screen.getByRole('textbox', { name: 'Phone number' }), { target: { value: '0771234567' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    expect(screen.getByRole('heading', { name: 'Nimal Perera' })).toBeInTheDocument();
    expect(screen.getByText('0771234567')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Edit details' }));
    fireEvent.change(screen.getByRole('textbox', { name: 'Full name' }), { target: { value: 'Unsaved name' } });
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.getByRole('heading', { name: 'Nimal Perera' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('link', { name: 'Overview' }));
    expect(screen.getByRole('heading', { name: 'Welcome, Nimal Perera.' })).toBeInTheDocument();
  });

  it('keeps real profile details read-only and explains unavailable preferences', () => {
    workspace(false, 'profile');
    expect(screen.queryByRole('button', { name: 'Edit details' })).not.toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Assignment notifications' })).toBeDisabled();
    expect(screen.getByText('Contact your administrator to update account details.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reset preview shift' })).not.toBeInTheDocument();
  });

  it('lets couriers cancel a preview reset and keeps profile edits when resetting', async () => {
    preview('profile');
    fireEvent.click(screen.getByRole('button', { name: 'Edit details' }));
    fireEvent.change(screen.getByRole('textbox', { name: 'Full name' }), { target: { value: 'Nimal Perera' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    fireEvent.click(screen.getByRole('button', { name: 'Reset preview shift' }));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Keep progress' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Reset preview shift' }));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Reset shift' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByRole('heading', { name: 'Nimal Perera' })).toBeInTheDocument();
  });
});
