import React, { useEffect, useState } from 'react';
import { Avatar, Box, Card, Grid } from '@mui/material';
import { Check, Edit2, ShieldCheck, Users, Building2 } from 'lucide-react';
import SoftTypography from 'components/SoftTypography';
import SoftInput from 'components/SoftInput';
import SoftButton from 'components/SoftButton';
import type { User } from '../../types/auth';
import { AdminBadge, AdminDetails, adminActionStyle } from './AdminShared';
import { dateTime } from './adminData';

export function AdminProfile({ account, previewMode, onSave }: { account: User; previewMode: boolean; onSave: (user: User) => void }) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(account);
  const [error, setError] = useState('');
  useEffect(() => { setDraft(account); setEditing(false); setError(''); }, [account]);
  const initials = account.fullName.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('') || 'A';
  return <Grid container spacing={3} alignItems="flex-start">
    <Grid item xs={12} lg={4}><Card sx={{ overflow: 'hidden' }}>
      <Box sx={{ height: 80, background: 'linear-gradient(120deg,#00274c,#16446d)', borderBottom: '4px solid #ffcb05' }} />
      <Box sx={{ px: 3, pb: 3 }}><Avatar sx={{ width: 64, height: 64, mt: -3.5, mb: 2, bgcolor: '#fff8da', color: '#00274c', border: '4px solid white', fontWeight: 700 }}>{initials}</Avatar>
        <SoftTypography variant="h5" fontWeight="bold" sx={{ overflowWrap: 'anywhere' }}>{account.fullName}</SoftTypography>
        <SoftTypography variant="button" color="text" display="block" mt={0.5}>Platform administrator</SoftTypography>
        <Box sx={{ mt: 2, mb: 3 }}><AdminBadge status={account.status} /></Box>
        <AdminDetails items={[["Account", previewMode ? 'Preview account' : 'Signed-in account'], ['Member since', dateTime(account.createdAt)]]} />
      </Box>
    </Card></Grid>
    <Grid item xs={12} lg={8}><Box sx={{ display: 'grid', gap: 3 }}>
      <Card sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: 2, mb: 3 }}><Box><SoftTypography variant="h6" fontWeight="bold">Personal details</SoftTypography><SoftTypography variant="button" color="text" fontWeight="regular">Your administrator contact information.</SoftTypography></Box>{previewMode && !editing && <SoftButton variant="outlined" color="dark" size="small" onClick={() => { setDraft(account); setEditing(true); setError(''); }}><Edit2 size={13} style={{ marginRight: 6 }} />Edit details</SoftButton>}</Box>
        {editing ? <Box component="form" onSubmit={(event: React.FormEvent) => { event.preventDefault(); if (!draft.fullName.trim()) { setError('Enter your full name.'); return; } onSave({ ...draft, fullName: draft.fullName.trim(), email: draft.email.trim(), phoneNumber: draft.phoneNumber?.trim() }); setEditing(false); }}>
          <Grid container spacing={2.5}>{[{ key: 'fullName' as const, label: 'Full name', type: 'text' }, { key: 'email' as const, label: 'Email address', type: 'email' }, { key: 'phoneNumber' as const, label: 'Phone number', type: 'tel' }].map(({ key, label, type }) => <Grid item xs={12} sm={key === 'fullName' ? 12 : 6} key={key}><SoftTypography component="label" htmlFor={`admin-${key}`} variant="caption" fontWeight="bold" display="block" mb={1}>{label}</SoftTypography><SoftInput id={`admin-${key}`} type={type} fullWidth required={key !== 'phoneNumber'} value={draft[key] || ''} onChange={(event: React.ChangeEvent<HTMLInputElement>) => { setDraft({ ...draft, [key]: event.target.value }); setError(''); }} /></Grid>)}</Grid>
          {error && <SoftTypography variant="caption" color="error">{error}</SoftTypography>}
          <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end', gap: 1 }}><SoftButton variant="text" color="secondary" onClick={() => setEditing(false)}>Cancel</SoftButton><SoftButton type="submit" variant="gradient" color="primary" sx={adminActionStyle}><Check size={14} style={{ marginRight: 6 }} />Save changes</SoftButton></Box>
        </Box> : <AdminDetails items={[["Full name", account.fullName], ['Email address', account.email], ['Phone number', account.phoneNumber || 'Not provided'], ['Roles', account.roles.join(', ')]]} />}
        <SoftTypography variant="caption" color="text" display="block" mt={3}>{previewMode ? 'Edits stay in this preview session. Your sign-in account is unchanged.' : 'Account editing is not available in this workspace yet.'}</SoftTypography>
      </Card>
      <Card sx={{ p: 3 }}><SoftTypography variant="h6" fontWeight="bold" mb={1}>Administrator access</SoftTypography><SoftTypography variant="button" color="text" fontWeight="regular">Tools available to the administrator role.</SoftTypography>
        {[{ Icon: Building2, title: 'Merchant reviews', text: 'Review registrations and record approval decisions.' }, { Icon: Users, title: 'Account directory', text: 'View user accounts, roles, and current account status.' }, { Icon: ShieldCheck, title: 'Protected workspace', text: previewMode ? 'Preview actions use sample data and do not contact the identity service.' : 'Live administration requests are authorized by the identity service.' }].map(({ Icon, title, text }) => <Box key={title} sx={{ display: 'flex', gap: 1.5, pt: 2.5 }}><Box sx={{ width: 36, height: 36, bgcolor: '#f8fafc', borderRadius: 2, display: 'grid', placeItems: 'center', flexShrink: 0 }}><Icon size={18} color="#00274c" /></Box><Box><SoftTypography variant="button" fontWeight="bold">{title}</SoftTypography><SoftTypography variant="caption" color="text" display="block" mt={0.5}>{text}</SoftTypography></Box></Box>)}
      </Card>
    </Box></Grid>
  </Grid>;
}
