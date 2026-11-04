import React from 'react';
import { LucideIcon } from 'lucide-react';

type SoftColor = 'primary' | 'info' | 'success' | 'warning' | 'dark';

interface SoftCardProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
}

interface SoftStatCardProps {
  label: string;
  value: string | number;
  helper?: string;
  icon: LucideIcon;
  color?: SoftColor;
}

interface SoftBadgeProps {
  children: React.ReactNode;
  color?: SoftColor;
  className?: string;
}

export const SoftCard: React.FC<SoftCardProps> = ({ children, className = '', ...props }) => (
  <div className={`soft-card ${className}`} {...props}>
    {children}
  </div>
);

export const SoftStatCard: React.FC<SoftStatCardProps> = ({
  label,
  value,
  helper,
  icon: Icon,
  color = 'primary',
}) => (
  <SoftCard className="soft-stat-card">
    <div className={`soft-icon soft-icon-${color}`}>
      <Icon aria-hidden="true" size={18} strokeWidth={2.25} />
    </div>
    <div className="min-w-0">
      <span className="soft-stat-label">{label}</span>
      <strong className="soft-stat-value">{value}</strong>
      {helper && <span className="soft-stat-helper">{helper}</span>}
    </div>
  </SoftCard>
);

export const SoftBadge: React.FC<SoftBadgeProps> = ({
  children,
  color = 'primary',
  className = '',
}) => <span className={`soft-badge soft-badge-${color} ${className}`}>{children}</span>;
