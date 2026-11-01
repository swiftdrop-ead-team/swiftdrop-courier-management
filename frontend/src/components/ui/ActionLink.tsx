import React from 'react';
import { Link, type LinkProps } from 'react-router-dom';
import { twMerge } from 'tailwind-merge';
import type { ButtonVariant } from './Button';

interface ActionLinkProps extends LinkProps {
  variant?: ButtonVariant;
  fullWidth?: boolean;
  className?: string;
}

const variantClasses: Record<ButtonVariant, string> = {
  primary: 'bg-yellow-500 text-navy-950 shadow-lg shadow-yellow-500/20 hover:bg-yellow-400',
  secondary: 'bg-navy-900 text-white shadow-lg shadow-navy-900/15 hover:bg-navy-800',
  outline: 'border border-slate-200 bg-white text-slate-700 hover:border-navy-200 hover:bg-slate-50 hover:text-navy-900',
  ghost: 'text-slate-600 hover:bg-slate-100 hover:text-navy-900',
};

export const ActionLink: React.FC<ActionLinkProps> = ({ children, variant = 'primary', fullWidth = false, className = '', ...props }) => (
  <Link
    className={twMerge(
      'inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-4 py-3 text-sm font-bold transition-all duration-200',
      variantClasses[variant],
      fullWidth && 'w-full',
      className
    )}
    {...props}
  >
    {children}
  </Link>
);
