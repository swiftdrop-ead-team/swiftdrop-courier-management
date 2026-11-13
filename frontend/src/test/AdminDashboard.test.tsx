import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { UnifiedDashboardLayout } from '../components/layout/UnifiedDashboardLayout';
import { AdminDashboard } from '../pages/admin/AdminDashboard';
import { createAdminPreview } from '../pages/admin/adminData';

const service = vi.hoisted(() => ({ getAllUsers: vi.fn(), getPendingMerchants: vi.fn(), approveMerchant: vi.fn(), rejectMerchant: vi.fn() }));
vi.mock('../services/authService', () => ({ adminService: service }));
vi.mock('../hooks/useAuth', () => ({ useAuth: () => ({ user: { id: 'live-admin', fullName: 'Live Administrator', email: 'admin@example.com', roles: ['ADMIN'], status: 'ACTIVE' } }) }));
vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: true, addListener: vi.fn(), removeListener: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn() })));
vi.stubGlobal('scrollTo', vi.fn());

function workspace(tab = 'overview', previewMode = true) {
  const base = previewMode ? '/dashboard-preview/admin' : '/admin/dashboard';
  return render(<MemoryRouter initialEntries={[`${base}/${tab}`]}><Routes>
    <Route element={<UnifiedDashboardLayout role="ADMIN" basePath={base} />}>
      <Route path={`${base}/:tab?`} element={<AdminDashboard previewMode={previewMode} />} />
    </Route>
  </Routes></MemoryRouter>);
}
async function closeDialog() {
  fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Close' }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
}
beforeEach(() => {
  vi.clearAllMocks();
  service.getAllUsers.mockResolvedValue(createAdminPreview().users);
  service.getPendingMerchants.mockResolvedValue(createAdminPreview().merchants);
  service.approveMerchant.mockResolvedValue({ success: true });
  service.rejectMerchant.mockResolvedValue({ success: true });
});

describe('Admin preview workspace', () => {
  it('loads overview samples without making live administration requests', () => {
    workspace();
    expect(screen.getByRole('heading', { name: 'Keep good businesses moving.' })).toBeInTheDocument();
    expect(screen.getByText('Lanka Craft Studio')).toBeInTheDocument();
    expect(service.getAllUsers).not.toHaveBeenCalled();
    expect(service.getPendingMerchants).not.toHaveBeenCalled();
  });

  it('filters users, displays actual status, and opens read-only account details', async () => {
    workspace('users');
    fireEvent.change(screen.getByRole('combobox', { name: 'Account status' }), { target: { value: 'SUSPENDED' } });
    expect(screen.getByText('Dilan Senanayake')).toBeInTheDocument();
    expect(screen.queryByText('Asha Silva')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'View Dilan Senanayake' }));
    expect(within(screen.getByRole('dialog')).getByText('SUSPENDED')).toBeInTheDocument();
    await closeDialog();
    fireEvent.change(screen.getByRole('combobox', { name: 'Account status' }), { target: { value: 'All' } });
    fireEvent.change(screen.getByRole('combobox', { name: 'Role' }), { target: { value: 'COURIER' } });
    expect(screen.getByText('Kavindi Jayasuriya')).toBeInTheDocument();
    fireEvent.change(screen.getByRole('textbox', { name: 'Search users' }), { target: { value: 'missing' } });
    expect(screen.getByText('No matching users')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Export users' })).toBeDisabled();
  });

  it('requires confirmation for approval and updates session activity and user records', async () => {
    workspace('merchants');
    fireEvent.click(screen.getByRole('button', { name: 'Review Lanka Craft Studio' }));
    const dialog = within(screen.getByRole('dialog'));
    fireEvent.click(dialog.getByRole('button', { name: 'Approve application' }));
    expect(service.approveMerchant).not.toHaveBeenCalled();
    fireEvent.click(dialog.getByRole('button', { name: 'Confirm approval' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(within(screen.getByRole('table')).getByText('APPROVED')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('link', { name: 'Review Activity' }));
    expect(screen.getByText('Lanka Craft Studio')).toBeInTheDocument();
    expect(within(screen.getByRole('table')).getByText('APPROVED')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('link', { name: 'User Management' }));
    fireEvent.change(screen.getByRole('textbox', { name: 'Search users' }), { target: { value: 'Asha' } });
    expect(within(screen.getByRole('table')).getByText('APPROVED')).toBeInTheDocument();
    expect(service.approveMerchant).not.toHaveBeenCalled();
  });

  it('requires a rejection reason and records it in the review activity', async () => {
    workspace('merchants');
    fireEvent.click(screen.getByRole('button', { name: 'Review Green Basket' }));
    const dialog = within(screen.getByRole('dialog'));
    fireEvent.click(dialog.getByRole('button', { name: 'Reject application' }));
    fireEvent.click(dialog.getByRole('button', { name: 'Confirm rejection' }));
    expect(dialog.getByText('Add a reason so the merchant can understand the rejection.')).toBeInTheDocument();
    fireEvent.change(dialog.getByRole('textbox', { name: /Review notes/ }), { target: { value: 'Please provide a complete business address.' } });
    fireEvent.click(dialog.getByRole('button', { name: 'Confirm rejection' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    fireEvent.click(screen.getByRole('link', { name: 'Review Activity' }));
    expect(screen.getByText('Please provide a complete business address.')).toBeInTheDocument();
    expect(service.rejectMerchant).not.toHaveBeenCalled();
  });

  it('supports pagination and preview profile edits', () => {
    workspace('users');
    expect(screen.queryByText('Dilan Senanayake')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Go to next page' }));
    expect(screen.getByText('Dilan Senanayake')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('link', { name: 'Admin Profile' }));
    fireEvent.click(screen.getByRole('button', { name: 'Edit details' }));
    fireEvent.change(screen.getByRole('textbox', { name: 'Full name' }), { target: { value: 'Preview Reviewer' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    expect(screen.getByRole('heading', { name: 'Preview Reviewer' })).toBeInTheDocument();
  });
});

describe('Live administration', () => {
  it('reports a partial load failure while keeping available user records usable', async () => {
    service.getPendingMerchants.mockRejectedValue(new Error('Unavailable'));
    workspace('users', false);
    expect(await screen.findByText('Merchant applications could not be loaded. Refresh to try again.')).toBeInTheDocument();
    expect(screen.getByText('Asha Silva')).toBeInTheDocument();
    expect(service.getAllUsers).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByRole('link', { name: 'Merchant Approvals' }));
    expect(screen.getByText('Review queue unavailable')).toBeInTheDocument();
    expect(screen.queryByText('No pending applications')).not.toBeInTheDocument();
  });

  it('keeps the dialog open after a failed decision and permits retrying', async () => {
    service.approveMerchant.mockRejectedValueOnce({ response: { data: { message: 'Decision could not be saved.' } } });
    workspace('merchants', false);
    fireEvent.click(await screen.findByRole('button', { name: 'Review Lanka Craft Studio' }));
    const dialog = within(screen.getByRole('dialog'));
    fireEvent.click(dialog.getByRole('button', { name: 'Approve application' }));
    fireEvent.change(dialog.getByRole('textbox', { name: 'Review notes' }), { target: { value: 'Business information checked.' } });
    fireEvent.click(dialog.getByRole('button', { name: 'Confirm approval' }));
    expect(await dialog.findByText('Decision could not be saved.')).toBeInTheDocument();
    expect(dialog.getByRole('textbox')).toHaveValue('Business information checked.');
    expect(service.approveMerchant).toHaveBeenCalledWith('demo-application-1', 'Business information checked.');
    service.getPendingMerchants.mockResolvedValue(createAdminPreview().merchants.slice(1));
    fireEvent.click(dialog.getByRole('button', { name: 'Confirm approval' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.queryByText('Lanka Craft Studio')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('link', { name: 'Review Activity' }));
    expect(screen.getAllByText('Lanka Craft Studio')).toHaveLength(1);
  });

  it('does not submit a decision when the review is cancelled', async () => {
    workspace('merchants', false);
    fireEvent.click(await screen.findByRole('button', { name: 'Review Green Basket' }));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Reject application' }));
    await closeDialog();
    expect(service.rejectMerchant).not.toHaveBeenCalled();
    expect(service.approveMerchant).not.toHaveBeenCalled();
  });
});
