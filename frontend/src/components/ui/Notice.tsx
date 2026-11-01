import React from 'react';
import { AlertCircle, CheckCircle2, Info } from 'lucide-react';

interface NoticeProps {
  title: string;
  children?: React.ReactNode;
  variant?: 'error' | 'info' | 'success';
  className?: string;
}

const noticeStyles = {
  error: {
    container: 'border-red-200 bg-red-50 text-red-900',
    icon: <AlertCircle className="h-5 w-5 text-red-600" aria-hidden="true" />,
  },
  info: {
    container: 'border-blue-200 bg-blue-50 text-blue-900',
    icon: <Info className="h-5 w-5 text-blue-600" aria-hidden="true" />,
  },
  success: {
    container: 'border-emerald-200 bg-emerald-50 text-emerald-900',
    icon: <CheckCircle2 className="h-5 w-5 text-emerald-600" aria-hidden="true" />,
  },
};

export const Notice: React.FC<NoticeProps> = ({ title, children, variant = 'error', className = '' }) => {
  const styles = noticeStyles[variant];

  return (
    <div role="alert" className={`flex items-start gap-3 rounded-lg border p-4 text-sm ${styles.container} ${className}`}>
      <span className="shrink-0">{styles.icon}</span>
      <div className="min-w-0">
        <p className="font-bold">{title}</p>
        {children && <div className="mt-1 break-words text-xs leading-relaxed opacity-90">{children}</div>}
      </div>
    </div>
  );
};
