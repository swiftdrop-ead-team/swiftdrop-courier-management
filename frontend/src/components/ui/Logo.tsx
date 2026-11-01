import React from 'react';

interface LogoProps {
  className?: string;
  variant?: 'light' | 'dark';
  size?: 'sm' | 'md' | 'lg';
}

export const Logo: React.FC<LogoProps> = ({ className = '', variant = 'dark', size = 'md' }) => {
  const sizeClasses = {
    sm: 'h-7 text-lg',
    md: 'h-9 text-xl',
    lg: 'h-12 text-2xl',
  };

  const isLight = variant === 'light';

  return (
    <div className={`flex items-center gap-2.5 font-bold tracking-tight select-none ${className}`}>
      {/* Brand Icon SVG */}
      <div className={`relative flex items-center justify-center rounded-xl p-2 shadow-sm ${
        isLight ? 'bg-white text-navy-900 shadow-slate-900/20' : 'bg-gradient-to-tr from-navy-900 via-navy-800 to-teal-700 text-white shadow-teal-900/20'
      }`}>
        <svg 
          viewBox="0 0 24 24" 
          fill="none" 
          stroke="currentColor" 
          strokeWidth="2.2" 
          strokeLinecap="round" 
          strokeLinejoin="round" 
          className={size === 'sm' ? 'w-4 h-4' : size === 'lg' ? 'w-7 h-7' : 'w-5 h-5'}
        >
          {/* Fast box with delivery speed lines */}
          <path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z" />
          <path d="m3.3 7 8.7 5 8.7-5" />
          <path d="M12 22V12" />
          <polyline points="12 12 19 8 19 12" className="text-teal-400 stroke-teal-400" />
        </svg>
      </div>

      {/* Brand Typography */}
      <div className={`flex flex-col leading-none ${sizeClasses[size]}`}>
        <span className="flex items-center font-extrabold tracking-tight">
          <span className={isLight ? 'text-white' : 'text-navy-900'}>Swift</span>
          <span className="text-teal-600">Drop</span>
        </span>
        <span className={`text-[9px] font-semibold tracking-wider uppercase ${isLight ? 'text-slate-300' : 'text-slate-500'}`}>
          Courier Tech
        </span>
      </div>
    </div>
  );
};
