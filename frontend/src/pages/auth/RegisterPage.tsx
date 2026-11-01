import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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
  User as UserIcon, 
  Phone, 
  ArrowRight, 
} from 'lucide-react';

const registerSchema = z.object({
  fullName: z.string().min(2, 'Full name must be at least 2 characters'),
  email: z.string().email('Please enter a valid email address'),
  phoneNumber: z.string().regex(/^\+?[0-9]{7,15}$/, 'Please enter a valid phone number with digits'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
  acceptTerms: z.literal(true, {
    errorMap: () => ({ message: 'You must accept the terms and conditions' }),
  }),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords do not match",
  path: ["confirmPassword"],
});

type RegisterFormData = z.infer<typeof registerSchema>;

export const RegisterPage: React.FC = () => {
  const { registerCustomer } = useAuth();
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: '',
      email: '',
      phoneNumber: '',
      password: '',
      confirmPassword: '',
      acceptTerms: false as any,
    },
  });

  const onSubmit = async (data: RegisterFormData) => {
    setServerError(null);
    try {
      await registerCustomer({
        fullName: data.fullName,
        email: data.email,
        phoneNumber: data.phoneNumber,
        password: data.password,
        confirmPassword: data.confirmPassword,
        acceptTerms: data.acceptTerms,
      });

      // Customer account created, redirect to customer dashboard
      navigate('/customer/dashboard', { replace: true });
    } catch (err: any) {
      if (err.response?.data?.message) {
        setServerError(err.response.data.message);
      } else {
        setServerError('Registration failed. Please verify your details or try again.');
      }
    }
  };

  return (
    <div className="min-h-[100dvh] bg-gradient-to-br from-navy-700 via-navy-900 to-navy-950 p-5 sm:p-8 lg:h-[100dvh] lg:overflow-hidden lg:p-6">
      <div className="mx-auto flex h-auto max-w-5xl overflow-hidden rounded-lg bg-white shadow-2xl shadow-navy-950/40 lg:h-[calc(100dvh-3rem)] lg:min-h-0 lg:flex-row">
      <aside className="relative hidden overflow-hidden bg-gradient-to-br from-blue-500 via-blue-600 to-blue-800 px-10 py-12 text-white lg:flex lg:w-[42%] lg:flex-col lg:justify-between xl:px-16">
        <img src="/auth-abstract.svg" alt="" aria-hidden="true" className="pointer-events-none absolute inset-0 h-full w-full -scale-y-100 object-cover opacity-90" />
        <div className="relative">
          <Link to="/">
            <Logo variant="light" size="lg" />
          </Link>
        </div>
        <div className="relative max-w-sm">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-blue-100">Join SwiftDrop</p>
          <h2 className="mt-4 text-4xl font-black leading-tight tracking-tight xl:text-5xl">Shipping made clear.</h2>
          <p className="mt-5 text-sm leading-6 text-blue-100">Keep every shipment, update, and delivery confirmation in one place.</p>
        </div>
        <p className="relative text-xs text-blue-100/70">Simple. Reliable. On time.</p>
      </aside>

      <div className="auth-form-panel flex min-h-full w-full items-start justify-center overflow-x-hidden overflow-y-auto px-4 py-8 sm:px-8 lg:w-[58%] lg:px-10 lg:py-8 xl:px-14">
      <div className="auth-form-content w-full max-w-2xl bg-white p-2 sm:p-5">
        {/* Header */}
        <div className="mb-8 space-y-3 text-center">
          <div className="flex justify-center">
            <Link to="/">
              <Logo size="md" />
            </Link>
          </div>
          <h1 className="text-3xl font-black tracking-tight text-ink sm:text-4xl">
            Create Customer Account
          </h1>
          <p className="mx-auto max-w-lg text-xs leading-6 text-slate-500 sm:text-sm">
            Sign up for instant parcel dispatch, real-time tracking, and automated receipts.
          </p>
        </div>

        {/* Server Error Alert */}
        {serverError && (
          <Notice title="Registration Issue" className="mb-6 animate-fade-in">
            {serverError}
          </Notice>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* Full Name */}
          <TextField
            label="Full Name"
            type="text"
            placeholder="Jane Doe"
            icon={<UserIcon className="h-4 w-4" />}
            error={errors.fullName?.message}
            {...register('fullName')}
          />

          {/* Email */}
          <TextField
            label="Email Address"
            type="email"
            placeholder="jane@example.com"
            icon={<Mail className="h-4 w-4" />}
            error={errors.email?.message}
            {...register('email')}
          />

          {/* Phone Number */}
          <TextField
            label="Phone Number"
            type="tel"
            placeholder="+14155552671"
            icon={<Phone className="h-4 w-4" />}
            error={errors.phoneNumber?.message}
            {...register('phoneNumber')}
          />

          {/* Password & Confirm Password (Grid) */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wide">
                Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder="â€¢â€¢â€¢â€¢â€¢â€¢â€¢â€¢"
                  {...register('password')}
                  className={`w-full pl-10 pr-10 py-2.5 rounded-xl border text-sm text-slate-900 placeholder-slate-400 outline-none transition-all ${
                    errors.password
                      ? 'border-red-300 focus:border-red-500 focus:ring-2 focus:ring-red-100'
                      : 'border-slate-200 focus:border-yellow-500 focus:ring-2 focus:ring-yellow-500/20'
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && (
                <p className="mt-1 break-words text-xs text-red-600">{errors.password.message}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wide">
                Confirm Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  placeholder="â€¢â€¢â€¢â€¢â€¢â€¢â€¢â€¢"
                  {...register('confirmPassword')}
                  className={`w-full pl-10 pr-10 py-2.5 rounded-xl border text-sm text-slate-900 placeholder-slate-400 outline-none transition-all ${
                    errors.confirmPassword
                      ? 'border-red-300 focus:border-red-500 focus:ring-2 focus:ring-red-100'
                      : 'border-slate-200 focus:border-yellow-500 focus:ring-2 focus:ring-yellow-500/20'
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600"
                >
                  {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.confirmPassword && (
                <p className="mt-1 break-words text-xs text-red-600">{errors.confirmPassword.message}</p>
              )}
            </div>
          </div>

          {/* Terms Acceptance */}
          <div className="pt-2">
            <div className="flex items-start">
              <input
                id="acceptTerms"
                type="checkbox"
                {...register('acceptTerms')}
                className="w-4 h-4 mt-0.5 text-yellow-500 border-slate-300 rounded focus:ring-yellow-400 cursor-pointer"
              />
              <label htmlFor="acceptTerms" className="ml-2.5 text-xs text-slate-600 cursor-pointer">
                I agree to the SwiftDrop Terms of Service and Privacy Policy. My account will be provisioned with standard Customer dispatch privileges.
              </label>
            </div>
            {errors.acceptTerms && (
              <p className="mt-1 break-words text-xs text-red-600">{errors.acceptTerms.message}</p>
            )}
          </div>

          {/* Submit Button */}
          <Button
            type="submit"
            loading={isSubmitting}
            fullWidth
            className="mt-4"
          >
            {isSubmitting ? 'Registering Account...' : <><span>Complete Customer Registration</span><ArrowRight className="h-4 w-4" /></>}
          </Button>
        </form>

        <div className="mt-8 pt-6 border-t border-slate-100 text-center text-xs text-slate-500 space-y-2">
          <p>
            Already have an account?{' '}
            <Link to="/login" className="font-semibold text-navy-900 hover:underline">
              Sign In here
            </Link>
          </p>
          <p>
            Looking for business shipping?{' '}
            <Link to="/merchant/register" className="font-semibold text-yellow-600 hover:underline">
              Register as a Merchant
            </Link>
          </p>
        </div>
      </div>
      </div>
      </div>
    </div>
  );
};
