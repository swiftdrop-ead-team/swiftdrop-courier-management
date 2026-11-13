import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Alert, Box, Card, Dialog, DialogActions, DialogContent, DialogTitle, Grid, LinearProgress, Snackbar, TablePagination, TextField } from '@mui/material';
import { ArrowRight, Building2, Check, Download, RefreshCw, ShieldCheck, Users, X } from 'lucide-react';
import SoftBox from 'components/SoftBox';
import SoftTypography from 'components/SoftTypography';
import SoftButton from 'components/SoftButton';
import SoftInput from 'components/SoftInput';
import MiniStatisticsCard from 'examples/Cards/StatisticsCards/MiniStatisticsCard';
import Table from 'examples/Tables/Table';
import rocket from 'assets/images/illustrations/rocket-white.png';
import { useAuth } from '../../hooks/useAuth';
import { adminService } from '../../services/authService';
import type { PendingMerchant, User } from '../../types/auth';
import { AdminProfile } from './AdminProfile';
import { AdminActivity, createAdminPreview, dateTime, downloadCsv } from './adminData';
import { AdminBadge, AdminDetails, AdminEmpty, adminActionStyle, adminSelectStyle } from './AdminShared';

const headings = {
  overview: ['Your platform, at a glance.', 'Keep account activity and merchant reviews moving.'],
  merchants: ['Merchant approvals', 'Review business registrations and make informed decisions.'],
  users: ['User management', 'Find accounts and review roles, status, and contact details.'],
  activity: ['Review activity', 'Merchant decisions made in this browser session.'],
  profile: ['Administrator profile', 'Your account details and workspace access.'],
};
type Tab = keyof typeof headings;
type Decision = 'APPROVED' | 'REJECTED';
const roles = ['ADMIN', 'MERCHANT', 'COURIER', 'CUSTOMER'];

export const AdminDashboard: React.FC<{ previewMode?: boolean }> = ({ previewMode = false }) => {
  const { user } = useAuth();
  const location = useLocation();
  const base = previewMode ? '/dashboard-preview/admin' : '/admin/dashboard';
  const segment = location.pathname.slice(base.length).split('/')[1] || location.hash.slice(1) || 'overview';
  const activeTab: Tab = Object.prototype.hasOwnProperty.call(headings, segment) ? segment as Tab : 'overview';
  const [users, setUsers] = useState<User[] | null>(() => previewMode ? createAdminPreview().users : null);
  const [merchants, setMerchants] = useState<PendingMerchant[] | null>(() => previewMode ? createAdminPreview().merchants : null);
  const [account, setAccount] = useState<User>(() => createAdminPreview().users[0]);
  const [loading, setLoading] = useState(!previewMode);
  const [errors, setErrors] = useState({ users: '', merchants: '' });
  const [updatedAt, setUpdatedAt] = useState<string | null>(null);
  const [activity, setActivity] = useState<AdminActivity[]>([]);
  const [query, setQuery] = useState('');
  const [role, setRole] = useState('All');
  const [status, setStatus] = useState('All');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(5);
  const [selectedMerchant, setSelectedMerchant] = useState<PendingMerchant | null>(null);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [decision, setDecision] = useState<Decision | null>(null);
  const [notes, setNotes] = useState('');
  const [reviewError, setReviewError] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [resetOpen, setResetOpen] = useState(false);
  const requestSequence = useRef(0);
  const displayAccount = previewMode ? account : user;
  const name = displayAccount?.fullName || 'Administrator';

  const loadData = useCallback(async () => {
    if (previewMode) return;
    const request = ++requestSequence.current;
    setLoading(true); setPage(0); setErrors({ users: '', merchants: '' });
    const [userResult, merchantResult] = await Promise.allSettled([adminService.getAllUsers(), adminService.getPendingMerchants()]);
    if (request !== requestSequence.current) return;
    setUsers(userResult.status === 'fulfilled' ? userResult.value : null);
    setMerchants(merchantResult.status === 'fulfilled' ? merchantResult.value : null);
    setErrors({ users: userResult.status === 'rejected' ? 'User accounts could not be loaded. Refresh to try again.' : '', merchants: merchantResult.status === 'rejected' ? 'Merchant applications could not be loaded. Refresh to try again.' : '' });
    setUpdatedAt(userResult.status === 'fulfilled' && merchantResult.status === 'fulfilled' ? new Date().toISOString() : null);
    setLoading(false);
  }, [previewMode]);
  useEffect(() => { void loadData(); return () => { requestSequence.current += 1; }; }, [loadData]);
  useEffect(() => { setQuery(''); setRole('All'); setStatus('All'); setPage(0); setSelectedMerchant(null); setSelectedUser(null); }, [activeTab]);

  const pending = (merchants || []).filter((item) => item.approvalStatus === 'PENDING');
  const visibleUsers = (users || []).filter((item) => `${item.fullName} ${item.email} ${item.phoneNumber || ''}`.toLowerCase().includes(query.trim().toLowerCase()) && (role === 'All' || item.roles.includes(role as User['roles'][number])) && (status === 'All' || item.status === status));
  const visibleMerchants = (merchants || []).filter((item) => `${item.businessName} ${item.contactPerson} ${item.email}`.toLowerCase().includes(query.trim().toLowerCase()) && (status === 'All' || item.approvalStatus === status));
  const visibleActivity = activity.filter((item) => `${item.merchant} ${item.actor} ${item.notes}`.toLowerCase().includes(query.trim().toLowerCase()) && (status === 'All' || item.decision === status));
  const pageRows = <T,>(items: T[]) => items.slice(page * pageSize, (page + 1) * pageSize);
  const resetPage = (value: string, setter: (value: string) => void) => { setter(value); setPage(0); };
  const go = (tab: Tab) => `${base}/${tab}`;

  function review(item: PendingMerchant) { setSelectedMerchant(item); setDecision(null); setNotes(''); setReviewError(''); }
  async function submitDecision() {
    if (!selectedMerchant || !decision || busy) return;
    if (decision === 'REJECTED' && !notes.trim()) { setReviewError('Add a reason so the merchant can understand the rejection.'); return; }
    setBusy(true); setReviewError('');
    const application = selectedMerchant;
    try {
      if (!previewMode) {
        const result = decision === 'APPROVED' ? await adminService.approveMerchant(application.id, notes.trim() || undefined) : await adminService.rejectMerchant(application.id, notes.trim());
        if (!result.success) throw new Error(result.message || 'The decision could not be saved.');
      }
      const timestamp = new Date().toISOString();
      setActivity((items) => [{ id: `${application.id}-${timestamp}`, merchant: application.businessName, decision, notes: notes.trim(), timestamp, actor: name }, ...items]);
      if (previewMode) setMerchants((items) => (items || []).map((item) => item.id === application.id ? { ...item, approvalStatus: decision, notes: notes.trim() } : item));
      else setMerchants((items) => (items || []).filter((item) => item.id !== application.id));
      setUsers((items) => items?.map((item) => item.id === application.userId ? { ...item, merchantApprovalStatus: decision } : item) || null);
      setSelectedMerchant(null);
      setPage(0);
      setMessage(`${previewMode ? 'Preview: ' : ''}${application.businessName} ${decision === 'APPROVED' ? 'approved' : 'rejected'}.`);
      if (!previewMode) await loadData();
    } catch (error: unknown) {
      const serverMessage = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setReviewError(serverMessage || (error instanceof Error ? error.message : 'The decision could not be saved. Please try again.'));
    } finally { setBusy(false); }
  }

  function pagination(count: number) {
    return <TablePagination component="div" count={count} page={page} rowsPerPage={pageSize} rowsPerPageOptions={[5, 10, 25]} onPageChange={(_, value) => setPage(value)} onRowsPerPageChange={(event) => { setPageSize(Number(event.target.value)); setPage(0); }} sx={{ '& .MuiTablePagination-toolbar': { flexWrap: 'wrap', px: 2 } }} />;
  }
  function select(label: string, value: string, values: string[], setter: (value: string) => void) {
    return <Box><SoftTypography component="label" htmlFor={`admin-filter-${label}`} variant="caption" fontWeight="bold" display="block" mb={1}>{label}</SoftTypography><Box component="select" id={`admin-filter-${label}`} aria-label={label} value={value} onChange={(event: React.ChangeEvent<HTMLSelectElement>) => resetPage(event.target.value, setter)} sx={adminSelectStyle}>{values.map((option) => <option key={option} value={option}>{option === 'All' ? 'All ' + label.toLowerCase() : option}</option>)}</Box></Box>;
  }
  function search(label: string) {
    return <Box sx={{ flex: '1 1 240px', maxWidth: { xs: '100%', md: 380 } }}><SoftTypography component="label" htmlFor="admin-search" variant="caption" fontWeight="bold" display="block" mb={1}>{label}</SoftTypography><SoftInput id="admin-search" fullWidth placeholder={activeTab === 'users' ? 'Name, email or phone' : 'Business, contact or notes'} value={query} onChange={(event: React.ChangeEvent<HTMLInputElement>) => resetPage(event.target.value, setQuery)} /></Box>;
  }
  function merchantTable(items: PendingMerchant[]) {
    if (!items.length) return <AdminEmpty title={query || status !== 'All' ? 'No matching applications' : 'No pending applications'} text="New merchant registrations will appear here for review." />;
    return <Table columns={[{ name: 'business', align: 'left' }, { name: 'registered', align: 'left' }, { name: 'status', align: 'center' }, { name: 'action', align: 'right' }]} rows={items.map((item) => ({
      id: item.id, hasBorder: true,
      business: <Box component="span" sx={{ display: 'flex', alignItems: 'center', gap: 1.5, px: 2 }}><SoftBox component="span" bgColor="primary" borderRadius="lg" p={1} display="flex"><Building2 size={18} color="#00274c" /></SoftBox><Box component="span"><SoftTypography component="span" variant="button" fontWeight="bold" display="block">{item.businessName}</SoftTypography><SoftTypography component="span" variant="caption" color="text" display="block">{item.contactPerson}</SoftTypography></Box></Box>,
      registered: dateTime(item.registeredAt), status: <AdminBadge status={item.approvalStatus} />,
      action: <SoftButton variant="text" color="dark" size="small" disabled={busy || loading} aria-label={`Review ${item.businessName}`} onClick={() => review(item)} sx={{ fontWeight: 'bold' }}>{item.approvalStatus === 'PENDING' ? 'Review application' : 'View decision'}<ArrowRight size={13} style={{ marginLeft: 6 }} /></SoftButton>,
    }))} />;
  }
  function userTable(items: User[]) {
    if (!items.length) return <AdminEmpty title="No matching users" text="Try a different name, role, or account status." />;
    return <Table columns={[{ name: 'user', align: 'left' }, { name: 'roles', align: 'left' }, { name: 'account', align: 'center' }, { name: 'merchant', align: 'center' }, { name: 'action', align: 'right' }]} rows={items.map((item) => ({
      id: item.id, hasBorder: true,
      user: <Box component="span" sx={{ px: 2, display: 'block' }}><SoftTypography component="span" variant="button" fontWeight="bold" display="block">{item.fullName}</SoftTypography><SoftTypography component="span" variant="caption" color="text" display="block">{item.email}</SoftTypography></Box>,
      roles: item.roles.join(', '), account: <AdminBadge status={item.status} />, merchant: item.merchantApprovalStatus ? <AdminBadge status={item.merchantApprovalStatus} /> : 'Not applicable',
      action: <SoftButton variant="text" color="dark" size="small" aria-label={`View ${item.fullName}`} onClick={() => setSelectedUser(item)}>Details <ArrowRight size={13} style={{ marginLeft: 6 }} /></SoftButton>,
    }))} />;
  }

  function overview() {
    return <Grid container spacing={3}>
      {[
        { title: 'Registered users', count: users?.length ?? 'Unavailable', icon: 'people', color: 'primary' },
        { title: 'Pending approvals', count: merchants ? pending.length : 'Unavailable', icon: 'pending_actions', color: 'warning' },
        { title: 'Active accounts', count: users?.filter((item) => item.status === 'ACTIVE').length ?? 'Unavailable', icon: 'verified_user', color: 'success' },
        { title: 'Couriers', count: users?.filter((item) => item.roles.includes('COURIER')).length ?? 'Unavailable', icon: 'local_shipping', color: 'info' },
      ].map(({ title, count, icon, color }) => <Grid item xs={12} sm={6} xl={3} key={title}><MiniStatisticsCard title={{ text: title, fontWeight: 'medium' }} count={count} percentage={{ color: 'secondary', text: '' }} icon={{ color, component: icon }} /></Grid>)}
      <Grid item xs={12} lg={8}><Card sx={{ p: { xs: 3, md: 4 }, height: '100%' }}>
        <Grid container spacing={3} alignItems="center" sx={{ height: '100%' }}>
        <Grid item xs={12} sm={7}>
        <SoftTypography variant="caption" color="text" fontWeight="bold" sx={{ letterSpacing: 1.2 }}>YOUR REVIEW DESK</SoftTypography>
        <SoftTypography variant="h3" fontWeight="bold" mt={1}>Keep good businesses moving.</SoftTypography>
        <SoftTypography variant="body2" color="text" mt={1.5} mb={3} sx={{ maxWidth: 520 }}>{merchants ? pending.length ? `${pending.length} merchant applications are waiting for your review. Check business details and leave a clear decision.` : 'Your review queue is clear. New applications will appear as merchants register.' : 'Merchant data is unavailable. Refresh the workspace to try again.'}</SoftTypography>
        <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap' }}><SoftButton component={Link} to={go('merchants')} variant="gradient" color="primary" sx={adminActionStyle}>Review applications <ArrowRight size={15} style={{ marginLeft: 6 }} /></SoftButton><SoftButton component={Link} to={go('users')} variant="outlined" color="dark">View users</SoftButton></Box>
        </Grid>
        <Grid item xs={12} sm={5}>
          <SoftBox variant="gradient" bgColor="primary" borderRadius="lg" sx={{ minHeight: 200, display: 'grid', placeItems: 'center', overflow: 'hidden', boxShadow: '0 8px 24px rgba(0, 39, 76, 0.12)' }}>
            <Box component="img" src={rocket} alt="" sx={{ width: '85%', maxHeight: 230, objectFit: 'contain', display: 'block' }} />
          </SoftBox>
        </Grid>
        </Grid>
      </Card></Grid>
      <Grid item xs={12} lg={4}><Card sx={{ p: 3, height: '100%', bgcolor: '#00274c' }}>
        <ShieldCheck size={28} color="#ffcb05" /><SoftTypography variant="h5" color="white" fontWeight="bold" mt={2}>A focused admin workspace.</SoftTypography><SoftTypography variant="body2" color="white" opacity={0.8} mt={1.5}>Review merchant registrations, understand your account directory, and keep a record of this session's decisions.</SoftTypography><SoftTypography variant="caption" color="white" opacity={0.65} mt={3} display="block">{previewMode ? 'Sample data / no live changes' : updatedAt ? `Last refreshed: ${dateTime(updatedAt)}` : 'Live data has not fully loaded'}</SoftTypography>
      </Card></Grid>
      <Grid item xs={12} lg={8}><Card><Box sx={{ p: 3, display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: 2 }}><Box><SoftTypography variant="h6" fontWeight="bold">Waiting for review</SoftTypography><SoftTypography variant="button" color="text" fontWeight="regular">Your next merchant applications.</SoftTypography></Box><SoftButton component={Link} to={go('merchants')} variant="text" color="dark">View queue <ArrowRight size={13} style={{ marginLeft: 6 }} /></SoftButton></Box>{merchants ? merchantTable(pending.slice(0, 5)) : <AdminEmpty title="Review queue unavailable" text="Refresh to retry loading merchant applications." />}</Card></Grid>
      <Grid item xs={12} lg={4}><Card sx={{ p: 3 }}><SoftTypography variant="h6" fontWeight="bold" mb={1}>Accounts by role</SoftTypography><SoftTypography variant="caption" color="text">Accounts with multiple roles appear in each group.</SoftTypography>{roles.map((item) => { const count = users?.filter((entry) => entry.roles.includes(item as User['roles'][number])).length; return <Box key={item} mt={2.5}><Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}><SoftTypography variant="caption" fontWeight="bold">{item}</SoftTypography><SoftTypography variant="caption" color="text">{count ?? 'Unavailable'}</SoftTypography></Box><LinearProgress variant="determinate" aria-label={`${item} accounts`} value={users?.length && count !== undefined ? count / users.length * 100 : 0} /></Box>; })}</Card></Grid>
    </Grid>;
  }

  function merchantsPage() {
    if (!merchants) return <Card><AdminEmpty title="Review queue unavailable" text="Refresh to retry loading merchant applications." /></Card>;
    return <Card><Box sx={{ p: 3 }}><SoftTypography variant="h6" fontWeight="bold">{previewMode ? 'Merchant applications' : 'Pending applications'}</SoftTypography><SoftTypography variant="button" color="text" fontWeight="regular">{previewMode ? 'Review sample businesses and see how decisions update the workspace.' : 'This queue contains applications currently awaiting a decision.'}</SoftTypography><Box sx={{ display: 'flex', alignItems: 'flex-end', flexWrap: 'wrap', gap: 2, mt: 3 }}>{search('Search applications')}{previewMode && select('Application status', status, ['All', 'PENDING', 'APPROVED', 'REJECTED'], setStatus)}</Box></Box>{merchantTable(pageRows(visibleMerchants))}{pagination(visibleMerchants.length)}</Card>;
  }
  function usersPage() {
    if (!users) return <Card><AdminEmpty title="Account directory unavailable" text="Refresh to retry loading user accounts." /></Card>;
    return <Card><Box sx={{ p: 3 }}><Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: 2 }}><Box><SoftTypography variant="h6" fontWeight="bold">Account directory</SoftTypography><SoftTypography variant="button" color="text" fontWeight="regular">{visibleUsers.length} matching accounts</SoftTypography></Box><SoftButton variant="outlined" color="dark" disabled={!visibleUsers.length} onClick={() => downloadCsv('swiftdrop-users.csv', [['Name', 'Email', 'Phone', 'Roles', 'Status', 'Merchant status'], ...visibleUsers.map((item) => [item.fullName, item.email, item.phoneNumber || '', item.roles.join(', '), item.status, item.merchantApprovalStatus || ''])])}><Download size={14} style={{ marginRight: 6 }} />Export users</SoftButton></Box><Box sx={{ display: 'flex', alignItems: 'flex-end', flexWrap: 'wrap', gap: 2, mt: 3 }}>{search('Search users')}{select('Role', role, ['All', ...roles], setRole)}{select('Account status', status, ['All', 'ACTIVE', 'INACTIVE', 'SUSPENDED'], setStatus)}</Box></Box>{userTable(pageRows(visibleUsers))}{pagination(visibleUsers.length)}</Card>;
  }
  function activityPage() {
    return <Card><Box sx={{ p: 3 }}><SoftTypography variant="h6" fontWeight="bold">Session review log</SoftTypography><SoftTypography variant="button" color="text" fontWeight="regular" display="block">Only successful decisions made in this browser session appear here. This is not the platform audit history; reloading clears this list.</SoftTypography><Box sx={{ display: 'flex', alignItems: 'flex-end', flexWrap: 'wrap', gap: 2, mt: 3 }}>{search('Search activity')}{select('Decision', status, ['All', 'APPROVED', 'REJECTED'], setStatus)}</Box></Box>{visibleActivity.length ? <Table columns={[{ name: 'business', align: 'left' }, { name: 'decision', align: 'center' }, { name: 'reviewer', align: 'left' }, { name: 'time', align: 'left' }, { name: 'notes', align: 'left' }]} rows={pageRows(visibleActivity).map((item) => ({ id: item.id, hasBorder: true, business: item.merchant, decision: <AdminBadge status={item.decision} />, reviewer: item.actor, time: dateTime(item.timestamp), notes: <Box component="span" sx={{ display: 'block', whiteSpace: 'normal', maxWidth: 260, overflowWrap: 'anywhere' }}>{item.notes || 'No notes added'}</Box> }))} /> : <AdminEmpty title={query || status !== 'All' ? 'No matching activity' : 'No decisions this session'} text="Successful merchant approval decisions will appear here." />}{pagination(visibleActivity.length)}</Card>;
  }

  return <Box sx={{ '& .MuiCard-root': { minWidth: 0 } }}>
    {previewMode && <Alert severity="info" sx={{ mb: 3, borderRadius: 3, fontSize: 12 }}>Preview workspace / Sample accounts and applications. Changes reset when you reload. No live accounts are modified.</Alert>}
    <Box sx={{ mb: 3, display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-start', gap: 2 }}><Box><SoftTypography component="h1" variant="h4" fontWeight="bold">{headings[activeTab][0]}</SoftTypography><SoftTypography variant="body2" color="text" mt={0.5}>{headings[activeTab][1]}</SoftTypography></Box><SoftButton variant="outlined" color="dark" disabled={loading || busy} onClick={() => previewMode ? setResetOpen(true) : void loadData()}><RefreshCw size={14} style={{ marginRight: 6 }} />{previewMode ? 'Reset preview' : 'Refresh data'}</SoftButton></Box>
    {errors.users && <Alert severity="error" sx={{ mb: 2 }}>{errors.users}</Alert>}{errors.merchants && <Alert severity="error" sx={{ mb: 2 }}>{errors.merchants}</Alert>}
    {loading ? <Box role="status" aria-label="Loading admin workspace" sx={{ py: 4 }}><LinearProgress /><SoftTypography variant="button" color="text" display="block" mt={2}>Loading administration data...</SoftTypography></Box> : <>
      {activeTab === 'overview' && overview()}{activeTab === 'merchants' && merchantsPage()}{activeTab === 'users' && usersPage()}{activeTab === 'activity' && activityPage()}
      {activeTab === 'profile' && (displayAccount ? <AdminProfile account={displayAccount} previewMode={previewMode} onSave={(updated) => { setAccount(updated); setUsers((items) => items?.map((entry) => entry.id === updated.id ? updated : entry) || null); setMessage('Preview profile updated.'); }} /> : <Card><AdminEmpty title="Account unavailable" text="Your signed-in profile could not be loaded." /></Card>)}
    </>}
    <Dialog open={Boolean(selectedMerchant)} onClose={() => { if (!busy) setSelectedMerchant(null); }} fullWidth maxWidth="sm" aria-labelledby="merchant-review-title"><DialogTitle id="merchant-review-title">{selectedMerchant?.businessName}</DialogTitle>{selectedMerchant && <><DialogContent><Box sx={{ display: 'grid', gap: 3 }}><AdminBadge status={selectedMerchant.approvalStatus} /><AdminDetails items={[["Contact person", selectedMerchant.contactPerson], ['Email address', selectedMerchant.email], ['Phone number', selectedMerchant.phoneNumber], ['Business address', selectedMerchant.businessAddress], ['Registered', dateTime(selectedMerchant.registeredAt)], ['Application ID', selectedMerchant.id]]} />{selectedMerchant.approvalStatus !== 'PENDING' && <AdminDetails items={[["Review notes", selectedMerchant.notes || 'No notes added']]} />}{decision && <><Alert severity={decision === 'APPROVED' ? 'info' : 'warning'}>{previewMode ? 'Preview decision only. ' : ''}{decision === 'APPROVED' ? 'Approval enables merchant access. Confirm the business details before continuing.' : 'The merchant will be marked as rejected. Include a clear reason.'}</Alert><TextField fullWidth multiline minRows={3} label="Review notes" required={decision === 'REJECTED'} value={notes} disabled={busy} onChange={(event) => { setNotes(event.target.value); setReviewError(''); }} inputProps={{ maxLength: 2000 }} helperText={decision === 'REJECTED' ? 'A rejection reason is required.' : 'Optional context for this decision.'} /></>}{reviewError && <Alert severity="error">{reviewError}</Alert>}</Box></DialogContent><DialogActions sx={{ p: 3, gap: 1, flexWrap: 'wrap' }}><SoftButton variant="text" color="secondary" disabled={busy} onClick={() => setSelectedMerchant(null)}>Close</SoftButton>{selectedMerchant.approvalStatus === 'PENDING' && (decision ? <><SoftButton variant="text" color="secondary" disabled={busy} onClick={() => { setDecision(null); setReviewError(''); }}>Back</SoftButton><SoftButton variant="gradient" color={decision === 'APPROVED' ? 'primary' : 'error'} sx={decision === 'APPROVED' ? adminActionStyle : undefined} disabled={busy} onClick={() => void submitDecision()}>{busy ? 'Saving decision...' : decision === 'APPROVED' ? 'Confirm approval' : 'Confirm rejection'}</SoftButton></> : <><SoftButton variant="outlined" color="error" disabled={busy || loading} onClick={() => setDecision('REJECTED')}><X size={14} style={{ marginRight: 6 }} />Reject application</SoftButton><SoftButton variant="gradient" color="primary" sx={adminActionStyle} disabled={busy || loading} onClick={() => setDecision('APPROVED')}><Check size={14} style={{ marginRight: 6 }} />Approve application</SoftButton></>)}</DialogActions></>}</Dialog>
    <Dialog open={Boolean(selectedUser)} onClose={() => setSelectedUser(null)} fullWidth maxWidth="sm" aria-labelledby="user-detail-title"><DialogTitle id="user-detail-title">{selectedUser?.fullName}</DialogTitle>{selectedUser && <DialogContent><Box mb={3}><AdminBadge status={selectedUser.status} /></Box><AdminDetails items={[["Email address", selectedUser.email], ['Phone number', selectedUser.phoneNumber || 'Not provided'], ['Roles', selectedUser.roles.join(', ')], ['Merchant status', selectedUser.merchantApprovalStatus || 'Not applicable'], ['Registered', dateTime(selectedUser.createdAt)], ['Last updated', dateTime(selectedUser.updatedAt)], ['Account ID', selectedUser.id]]} /><SoftTypography variant="caption" color="text" display="block" mt={3}>Role and account status changes are not available in this workspace.</SoftTypography></DialogContent>}<DialogActions sx={{ p: 3 }}><SoftButton variant="text" color="secondary" onClick={() => setSelectedUser(null)}>Close</SoftButton></DialogActions></Dialog>
    <Dialog open={resetOpen} onClose={() => setResetOpen(false)} fullWidth maxWidth="xs" aria-labelledby="admin-reset-title"><DialogTitle id="admin-reset-title">Reset preview workspace?</DialogTitle><DialogContent><SoftTypography variant="body2" color="text">Restore sample accounts and applications and clear this session's decisions. Your preview profile edits will be reset too.</SoftTypography></DialogContent><DialogActions sx={{ p: 3 }}><SoftButton variant="text" color="secondary" onClick={() => setResetOpen(false)}>Keep changes</SoftButton><SoftButton variant="gradient" color="primary" sx={adminActionStyle} onClick={() => { const data = createAdminPreview(); setUsers(data.users); setMerchants(data.merchants); setAccount(data.users[0]); setActivity([]); setQuery(''); setRole('All'); setStatus('All'); setPage(0); setResetOpen(false); setMessage('Preview workspace reset.'); }}>Reset workspace</SoftButton></DialogActions></Dialog>
    <Snackbar open={Boolean(message)} message={message} autoHideDuration={4500} onClose={() => setMessage('')} />
  </Box>;
};
