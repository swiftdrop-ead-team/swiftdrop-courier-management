import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../../hooks/useAuth';
import { Logo } from '../../components/ui/Logo';
import { 
  Eye, 
  EyeOff, 
  Lock, 
  Mail, 
  ArrowRight, 
  AlertCircle, 
  Loader2, 
  ShieldCheck, 
  CheckCircle2,
  Sparkles,
  KeyRound
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
    setValue,
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

  // Helper for university evaluators to quickly test each role
  const fillCredentials = (role: 'admin' | 'customer' | 'merchant_approved' | 'merchant_pending' | 'courier') => {
    switch (role) {
      case 'admin':
        setValue('email', 'admin@swiftdrop.io');
        setValue('password', 'Admin@SwiftDrop2026!');
        break;
      case 'customer':
        setValue('email', 'customer@swiftdrop.io');
        setValue('password', 'Customer@123!');
        break;
      case 'merchant_approved':
        setValue('email', 'merchant.approved@swiftdrop.io');
        setValue('password', 'Merchant@123!');
        break;
      case 'merchant_pending':
        setValue('email', 'merchant.pending@swiftdrop.io');
        setValue('password', 'Merchant@123!');
        break;
      case 'courier':
        setValue('email', 'courier@swiftdrop.io');
        setValue('password', 'Courier@123!');
        break;
    }
  };

  return (
    <div className="min-h-screen flex flex-col lg:flex-row bg-white">
      {/* ==================================================================== */}
      {/* Left Panel: Logistics-Themed Visual Branding (Split Screen)          */}
      {/* ==================================================================== */}
      <div className="hidden lg:flex lg:w-1/2 relative bg-gradient-to-br from-navy-950 via-navy-900 to-navy-800 text-white flex-col justify-between p-12 overflow-hidden">
        {/* Subtle SVG Map Grid Background */}
        <div className="absolute inset-0 opacity-10 pointer-events-none bg-[radial-gradient(#FFCB05_1.5px,transparent_1.5px)] [background-size:32px_32px]"></div>

        {/* Ambient Glows */}
        <div className="absolute top-1/4 -left-20 w-80 h-80 bg-yellow-500/20 rounded-full blur-3xl pointer-events-none"></div>
        <div className="absolute bottom-1/4 -right-20 w-80 h-80 bg-navy-700/60 rounded-full blur-3xl pointer-events-none"></div>

        {/* Top Branding */}
        <div className="relative z-10">
          <Link to="/">
            <Logo variant="light" size="lg" />
          </Link>
        </div>

        {/* Middle Logistics Graphics & Message */}
        <div className="relative z-10 max-w-lg space-y-8 my-auto">
          <div className="space-y-4">
            <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-yellow-500/10 border border-yellow-500/20 text-yellow-400 text-xs font-semibold">
              <Sparkles className="w-3.5 h-3.5" />
              Enterprise Courier & Freight Cloud
            </span>
            <h2 className="text-4xl font-extrabold tracking-tight leading-tight">
              Next-generation freight dispatch and parcel tracking.
            </h2>
            <p className="text-slate-300 text-sm leading-relaxed">
              Log in to manage live delivery manifests, calculate multi-tier shipping rates, approve merchants, and coordinate last-mile couriers.
            </p>
          </div>

          {/* Logistics Route Node Illustration */}
          <div className="p-5 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-sm space-y-3">
            <div className="flex items-center justify-between text-xs text-slate-300 border-b border-white/10 pb-2.5">
              <span className="font-semibold text-white">Live Route Telemetry</span>
              <span className="text-yellow-400 font-mono text-[11px]">ACTIVE â€¢ 99.98% SLA</span>
            </div>

            <div className="flex items-center justify-between text-xs pt-1">
              <div className="flex items-center gap-2">
                <div className="w-2.5 h-2.5 rounded-full bg-yellow-400 animate-ping"></div>
                <span className="text-slate-200 font-medium">Regional Sorting Hub</span>
              </div>
              <span className="text-slate-400 font-mono">Gateway :8080</span>
            </div>

            <div className="flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <div className="w-2.5 h-2.5 rounded-full bg-blue-400"></div>
                <span className="text-slate-200 font-medium">PostgreSQL Persistence</span>
              </div>
              <span className="text-slate-400 font-mono">Port :5432</span>
            </div>
          </div>
        </div>

        {/* Bottom Footer Notice */}
        <div className="relative z-10 text-xs text-slate-400 flex items-center justify-between">
          <span>SwiftDrop Enterprise Edition v1.0</span>
          <span className="flex items-center gap-1.5 text-yellow-400 font-medium">
            <ShieldCheck className="w-4 h-4" /> End-to-End Encrypted Sessions
          </span>
        </div>
      </div>

      {/* ==================================================================== */}
      {/* Right Panel: Authentication Form                                      */}
      {/* ==================================================================== */}
      <div className="flex-1 flex flex-col justify-center px-6 py-12 sm:px-12 lg:px-16 xl:px-20 max-w-2xl mx-auto w-full">
        {/* Mobile Header Logo */}
        <div className="lg:hidden mb-8">
          <Link to="/">
            <Logo size="md" />
          </Link>
        </div>

        <div className="space-y-2 mb-8">
          <h1 className="text-3xl font-bold tracking-tight text-navy-900">
            Welcome back
          </h1>
          <p className="text-sm text-slate-500">
            Enter your credentials to access your SwiftDrop dashboard.
          </p>
        </div>

        {/* Server Error Alert */}
        {serverError && (
          <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-800 text-sm flex items-start gap-3 animate-fade-in">
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
            <div className="flex-1">
              <p className="font-semibold">Authentication Error</p>
              <p className="text-xs text-red-700 mt-0.5">{serverError}</p>
            </div>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          {/* Email field */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5 uppercase tracking-wide">
              Email Address
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <Mail className="w-4 h-4" />
              </div>
              <input
                type="email"
                placeholder="name@company.com"
                {...register('email')}
                className={`w-full pl-10 pr-4 py-3 rounded-xl border text-sm text-slate-900 placeholder-slate-400 outline-none transition-all ${
                  errors.email
                    ? 'border-red-300 focus:border-red-500 focus:ring-2 focus:ring-red-100'
                    : 'border-slate-200 focus:border-yellow-500 focus:ring-2 focus:ring-yellow-500/20'
                }`}
              />
            </div>
            {errors.email && (
              <p className="text-xs text-red-600 mt-1 font-medium">{errors.email.message}</p>
            )}
          </div>

          {/* Password field */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wide">
                Password
              </label>
              <Link
                to="/forgot-password"
                className="text-xs font-semibold text-navy-800 hover:text-navy-900 transition-colors"
              >
                Forgot password?
              </Link>
            </div>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <Lock className="w-4 h-4" />
              </div>
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="â€¢â€¢â€¢â€¢â€¢â€¢â€¢â€¢"
                {...register('password')}
                className={`w-full pl-10 pr-11 py-3 rounded-xl border text-sm text-slate-900 placeholder-slate-400 outline-none transition-all ${
                  errors.password
                    ? 'border-red-300 focus:border-red-500 focus:ring-2 focus:ring-red-100'
                    : 'border-slate-200 focus:border-yellow-500 focus:ring-2 focus:ring-yellow-500/20'
                }`}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600"
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            {errors.password && (
              <p className="text-xs text-red-600 mt-1 font-medium">{errors.password.message}</p>
            )}
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
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-3.5 px-4 rounded-xl bg-yellow-500 hover:bg-yellow-400 active:bg-yellow-600 text-navy-950 font-extrabold text-sm shadow-lg shadow-yellow-500/25 transition-all flex items-center justify-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Authenticating...</span>
              </>
            ) : (
              <>
                <span>Sign In to Account</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
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
        <button
          type="button"
          onClick={handleGoogleLogin}
          className="w-full py-3 px-4 rounded-xl border border-slate-200 hover:border-slate-300 hover:bg-slate-50 text-slate-700 font-semibold text-sm transition-all flex items-center justify-center gap-3 shadow-sm"
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
        </button>

        {/* Quick Demo Fill Buttons for Evaluators */}
        <div className="mt-8 p-4 rounded-xl bg-slate-50 border border-slate-200">
          <div className="flex items-center gap-1.5 text-xs font-bold text-navy-900 mb-2">
            <KeyRound className="w-3.5 h-3.5 text-yellow-500" />
            <span>Assignment Evaluator Quick Credentials:</span>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-1.5">
            <button
              type="button"
              onClick={() => fillCredentials('admin')}
              className="px-2 py-1.5 text-[11px] font-semibold bg-white border border-slate-200 hover:border-purple-300 hover:bg-purple-50 text-purple-700 rounded-lg transition-colors truncate"
            >
              Admin Role
            </button>
            <button
              type="button"
              onClick={() => fillCredentials('customer')}
              className="px-2 py-1.5 text-[11px] font-semibold bg-white border border-slate-200 hover:border-yellow-500/30 hover:bg-yellow-500/10 text-yellow-700 rounded-lg transition-colors truncate"
            >
              Customer Role
            </button>
            <button
              type="button"
              onClick={() => fillCredentials('merchant_approved')}
              className="px-2 py-1.5 text-[11px] font-semibold bg-white border border-slate-200 hover:border-blue-300 hover:bg-blue-50 text-blue-700 rounded-lg transition-colors truncate"
            >
              Merchant (Approved)
            </button>
            <button
              type="button"
              onClick={() => fillCredentials('courier')}
              className="px-2 py-1.5 text-[11px] font-semibold bg-white border border-slate-200 hover:border-amber-300 hover:bg-amber-50 text-amber-700 rounded-lg transition-colors truncate"
            >
              Courier Role
            </button>
          </div>
        </div>

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
  );
};

