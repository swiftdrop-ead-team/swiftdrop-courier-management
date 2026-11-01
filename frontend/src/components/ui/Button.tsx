import React from 'react';
import { Loader2 } from 'lucide-react';

export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  loading?: boolean;
  fullWidth?: boolean;
}

const variantClasses: Record<ButtonVariant, string> = {
  primary: 'bg-yellow-500 text-navy-950 shadow-lg shadow-yellow-500/20 hover:bg-yellow-400 active:bg-yellow-600',
  secondary: 'bg-navy-900 text-white shadow-lg shadow-navy-900/15 hover:bg-navy-800 active:bg-navy-950',
  outline: 'border border-slate-200 bg-white text-slate-700 hover:border-navy-200 hover:bg-slate-50 hover:text-navy-900',
  ghost: 'text-slate-600 hover:bg-slate-100 hover:text-navy-900',
};

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ children, className = '', variant = 'primary', loading = false, fullWidth = false, disabled, ...props }, ref) => (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={`inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-4 py-3 text-sm font-bold transition-all duration-200 disabled:cursor-not-allowed disabled:opacity-60 ${variantClasses[variant]} ${fullWidth ? 'w-full' : ''} ${className}`}
      {...props}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
      {children}
    </button>
  )
);

Button.displayName = 'Button';
