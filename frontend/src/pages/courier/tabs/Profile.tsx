import React, { useState } from 'react';
import { Avatar, Box, Card, Dialog, DialogActions, DialogContent, DialogTitle, Grid, Switch } from '@mui/material';
import { Building2, Check, Edit2, Mail, RotateCcw, Truck, UserRound } from 'lucide-react';
import SoftTypography from 'components/SoftTypography';
import SoftButton from 'components/SoftButton';
import SoftInput from 'components/SoftInput';

export interface CourierProfileDetails { name: string; email: string; phone: string }
interface CourierProfileProps {
  profile: CourierProfileDetails;
  previewMode: boolean;
  notifications: boolean;
  onNotificationsChange: (enabled: boolean) => void;
  onSave: (profile: CourierProfileDetails) => void;
  onReset: () => void;
}
const actionStyle = { color: '#00274c !important', fontWeight: 'bold' };

function SectionHeading({ title, description }: { title: string; description: string }) {
  return <Box sx={{ mb: 3 }}>
    <SoftTypography variant="h6" fontWeight="bold">{title}</SoftTypography>
    <SoftTypography variant="button" color="text" fontWeight="regular" display="block" mt={0.5}>{description}</SoftTypography>
  </Box>;
}

export function CourierProfile({ profile, previewMode, notifications, onNotificationsChange, onSave, onReset }: CourierProfileProps) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(profile);
  const [nameError, setNameError] = useState('');
  const [resetOpen, setResetOpen] = useState(false);
  const initials = profile.name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'C';
  const fields = [{ key: 'name' as const, label: 'Full name', type: 'text' }, { key: 'email' as const, label: 'Email address', type: 'email' }, { key: 'phone' as const, label: 'Phone number', type: 'tel' }];

  return <Grid container spacing={3} alignItems="flex-start">
    <Grid item xs={12} lg={4}>
      <Box sx={{ display: 'grid', gap: 3 }}>
        <Card sx={{ overflow: 'hidden' }}>
          <Box sx={{ height: 80, background: 'linear-gradient(120deg, #00274c, #16446d)', position: 'relative' }}>
            <Box sx={{ position: 'absolute', bottom: 0, left: 24, width: 42, height: 4, bgcolor: '#ffcb05', borderRadius: '4px 4px 0 0' }} />
          </Box>
          <Box sx={{ px: 3, pb: 3 }}>
            <Avatar sx={{ mt: -3.5, mb: 2, width: 64, height: 64, bgcolor: '#fff8da', color: '#00274c', border: '4px solid white', fontSize: 22, fontWeight: 700 }}>{initials}</Avatar>
            <SoftTypography variant="h5" fontWeight="bold" sx={{ overflowWrap: 'anywhere' }}>{profile.name}</SoftTypography>
            <SoftTypography variant="button" color="text" display="block" mt={0.5}>SwiftDrop courier</SoftTypography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 2, p: 1.5, bgcolor: '#f8fafc', borderRadius: 2 }}>
              <UserRound size={16} color="#64748b" />
              <SoftTypography variant="caption" color="text">{previewMode ? 'Preview account' : 'Signed-in account'}</SoftTypography>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, mt: 2 }}><Mail size={15} color="#64748b" style={{ marginTop: 3, flexShrink: 0 }} /><SoftTypography variant="button" color="text" fontWeight="regular" sx={{ overflowWrap: 'anywhere' }}>{profile.email || 'Email not provided'}</SoftTypography></Box>
          </Box>
        </Card>

        <Card sx={{ p: 3 }}>
          <SectionHeading title="Work assignment" description="Your hub and vehicle are managed by dispatch." />
          {[{ icon: Building2, label: 'Assigned hub', text: 'No hub assigned' }, { icon: Truck, label: 'Vehicle', text: 'No vehicle assigned' }].map(({ icon: Icon, label, text }, index) => <Box key={label} sx={{ display: 'flex', gap: 1.5, py: 2, borderTop: index ? '1px solid #eef2f6' : undefined }}>
            <Box sx={{ width: 36, height: 36, borderRadius: 2, bgcolor: '#f8fafc', display: 'grid', placeItems: 'center', flexShrink: 0 }}><Icon size={18} color="#64748b" /></Box>
            <Box><SoftTypography variant="caption" color="text" display="block">{label}</SoftTypography><SoftTypography variant="button" fontWeight="medium">{text}</SoftTypography></Box>
          </Box>)}
          <SoftTypography variant="caption" color="text" display="block" mt={1}>Assignments will appear here once dispatch is connected.</SoftTypography>
        </Card>
      </Box>
    </Grid>

    <Grid item xs={12} lg={8}>
      <Box sx={{ display: 'grid', gap: 3 }}>
        <Card sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-start', gap: 2, mb: 2 }}>
            <SectionHeading title="Personal details" description={previewMode ? 'Keep your contact information up to date in this preview.' : 'The contact information associated with your account.'} />
            {previewMode && !editing && <SoftButton variant="outlined" color="dark" size="small" onClick={() => { setDraft(profile); setNameError(''); setEditing(true); }}><Edit2 size={13} style={{ marginRight: 6 }} />Edit details</SoftButton>}
          </Box>
          {editing ? <Box component="form" onSubmit={(event: React.FormEvent) => {
            event.preventDefault();
            if (!draft.name.trim()) { setNameError('Enter your full name.'); return; }
            onSave({ name: draft.name.trim(), email: draft.email.trim(), phone: draft.phone.trim() }); setEditing(false);
          }}>
            <Grid container spacing={2.5}>{fields.map(({ key, label, type }) => <Grid item xs={12} sm={key === 'name' ? 12 : 6} key={key}>
              <SoftTypography component="label" htmlFor={`courier-${key}`} variant="caption" fontWeight="bold" display="block" mb={1}>{label}</SoftTypography>
              <SoftInput id={`courier-${key}`} type={type} value={draft[key]} fullWidth required={key !== 'phone'} error={key === 'name' && Boolean(nameError)} inputProps={key === 'name' ? { 'aria-describedby': nameError ? 'courier-name-error' : undefined } : undefined} onChange={(event: React.ChangeEvent<HTMLInputElement>) => { setDraft({ ...draft, [key]: event.target.value }); if (key === 'name') setNameError(''); }} />
              {key === 'name' && nameError && <SoftTypography id="courier-name-error" variant="caption" color="error">{nameError}</SoftTypography>}
            </Grid>)}</Grid>
            <Box sx={{ mt: 3, pt: 2.5, borderTop: '1px solid #eef2f6', display: 'flex', flexWrap: 'wrap', justifyContent: 'flex-end', gap: 1 }}>
              <SoftButton variant="text" color="secondary" onClick={() => setEditing(false)}>Cancel</SoftButton>
              <SoftButton type="submit" variant="gradient" color="primary" sx={actionStyle}><Check size={14} style={{ marginRight: 6 }} />Save changes</SoftButton>
            </Box>
          </Box> : <Grid container spacing={3}>{fields.map(({ key, label }) => <Grid item xs={12} sm={key === 'name' ? 12 : 6} key={key}><SoftTypography variant="caption" color="text" display="block" mb={0.5}>{label}</SoftTypography><SoftTypography variant="body2" fontWeight="medium" sx={{ overflowWrap: 'anywhere' }}>{profile[key] || 'Not provided'}</SoftTypography></Grid>)}</Grid>}
          <SoftTypography variant="caption" color="text" display="block" mt={3}>{previewMode ? 'Profile edits stay in this preview session. Your sign-in account is unchanged.' : 'Contact your administrator to update account details.'}</SoftTypography>
        </Card>

        <Card sx={{ p: 3 }}>
          <SectionHeading title="Notifications" description="Choose what you want to hear about during a shift." />
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 2 }}>
            <Box><SoftTypography component="label" htmlFor="courier-notifications" variant="button" fontWeight="bold">Assignment notifications</SoftTypography><SoftTypography id="courier-notifications-help" variant="caption" color="text" display="block" mt={0.5}>New parcels and changes to your delivery queue.</SoftTypography></Box>
            <Switch checked={notifications} disabled={!previewMode} inputProps={{ id: 'courier-notifications', 'aria-label': 'Assignment notifications', 'aria-describedby': 'courier-notifications-help' }} onChange={(event) => onNotificationsChange(event.target.checked)} />
          </Box>
          <SoftTypography variant="caption" color="text" display="block" mt={2}>{previewMode ? 'Preview preference only. No notifications are sent.' : 'Notification preferences will be available when dispatch is connected.'}</SoftTypography>
        </Card>

        {previewMode && <Card sx={{ p: 3, border: '1px dashed #d8e1eb', boxShadow: 'none', bgcolor: '#fafbfc' }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
            <Box><SoftTypography variant="button" fontWeight="bold" display="block">Preview controls</SoftTypography><SoftTypography variant="caption" color="text">Restore sample deliveries and start a fresh shift.</SoftTypography></Box>
            <SoftButton variant="text" color="secondary" size="small" onClick={() => setResetOpen(true)}><RotateCcw size={13} style={{ marginRight: 6 }} />Reset preview shift</SoftButton>
          </Box>
        </Card>}
      </Box>
    </Grid>

    <Dialog open={resetOpen} onClose={() => setResetOpen(false)} aria-labelledby="reset-shift-title" fullWidth maxWidth="xs">
      <DialogTitle id="reset-shift-title">Reset preview shift?</DialogTitle>
      <DialogContent><SoftTypography variant="body2" color="text">This clears delivery progress and restores sample parcels. Your profile details are kept.</SoftTypography></DialogContent>
      <DialogActions sx={{ p: 3 }}><SoftButton variant="text" color="secondary" onClick={() => setResetOpen(false)}>Keep progress</SoftButton><SoftButton variant="gradient" color="primary" sx={actionStyle} onClick={() => { onReset(); setResetOpen(false); }}>Reset shift</SoftButton></DialogActions>
    </Dialog>
  </Grid>;
}
