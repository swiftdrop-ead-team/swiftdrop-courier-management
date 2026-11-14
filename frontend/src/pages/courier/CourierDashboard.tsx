import React, { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Alert, Box, Card, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, Grid, LinearProgress, Snackbar, TextField } from '@mui/material';
import { MapPin, Power, Navigation, ArrowRight, Package, Download } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import SoftBox from 'components/SoftBox';
import SoftTypography from 'components/SoftTypography';
import SoftButton from 'components/SoftButton';
import SoftInput from 'components/SoftInput';
import SoftBadge from 'components/SoftBadge';
import MiniStatisticsCard from 'examples/Cards/StatisticsCards/MiniStatisticsCard';
import Table from 'examples/Tables/Table';
import courierIllustration from '../../assets/images/courier-shift.png';
import { CourierProfile, CourierProfileDetails } from './CourierProfile';
import { CourierDelivery, DeliveryStatus, isCompleted, lkr, previewDeliveries } from './courierData';

const tabs = ['overview', 'deliveries', 'route', 'history', 'profile'] as const;
type Tab = typeof tabs[number];
const labels: Record<Tab, string> = { overview: 'Overview', deliveries: 'Assigned deliveries', route: 'Active route', history: 'Delivery history', profile: 'Profile' };
const descriptions: Record<Tab, string> = {
  overview: 'Your availability, deliveries, and daily progress in one place.',
  deliveries: 'Review parcel details and manage each handover.',
  route: 'Follow your stop sequence and update delivery progress.',
  history: 'Review completed handovers and unsuccessful attempts.',
  profile: 'Your account and workspace preferences.',
};
const badgeColor = (status: DeliveryStatus) => status === 'Delivered' ? 'success' : status === 'Failed' ? 'error' : status === 'Assigned' ? 'warning' : 'info';
const primaryAction = { color: '#00274c !important', fontWeight: 'bold' };
const freshDeliveries = () => previewDeliveries.map((item) => ({ ...item }));

function EmptyState({ title, text }: { title: string; text: string }) {
  return <Box sx={{ p: 5, textAlign: 'center' }}>
    <Package size={32} style={{ margin: '0 auto 12px', color: '#8392ab' }} />
    <SoftTypography variant="h6">{title}</SoftTypography>
    <SoftTypography variant="button" color="text" fontWeight="regular">{text}</SoftTypography>
  </Box>;
}
function Details({ items }: { items: string[][] }) {
  return <Box sx={{ display: 'grid', gap: 2 }}>{items.map(([label, value]) => <Box key={label}>
    <SoftTypography variant="caption" color="text" display="block">{label}</SoftTypography>
    <SoftTypography variant="body2" sx={{ overflowWrap: 'anywhere' }}>{value}</SoftTypography>
  </Box>)}</Box>;
}
function StatusBadge({ status }: { status: DeliveryStatus }) {
  return <SoftBadge badgeContent={status} color={badgeColor(status)} size="xs" container />;
}

export const CourierDashboard: React.FC<{ previewMode?: boolean }> = ({ previewMode = false }) => {
  const { user } = useAuth();
  const { pathname } = useLocation();
  const base = previewMode ? '/dashboard-preview/courier' : '/courier/dashboard';
  const segment = pathname.slice(base.length).split('/')[1] || 'overview';
  const activeTab: Tab = tabs.includes(segment as Tab) ? segment as Tab : 'overview';
  const [isAvailable, setIsAvailable] = useState(false);
  const [deliveries, setDeliveries] = useState<CourierDelivery[]>(() => previewMode ? freshDeliveries() : []);
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('All');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [outcome, setOutcome] = useState<'Delivered' | 'Failed' | null>(null);
  const [proof, setProof] = useState('');
  const [proofError, setProofError] = useState('');
  const [codConfirmed, setCodConfirmed] = useState(false);
  const [message, setMessage] = useState('');
  const [notifications, setNotifications] = useState(true);
  const [profile, setProfile] = useState<CourierProfileDetails>(() => ({
    name: previewMode ? 'Courier Preview' : user?.fullName || 'Courier',
    email: previewMode ? 'courier@example.com' : user?.email || '',
    phone: previewMode ? '' : user?.phoneNumber || '',
  }));
  const name = previewMode ? profile.name : user?.fullName || 'Courier';
  const accountProfile = previewMode ? profile : { name, email: user?.email || '', phone: user?.phoneNumber || '' };
  useEffect(() => {
    setQuery('');
    setFilter('All');
    setSelectedId(null);
  }, [activeTab]);
  const selected = deliveries.find((item) => item.id === selectedId);
  const pending = deliveries.filter((item) => !isCompleted(item));
  const history = deliveries.filter(isCompleted);
  const delivered = deliveries.filter((item) => item.status === 'Delivered');
  const next = pending[0];
  const source = activeTab === 'history' ? history : pending;
  const visible = source.filter((item) => (filter === 'All' || item.status === filter) && `${item.id} ${item.recipient} ${item.city}`.toLowerCase().includes(query.trim().toLowerCase()));

  function openDetails(id: string) {
    setSelectedId(id); setOutcome(null); setProof(''); setProofError(''); setCodConfirmed(false);
  }
  function updateStatus(id: string, status: DeliveryStatus, note?: string) {
    if (!previewMode) return;
    setDeliveries((items) => items.map((item) => item.id === id ? { ...item, status, proof: note, completedAt: ['Delivered', 'Failed'].includes(status) ? new Date().toISOString() : undefined } : item));
    setMessage(`Preview updated: ${id} / ${status}.`);
  }
  function confirmOutcome() {
    if (!selected || !outcome) return;
    if (!proof.trim()) { setProofError(outcome === 'Delivered' ? 'Enter the recipient name or handover note.' : 'Explain why the delivery could not be completed.'); return; }
    if (outcome === 'Delivered' && selected.cod > 0 && !codConfirmed) { setProofError('Confirm that the cash on delivery payment was collected.'); return; }
    updateStatus(selected.id, outcome, proof.trim()); setSelectedId(null);
  }
  function exportHistory() {
    const cell = (value: string | number) => {
      let text = String(value);
      if (/^\s*[=+@-]/.test(text)) text = `'${text}`;
      return `"${text.replace(/"/g, '""')}"`;
    };
    const rows = [['Tracking', 'Recipient', 'Status', 'Completed at', 'COD (LKR)', 'Note'], ...history.map((item) => [item.id, item.recipient, item.status, item.completedAt || '', item.status === 'Delivered' ? item.cod : 0, item.proof || ''])];
    const url = URL.createObjectURL(new Blob(['\uFEFF' + rows.map((row) => row.map(cell).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a'); link.href = url; link.download = 'courier-preview-history.csv'; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  const go = (tab: Tab) => `${base}/${tab}`;
  function parcelTable(items: CourierDelivery[]) {
    if (!items.length) return <EmptyState title={query || filter !== 'All' ? 'No matching deliveries' : activeTab === 'history' ? 'No delivery history yet' : 'No assignments to show'} text={previewMode ? 'Complete a preview delivery to see it in history, or adjust your search and filters.' : 'Delivery records will appear when dispatch is connected.'} />;
    return <Table columns={[
      { name: 'parcel', align: 'left' }, { name: 'destination', align: 'left' },
      { name: 'status', align: 'center' }, { name: 'payment', align: 'right' }, { name: 'action', align: 'right' },
    ]} rows={items.map((item) => ({
      id: item.id, hasBorder: true,
      parcel: <Box component="span" sx={{ display: 'flex', alignItems: 'center', gap: 1.5, px: 2 }}>
        <SoftBox component="span" bgColor="primary" borderRadius="lg" p={1} display="flex" sx={{ color: '#00274c', boxShadow: '0 3px 10px rgba(0,39,76,.12)' }}><Package size={18} /></SoftBox>
        <Box component="span"><SoftTypography component="span" variant="button" fontWeight="bold" display="block">{item.id}</SoftTypography><SoftTypography component="span" variant="caption" color="text" display="block">{item.recipient}</SoftTypography></Box>
      </Box>,
      destination: <Box component="span"><SoftTypography component="span" variant="button" display="block">{item.city}</SoftTypography><SoftTypography component="span" variant="caption" color="text">{item.window}</SoftTypography></Box>,
      status: <StatusBadge status={item.status} />,
      payment: item.cod ? lkr(item.cod) + ' COD' : 'Prepaid',
      action: <SoftButton variant="text" color="dark" size="small" aria-label={`View ${item.id}`} onClick={() => openDetails(item.id)} sx={{ fontWeight: 'bold' }}>Details <ArrowRight size={13} style={{ marginLeft: 5 }} /></SoftButton>,
    }))} />;
  }

  function overview() {
    const completion = deliveries.length ? Math.round(history.length / deliveries.length * 100) : 0;
    return <Grid container spacing={3}>
      <Grid item xs={12}>
        <Card sx={{ overflow: 'hidden' }}>
          <Grid container>
            <Grid item xs={12} md={7}>
              <Box sx={{ p: { xs: 3, lg: 4 }, height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
                <SoftTypography variant="caption" color="text" fontWeight="bold" sx={{ letterSpacing: 1.5, textTransform: 'uppercase' }}>Your shift starts here</SoftTypography>
                <SoftTypography variant="h3" fontWeight="bold" mt={1} sx={{ maxWidth: 460 }}>Good deliveries start with a clear plan.</SoftTypography>
                <SoftTypography variant="body2" color="text" mt={1.5} mb={3} sx={{ maxWidth: 430 }}>Review your next stop, check the parcel details, and head out ready for every handover.</SoftTypography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5, mt: 'auto' }}>
                  <SoftButton component={Link} to={go('route')} variant="gradient" color="primary" sx={{ ...primaryAction, px: 2.5 }}>View route <ArrowRight size={16} style={{ marginLeft: 8 }} /></SoftButton>
                  <SoftButton component={Link} to={go('deliveries')} variant="outlined" color="dark">View assignments</SoftButton>
                </Box>
              </Box>
            </Grid>
            <Grid item xs={12} md={5} sx={{ minHeight: { xs: 220, md: 310 } }}>
              <Box component="img" src={courierIllustration} alt="Courier riding a delivery scooter with parcels" sx={{ width: '100%', height: '100%', maxHeight: { xs: 280, md: 400 }, display: 'block', objectFit: 'cover', objectPosition: 'center', backgroundColor: '#f7f3e9' }} />
            </Grid>
          </Grid>
        </Card>
      </Grid>
      {[
        { title: 'Assigned deliveries', count: previewMode ? deliveries.length : 'Unavailable', icon: 'inventory_2', color: 'primary' },
        { title: 'On the way', count: previewMode ? pending.length : 'Unavailable', icon: 'local_shipping', color: 'info' },
        { title: 'Delivered this shift', count: previewMode ? delivered.length : 'Unavailable', icon: 'task_alt', color: 'success' },
        { title: 'COD collected', count: previewMode ? lkr(delivered.reduce((sum, item) => sum + item.cod, 0)) : 'Unavailable', icon: 'account_balance_wallet', color: 'warning' },
      ].map(({ title, count, icon, color }) => <Grid item xs={12} sm={6} xl={3} key={title}>
        <MiniStatisticsCard title={{ text: title, fontWeight: 'medium' }} count={count} percentage={{ color: 'secondary', text: '' }} icon={{ color, component: icon }} />
      </Grid>)}
      <Grid item xs={12} lg={8}>
        <Box sx={{ display: 'grid', gap: 3 }}>
          <Card sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2 }}>
              <SoftBox variant="gradient" bgColor="info" borderRadius="lg" p={1.25} display="flex" sx={{ color: 'white' }}><MapPin size={20} /></SoftBox>
              <Box><SoftTypography variant="caption" color="text" fontWeight="bold">UP NEXT</SoftTypography><SoftTypography variant="h6" fontWeight="bold">Your next handover</SoftTypography></Box>
            </Box>
            {next ? <>
              <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 2, flexWrap: 'wrap' }}>
                <Box><SoftTypography variant="h5" fontWeight="bold">{next.recipient}</SoftTypography><SoftTypography variant="body2" color="text" mt={0.5}>{next.address}, {next.city}</SoftTypography></Box>
                <StatusBadge status={next.status} />
              </Box>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3, mt: 2.5, mb: 2.5 }}>
                <Details items={[["Delivery window", next.window]]} />
                <Details items={[["Tracking number", next.id]]} />
                <Details items={[["Payment", next.cod ? lkr(next.cod) + ' COD' : 'Prepaid']]} />
              </Box>
              <SoftButton variant="gradient" color="primary" sx={primaryAction} onClick={() => openDetails(next.id)}>Review next parcel <ArrowRight size={14} style={{ marginLeft: 6 }} /></SoftButton>
            </> : <SoftTypography variant="body2" color="text">{previewMode ? 'Your queue is clear. No pending handovers.' : 'Connect dispatch to see your next handover.'}</SoftTypography>}
          </Card>
          <Card>
            <Box sx={{ p: 3, display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: 2 }}><Box><SoftTypography variant="h6" fontWeight="bold">Delivery queue</SoftTypography><SoftTypography variant="button" color="text" fontWeight="regular">Keep every upcoming handover in view.</SoftTypography></Box><SoftButton component={Link} to={go('deliveries')} variant="text" color="dark">View all <ArrowRight size={14} style={{ marginLeft: 6 }} /></SoftButton></Box>
            {parcelTable(pending.slice(0, 3))}
          </Card>
        </Box>
      </Grid>
      <Grid item xs={12} lg={4}>
        <Box sx={{ display: 'grid', gap: 3 }}>
          <Card sx={{ p: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold">Your availability</SoftTypography>
            <SoftTypography id="availability-help" variant="button" color="text" fontWeight="regular" mt={1} display="block">{previewMode ? 'Try your shift availability. This does not notify dispatch.' : 'Availability updates will be enabled when dispatch is connected.'}</SoftTypography>
            <SoftBox bgColor="grey-100" borderRadius="lg" p={2} mt={2}>
              <Box aria-live="polite"><SoftTypography variant="button" fontWeight="bold" display="block">{previewMode ? isAvailable ? 'Available for assignments' : 'You are offline' : 'Availability unavailable'}</SoftTypography><SoftTypography variant="caption" color="text">{previewMode ? 'Preview availability only' : 'Status not synced with dispatch'}</SoftTypography></Box>
              <SoftButton fullWidth role="switch" aria-label="Courier availability" aria-checked={isAvailable} aria-describedby="availability-help" disabled={!previewMode} variant={isAvailable ? 'outlined' : 'gradient'} color={isAvailable ? 'dark' : 'primary'} sx={{ ...primaryAction, mt: 2 }} onClick={() => setIsAvailable((value) => !value)}><Power size={15} style={{ marginRight: 6 }} />{isAvailable ? 'Go offline' : 'Go available'}</SoftButton>
            </SoftBox>
          </Card>
          <Card sx={{ p: 3 }}>
            <SoftTypography variant="h6" fontWeight="bold">Shift progress</SoftTypography>
            <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1, mt: 2 }}><SoftTypography variant="h3" fontWeight="bold">{previewMode ? `${completion}%` : 'Unavailable'}</SoftTypography><SoftTypography variant="caption" color="text">completed</SoftTypography></Box>
            <LinearProgress aria-label="Shift progress" variant="determinate" value={completion} sx={{ mt: 2, mb: 1.5 }} />
            <SoftTypography variant="button" color="text" fontWeight="regular">{previewMode ? `${history.length} of ${deliveries.length} stops completed` : 'Waiting for dispatch data'}</SoftTypography>
            <Box mt={2}><SoftButton component={Link} to={go('history')} variant="text" color="dark" sx={{ px: 0 }}>View delivery history <ArrowRight size={14} style={{ marginLeft: 6 }} /></SoftButton></Box>
          </Card>
        </Box>
      </Grid>
    </Grid>;
  }

  function deliveriesPage() {
    return <Card>
      <Box sx={{ p: 3, display: 'flex', flexWrap: 'wrap', gap: 2, justifyContent: 'space-between', alignItems: 'center' }}>
        <Box><SoftTypography variant="h6" fontWeight="bold">{labels[activeTab]}</SoftTypography><SoftTypography variant="button" color="text" fontWeight="regular">{visible.length} parcels found</SoftTypography></Box>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', width: { xs: '100%', md: 'auto' }, '& > .MuiInputBase-root': { flex: '1 1 220px', minWidth: 0 } }}>
          <SoftInput placeholder="Tracking number, recipient or city" inputProps={{ 'aria-label': 'Search deliveries' }} value={query} onChange={(event: React.ChangeEvent<HTMLInputElement>) => setQuery(event.target.value)} />
          <Box component="select" aria-label="Filter delivery status" value={filter} onChange={(event: React.ChangeEvent<HTMLSelectElement>) => setFilter(event.target.value)} sx={{ border: '1px solid #d2d6da', borderRadius: 2, px: 1.5, py: 1, backgroundColor: 'white', fontSize: 13, color: '#344767', maxWidth: '100%' }}>{(activeTab === 'history' ? ['All', 'Delivered', 'Failed'] : ['All', 'Assigned', 'Picked up', 'Out for delivery']).map((status) => <option key={status}>{status}</option>)}</Box>
          {activeTab === 'history' && <SoftButton variant="outlined" color="dark" disabled={!history.length} onClick={exportHistory}><Download size={14} style={{ marginRight: 6 }} />Export CSV</SoftButton>}
        </Box>
      </Box>{parcelTable(visible)}
    </Card>;
  }

  function routePage() {
    return <Grid container spacing={3}>
      <Grid item xs={12} lg={8}><Card sx={{ p: 3 }}><SoftTypography variant="h6" fontWeight="bold" mb={3}>Stop sequence</SoftTypography>
        {!pending.length ? <EmptyState title="No active route" text="Your next route will appear when deliveries are assigned." /> : <Box component="ol" sx={{ listStyle: 'none', m: 0, p: 0 }}>{pending.map((item, index) => <Box component="li" key={item.id} sx={{ display: 'flex', gap: 2, py: 2, borderBottom: '1px solid #f1f5f9' }}>
          <SoftBox variant="gradient" bgColor={index === 0 ? 'primary' : 'info'} borderRadius="lg" sx={{ width: 36, height: 36, flexShrink: 0, display: 'grid', placeItems: 'center', color: '#00274c', fontSize: 14, fontWeight: 'bold' }}>{index + 1}</SoftBox>
          <Box sx={{ minWidth: 0, flex: 1 }}><Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: 1 }}><SoftTypography variant="button" fontWeight="bold">{item.recipient}</SoftTypography><StatusBadge status={item.status} /></Box><SoftTypography variant="body2" color="text" mt={0.5}>{item.address}, {item.city}</SoftTypography><SoftTypography variant="caption" color="text">{item.window} / {item.id}</SoftTypography><Box mt={1}><SoftButton variant="text" color="dark" size="small" onClick={() => openDetails(item.id)} sx={{ px: 0 }}>Manage stop <ArrowRight size={13} style={{ marginLeft: 5 }} /></SoftButton></Box></Box>
        </Box>)}</Box>}
      </Card></Grid>
      <Grid item xs={12} lg={4}><Card sx={{ p: 3 }}><Navigation size={24} color="#17c1e8" /><SoftTypography variant="h6" fontWeight="bold" mt={2}>Route summary</SoftTypography><SoftTypography variant="body2" color="text" mt={1} mb={3}>{previewMode ? 'A sample stop order for exploring the workflow. Live maps, location tracking, and optimized directions are not connected.' : 'Route details require a dispatch connection.'}</SoftTypography><Details items={[["Remaining stops", previewMode ? String(pending.length) : 'Unavailable'], ['Distance / ETA', 'Unavailable']]} /></Card></Grid>
    </Grid>;
  }


  return <Box sx={{ '& .MuiCard-root': { minWidth: 0 } }}>
    <Alert severity={previewMode ? 'info' : 'warning'} sx={{ mb: 3, borderRadius: 3, fontSize: 12 }}>{previewMode ? 'Sample data / Changes stay in this preview session and reset when you reload.' : 'Dispatch is not connected. Live assignments, status changes, and route information are unavailable.'}</Alert>
    <Box sx={{ mb: 3 }}><SoftTypography component="h1" variant="h4" fontWeight="bold">{activeTab === 'overview' ? `Welcome, ${name}.` : labels[activeTab]}</SoftTypography><SoftTypography variant="body2" color="text" mt={0.5}>{descriptions[activeTab]}</SoftTypography></Box>
    {activeTab === 'overview' && overview()}
    {(activeTab === 'deliveries' || activeTab === 'history') && deliveriesPage()}
    {activeTab === 'route' && routePage()}
    {activeTab === 'profile' && <CourierProfile
      profile={accountProfile}
      previewMode={previewMode}
      notifications={notifications}
      onNotificationsChange={(enabled) => { setNotifications(enabled); setMessage('Preview notification preference updated.'); }}
      onSave={(updated) => { setProfile(updated); setMessage('Profile updated for this preview session.'); }}
      onReset={() => { setDeliveries(freshDeliveries()); setIsAvailable(false); setMessage('Preview shift reset.'); }}
    />}
    <Dialog open={Boolean(selected)} onClose={() => setSelectedId(null)} fullWidth maxWidth="sm" aria-labelledby="parcel-title"><DialogTitle id="parcel-title">Parcel details {selected?.id}</DialogTitle>{selected && <><DialogContent>
      <Box sx={{ display: 'grid', gap: 3 }}><StatusBadge status={selected.status} /><Details items={[
        ['Recipient', selected.recipient], ['Address', `${selected.address}, ${selected.city}`], ['Delivery window', selected.window], ['Parcel', selected.description], ['Weight', `${selected.weight} kg`], ['Payment', selected.cod ? `${lkr(selected.cod)} COD` : 'Prepaid'],
      ]} /><Alert severity="info">{selected.instructions}</Alert>
      {isCompleted(selected) && <Details items={[["Delivery record", selected.proof || ''], ['Completed at', selected.completedAt ? new Date(selected.completedAt).toLocaleString('en-LK') : '']]} />}
      {outcome && <Box sx={{ display: 'grid', gap: 2 }}><TextField fullWidth multiline minRows={2} label={outcome === 'Delivered' ? 'Recipient name / handover note' : 'Reason for unsuccessful delivery'} value={proof} onChange={(event) => setProof(event.target.value)} error={Boolean(proofError)} helperText={proofError || 'Required for this preview delivery record.'} />{outcome === 'Delivered' && selected.cod > 0 && <FormControlLabel control={<Checkbox checked={codConfirmed} onChange={(event) => setCodConfirmed(event.target.checked)} />} label={<SoftTypography variant="button">I collected {lkr(selected.cod)}</SoftTypography>} />}</Box>}
      </Box></DialogContent><DialogActions sx={{ p: 3, flexWrap: 'wrap', gap: 1 }}><SoftButton variant="text" color="secondary" onClick={() => setSelectedId(null)}>Close</SoftButton>{!isCompleted(selected) && (outcome ? <SoftButton variant="gradient" color="primary" sx={primaryAction} onClick={confirmOutcome}>Confirm {outcome === 'Delivered' ? 'delivery' : 'failed attempt'}</SoftButton> : <>{selected.status === 'Assigned' && <SoftButton variant="gradient" color="primary" sx={primaryAction} disabled={!previewMode} onClick={() => updateStatus(selected.id, 'Picked up')}>Confirm pickup</SoftButton>}{selected.status === 'Picked up' && <SoftButton variant="gradient" color="primary" sx={primaryAction} disabled={!previewMode} onClick={() => updateStatus(selected.id, 'Out for delivery')}>Start delivery</SoftButton>}{selected.status === 'Out for delivery' && <SoftButton variant="gradient" color="primary" sx={primaryAction} disabled={!previewMode} onClick={() => setOutcome('Delivered')}>Mark delivered</SoftButton>}<SoftButton variant="outlined" color="dark" disabled={!previewMode} onClick={() => setOutcome('Failed')}>Report issue</SoftButton></>)}</DialogActions></>}</Dialog>
    <Snackbar open={Boolean(message)} autoHideDuration={4500} onClose={() => setMessage('')} message={message} />
  </Box>;
};
