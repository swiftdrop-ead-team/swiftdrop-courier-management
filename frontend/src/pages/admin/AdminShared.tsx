import React from 'react';
import { Box } from '@mui/material';
import { Search } from 'lucide-react';
import SoftTypography from 'components/SoftTypography';
import SoftBadge from 'components/SoftBadge';

export function AdminEmpty({ title, text }: { title: string; text: string }) {
  return <Box sx={{ p: 5, textAlign: 'center' }}>
    <Search size={28} style={{ margin: '0 auto 12px', color: '#8392ab' }} />
    <SoftTypography variant="h6" fontWeight="bold">{title}</SoftTypography>
    <SoftTypography variant="button" fontWeight="regular" color="text">{text}</SoftTypography>
  </Box>;
}

export function AdminDetails({ items }: { items: [string, string][] }) {
  return <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2.5 }}>{items.map(([label, value]) => <Box key={label}>
    <SoftTypography variant="caption" color="text" display="block" mb={0.5}>{label}</SoftTypography>
    <SoftTypography variant="body2" sx={{ overflowWrap: 'anywhere', whiteSpace: 'pre-wrap' }}>{value || 'Not provided'}</SoftTypography>
  </Box>)}</Box>;
}

export function AdminBadge({ status }: { status: string }) {
  const color = ['ACTIVE', 'APPROVED'].includes(status) ? 'success' : ['REJECTED', 'SUSPENDED'].includes(status) ? 'error' : status === 'PENDING' ? 'warning' : 'secondary';
  return <SoftBadge badgeContent={status} color={color} size="xs" container />;
}

export const adminActionStyle = { color: '#00274c !important', fontWeight: 'bold' };
export const adminSelectStyle = { border: '1px solid #d2d6da', borderRadius: 2, px: 1.5, py: 1, bgcolor: 'white', fontSize: 13, color: '#344767', maxWidth: '100%', minHeight: 40 };
