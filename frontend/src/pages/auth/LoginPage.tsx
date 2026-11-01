import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../../hooks/useAuth';
import { Logo } from '../../components/ui/Logo';
import { Button } from '../../components/ui/Button';
import { Notice } from '../../components/ui/Notice';
import { TextField } from '../../components/ui/TextField';
import { 
  Eye, 
  EyeOff, 
  Lock, 
  Mail, 
  ArrowRight, 
} from 'lucide-react';

const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
  rememberMe: z.boolean().optional(),
});

type LoginFormData = z.infer<typeof loginSchema>;

export const LoginPage: React.FC = () => {
  const { login, getDefaultDashboardRoute } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(
    searchParams.get('error') === 'oauth2_failure'
      ? 'Google OAuth login could not be completed. Please use email credentials or verify Google OAuth environment keys.'
      : null
  );

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
      rememberMe: true,
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    setServerError(null);
    try {
      const user = await login({
        email: data.email,
        password: data.password,
        rememberMe: data.rememberMe,
      });

      // Role-based redirect
      const targetRoute = getDefaultDashboardRoute(user.roles);
      navigate(targetRoute, { replace: true });
    } catch (err: any) {
      if (err.response?.data?.message) {
        setServerError(err.response.data.message);
      } else {
        setServerError('Unable to connect to SwiftDrop authentication service. Please check API Gateway status.');
      }
    }
  };

  const handleGoogleLogin = () => {
    // Redirect to Spring Security OAuth2 Google authorization endpoint
    const gatewayUrl = import.meta.env.VITE_GATEWAY_URL || 'http://localhost:8080';
    window.location.href = `${gatewayUrl}/oauth2/authorization/google`;
  };

  return (
    <div className="min-h-[100dvh] bg-gradient-to-br from-navy-700 via-navy-900 to-navy-950 p-5 sm:p-8 lg:h-[100dvh] lg:overflow-hidden lg:p-6">
      <div className="mx-auto flex h-auto max-w-5xl overflow-hidden rounded-lg bg-white shadow-2xl shadow-navy-950/40 lg:h-[calc(100dvh-3rem)] lg:min-h-0 lg:flex-row">
      <div className="relative hidden overflow-hidden bg-gradient-to-br from-blue-500 via-blue-600 to-blue-800 p-10 text-white lg:flex lg:w-[48%] lg:flex-col lg:justify-between xl:p-12">
        <img src="/auth-abstract.svg" alt="" aria-hidden="true" className="pointer-events-none absolute inset-0 h-full w-full -scale-y-100 object-cover opacity-90" />

        <div className="relative z-10">
          <Link to="/">
            <Logo variant="light" size="lg" />
          </Link>
        </div>

        <div className="relative z-10 max-w-sm">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-blue-100">SwiftDrop logistics</p>
          <h2 className="mt-4 text-4xl font-black leading-tight tracking-tight xl:text-5xl">
            Move every delivery forward.
          </h2>
          <p className="mt-5 max-w-xs text-sm leading-6 text-blue-100">
            One clear workspace for every shipment, update, and handoff.
          </p>
        </div>

        <p className="relative z-10 text-xs text-blue-100/70">Simple. Reliable. On time.</p>
      </div>

      {/* ==================================================================== */}
      {/* Right Panel: Authentication Form                                      */}
      {/* ==================================================================== */}
      <div className="auth-form-panel relative mx-auto flex min-h-full w-full max-w-2xl flex-1 flex-col justify-start overflow-x-hidden overflow-y-auto bg-white px-6 py-12 sm:px-12 lg:w-[54%] lg:max-w-none lg:px-12 lg:py-8 xl:px-16">
        <div className="auth-form-content w-full">
        {/* Mobile Header Logo */}
        <div className="lg:hidden mb-8">
          <Link to="/">
            <Logo size="md" />
          </Link>
        </div>

        <div className="mb-8 space-y-3">
          <h1 className="text-4xl font-black tracking-tight text-ink">
            Welcome back
          </h1>
          <p className="max-w-md text-sm leading-6 text-slate-500">
            Enter your credentials to access your SwiftDrop dashboard.
          </p>
        </div>

        {/* Server Error Alert */}
        {serverError && (
          <Notice title="Authentication Error" className="mb-6 animate-fade-in">
            {serverError}
          </Notice>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          {/* Email field */}
          <TextField
            label="Email Address"
            type="email"
            placeholder="name@company.com"
            icon={<Mail className="h-4 w-4" />}
            error={errors.email?.message}
            {...register('email')}
          />

          {/* Password field */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <span className="block text-xs font-bold uppercase tracking-[0.12em] text-slate-600">Password</span>
              <Link
                to="/forgot-password"
                className="text-xs font-semibold text-navy-800 hover:text-navy-900 transition-colors"
              >
                Forgot password?
              </Link>
            </div>
            <TextField
              label=""
              type={showPassword ? 'text' : 'password'}
              placeholder="••••••••"
              icon={<Lock className="h-4 w-4" />}
              error={errors.password?.message}
              endAdornment={
                <button
                  type="button"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  onClick={() => setShowPassword(!showPassword)}
                  className="text-slate-400 hover:text-slate-700"
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              }
              {...register('password')}
            />
          </div>

          {/* Remember Me */}
          <div className="flex items-center">
            <input
              id="rememberMe"
              type="checkbox"
              {...register('rememberMe')}
              className="w-4 h-4 text-yellow-500 border-slate-300 rounded focus:ring-yellow-400 cursor-pointer"
            />
            <label htmlFor="rememberMe" className="ml-2 text-xs font-medium text-slate-600 cursor-pointer">
              Remember this browser session
            </label>
          </div>

          {/* Sign In Button */}
          <Button
            type="submit"
            loading={isSubmitting}
            fullWidth
          >
            {isSubmitting ? 'Authenticating...' : <><span>Sign In to Account</span><ArrowRight className="h-4 w-4" /></>}
          </Button>
        </form>

        {/* Divider */}
        <div className="relative my-7">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-slate-200"></div>
          </div>
          <div className="relative flex justify-center text-xs uppercase">
            <span className="bg-white px-3 text-slate-400 font-semibold tracking-wider">
              Or continue with
            </span>
          </div>
        </div>

        {/* Google OAuth Button */}
        <Button
          type="button"
          onClick={handleGoogleLogin}
          variant="outline"
          fullWidth
        >
          <svg className="w-4 h-4" viewBox="0 0 24 24">
            <path
              fill="#4285F4"
              d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17z"
            />
            <path
              fill="#34A853"
              d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.33 24 12 24z"
            />
            <path
              fill="#FBBC05"
              d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.18 0 10.04 0 12s.45 3.82 1.25 5.42l4.03-3.15z"
            />
            <path
              fill="#EA4335"
              d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"
            />
          </svg>
          <span>Continue with Google</span>
        </Button>

        {/* Registration Links */}
        <div className="mt-8 pt-6 border-t border-slate-100 space-y-2 text-center text-xs text-slate-500">
          <p>
            Don't have an individual account?{' '}
            <Link to="/register" className="font-semibold text-yellow-600 hover:underline">
              Create Customer Account
            </Link>
          </p>
          <p>
            Are you a commercial business?{' '}
            <Link to="/merchant/register" className="font-semibold text-navy-900 hover:underline">
              Register as Merchant (Subject to Approval)
            </Link>
          </p>
        </div>
      </div>
        </div>
      </div>
    </div>
  );
};
