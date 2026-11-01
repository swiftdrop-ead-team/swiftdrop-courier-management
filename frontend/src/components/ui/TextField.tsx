import React from 'react';

interface TextFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  icon?: React.ReactNode;
  endAdornment?: React.ReactNode;
}

export const TextField = React.forwardRef<HTMLInputElement, TextFieldProps>(
  ({ label, error, icon, endAdornment, id, className = '', ...props }, ref) => {
    const inputId = id || props.name;

    return (
      <div className="space-y-1.5">
        <label htmlFor={inputId} className="block text-xs font-bold uppercase tracking-[0.12em] text-slate-600">
          {label}
        </label>
        <div className="relative">
          {icon && (
            <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-400" aria-hidden="true">
              {icon}
            </span>
          )}
          <input
            ref={ref}
            id={inputId}
            aria-invalid={Boolean(error)}
            aria-describedby={error ? `${inputId}-error` : undefined}
            className={`min-h-12 w-full rounded-lg border bg-white text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:ring-4 ${icon ? 'pl-10' : 'pl-4'} ${endAdornment ? 'pr-12' : 'pr-4'} ${error ? 'border-red-300 focus:border-red-500 focus:ring-red-100' : 'border-slate-200 focus:border-yellow-500 focus:ring-yellow-100'} ${className}`}
            {...props}
          />
          {endAdornment && <span className="absolute inset-y-0 right-0 flex items-center pr-3.5">{endAdornment}</span>}
        </div>
        {error && <p id={`${inputId}-error`} className="break-words text-xs font-medium text-red-600">{error}</p>}
      </div>
    );
  }
);

TextField.displayName = 'TextField';
