import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../../hooks/useAuth';
import { Logo } from '../../components/ui/Logo';
import { Notice } from '../../components/ui/Notice';
import { 
  Building2, 
  User as UserIcon, 
  Mail, 
  MapPin, 
  Phone, 
  Lock, 
  Eye, 
  EyeOff, 
  ArrowRight, 
  Loader2, 
  Clock, 
} from 'lucide-react';

const merchantSchema = z.object({
  businessName: z.string().min(2, 'Business name must be at least 2 characters'),
  contactPerson: z.string().min(2, 'Contact person name is required'),
  email: z.string().email('Please enter a valid business email address'),
  businessAddress: z.string().min(5, 'Business address is required'),
  phoneNumber: z.string().regex(/^(?:0|(?:\+|00)94)7\d{8}$/, 'Enter a valid Sri Lankan mobile number, e.g. +94771234567'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords do not match",
  path: ["confirmPassword"],
});

type MerchantFormData = z.infer<typeof merchantSchema>;

export const MerchantRegisterPage: React.FC = () => {
  const { registerMerchant } = useAuth();
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<MerchantFormData>({
    resolver: zodResolver(merchantSchema),
  });

  const onSubmit = async (data: MerchantFormData) => {
    setServerError(null);
    try {
      await registerMerchant(data);
      // Redirect to merchant dashboard where PENDING state is clearly displayed
      navigate('/merchant/dashboard', { replace: true });
    } catch (err: any) {
      if (err.response?.data?.message) {
        setServerError(err.response.data.message);
      } else {
        setServerError('Merchant registration failed. Please check your inputs or try again.');
      }
    }
  };

  return (
    <div className="min-h-[100dvh] bg-gradient-to-bl from-[#001a35] via-[#00274c] to-[#8c6504] p-5 sm:p-8 lg:h-[100dvh] lg:overflow-hidden lg:p-6">
      <div className="mx-auto flex h-auto max-w-5xl overflow-hidden rounded-lg bg-white shadow-2xl shadow-navy-950/40 lg:h-[calc(100dvh-3rem)] lg:min-h-0 lg:flex-row">
        <aside className="relative hidden overflow-hidden bg-gradient-to-bl from-[#001a35] via-[#00274c] to-[#8c6504] px-10 py-12 text-white lg:flex lg:w-[42%] lg:flex-col lg:justify-between xl:px-16">
          <img src="/auth-abstract.svg" alt="" aria-hidden="true" className="pointer-events-none absolute inset-0 h-full w-full -scale-y-100 object-cover opacity-90" />
          <div className="relative">
            <Link to="/">
              <Logo variant="light" size="lg" />
            </Link>
          </div>
          <div className="relative max-w-sm">
            <p className="text-sm font-semibold uppercase tracking-[0.18em] text-blue-100">Join SwiftDrop</p>
            <h2 className="mt-4 text-4xl font-black leading-tight tracking-tight xl:text-5xl">Shipping made clear.</h2>
            <p className="mt-5 text-sm leading-6 text-blue-100">Give your business a dependable workspace for every shipment and handoff.</p>
          </div>
          <p className="relative text-xs text-blue-100/70">Simple. Reliable. On time.</p>
        </aside>

        <main className="auth-form-panel flex min-h-full w-full items-start justify-center overflow-x-hidden overflow-y-auto px-4 py-8 sm:px-8 lg:w-[58%] lg:px-10 lg:py-8 xl:px-14">
        <div className="auth-form-content w-full max-w-2xl bg-white p-2 sm:p-5">
        {/* Header */}
        <div className="text-center space-y-3 mb-8">
          <div className="flex justify-center">
            <Link to="/">
              <Logo size="md" />
            </Link>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-navy-900 tracking-tight">
            Create Merchant Account
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 max-w-md mx-auto">
            Apply for business shipping tools, bulk dispatch, and delivery management.
          </p>
        </div>

        {/* Workflow Notice Banner */}
        <div className="mb-6 rounded-lg border border-amber-200 bg-amber-50 p-4 text-xs text-amber-900 flex items-start gap-3">
          <Clock className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
          <div className="min-w-0 space-y-1">
            <p className="font-bold">Important Approval Workflow Notice</p>
            <p className="text-amber-800 leading-relaxed">
              In accordance with enterprise compliance policies, all new merchant registrations are initialized in a <span className="font-semibold underline">PENDING</span> status until reviewed and approved by a SwiftDrop Administrator.
            </p>
          </div>
        </div>

        {/* Server Error Alert */}
        {serverError && (
          <Notice title="Application Submission Error" className="mb-6 animate-fade-in">
            {serverError}
          </Notice>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* Business Name & Contact Person */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wide">
                Business Name
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Building2 className="w-4 h-4" />
                </div>
                <input
                  type="text"
                  placeholder="Apex Retail Corp"
                  {...register('businessName')}
                  className={`w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-slate-900 placeholder-slate-400 outline-none transition-all ${
                    errors.businessName
                      ? 'border-red-300 focus:border-red-500'
                      : 'border-slate-200 focus:border-yellow-500 focus:ring-2 focus:ring-yellow-500/20'
                  }`}
                />
              </div>
              {errors.businessName && (
                <p className="mt-1 break-words text-xs text-red-600">{errors.businessName.message}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wide">
                Contact Person Name
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <UserIcon className="w-4 h-4" />
                </div>
                <input
                  type="text"
                  placeholder="Robert Smith"
                  {...register('contactPerson')}
                  className={`w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-slate-900 placeholder-slate-400 outline-none transition-all ${
                    errors.contactPerson
                      ? 'border-red-300 focus:border-red-500'
                      : 'border-slate-200 focus:border-yellow-500 focus:ring-2 focus:ring-yellow-500/20'
                  }`}
                />
              </div>
              {errors.contactPerson && (
                <p className="mt-1 break-words text-xs text-red-600">{errors.contactPerson.message}</p>
              )}
            </div>
          </div>

          {/* Business Email & Phone */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wide">
                Business Email
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Mail className="w-4 h-4" />
                </div>
                <input
                  type="email"
                  placeholder="orders@apexretail.com"
                  {...register('email')}
                  className={`w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-slate-900 placeholder-slate-400 outline-none transition-all ${
                    errors.email
                      ? 'border-red-300 focus:border-red-500'
                      : 'border-slate-200 focus:border-yellow-500 focus:ring-2 focus:ring-yellow-500/20'
                  }`}
                />
              </div>
              {errors.email && (
                <p className="mt-1 break-words text-xs text-red-600">{errors.email.message}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wide">
                Phone Number
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Phone className="w-4 h-4" />
                </div>
                <input
                  type="tel"
                  placeholder="+94771234567"
                  {...register('phoneNumber')}
                  className={`w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-slate-900 placeholder-slate-400 outline-none transition-all ${
                    errors.phoneNumber
                      ? 'border-red-300 focus:border-red-500'
                      : 'border-slate-200 focus:border-yellow-500 focus:ring-2 focus:ring-yellow-500/20'
                  }`}
                />
              </div>
              {errors.phoneNumber && (
                <p className="mt-1 break-words text-xs text-red-600">{errors.phoneNumber.message}</p>
              )}
            </div>
          </div>

          {/* Business Address */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wide">
              Registered Business Address / Warehouse
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <MapPin className="w-4 h-4" />
              </div>
              <input
                type="text"
                placeholder="456 Commercial Parkway, Suite 200, San Francisco, CA"
                {...register('businessAddress')}
                className={`w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-slate-900 placeholder-slate-400 outline-none transition-all ${
                  errors.businessAddress
                    ? 'border-red-300 focus:border-red-500'
                    : 'border-slate-200 focus:border-yellow-500 focus:ring-2 focus:ring-yellow-500/20'
                }`}
              />
            </div>
            {errors.businessAddress && (
              <p className="mt-1 break-words text-xs text-red-600">{errors.businessAddress.message}</p>
            )}
          </div>

          {/* Password & Confirm Password */}
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
                  placeholder="••••••••"
                  {...register('password')}
                  className={`w-full pl-10 pr-10 py-2.5 rounded-xl border text-sm text-slate-900 placeholder-slate-400 outline-none transition-all ${
                    errors.password
                      ? 'border-red-300 focus:border-red-500'
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
                  placeholder="••••••••"
                  {...register('confirmPassword')}
                  className={`w-full pl-10 pr-10 py-2.5 rounded-xl border text-sm text-slate-900 placeholder-slate-400 outline-none transition-all ${
                    errors.confirmPassword
                      ? 'border-red-300 focus:border-red-500'
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

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full mt-4 py-3.5 px-4 rounded-xl bg-yellow-500 hover:bg-yellow-400 active:bg-yellow-600 text-navy-950 font-extrabold text-sm shadow-lg shadow-navy-900/15 transition-all flex items-center justify-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Submitting Merchant Application...</span>
              </>
            ) : (
              <>
                <span>Submit Merchant Registration</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        <div className="mt-8 pt-6 border-t border-slate-100 text-center text-xs text-slate-500 space-y-2">
          <p>
            Already an approved merchant partner?{' '}
            <Link to="/login" className="font-semibold text-navy-900 hover:underline">
              Sign In to Merchant Console
            </Link>
          </p>
          <p>
            Personal retail shipments only?{' '}
            <Link to="/register" className="font-semibold text-yellow-600 hover:underline">
              Register as Individual Customer
            </Link>
          </p>
        </div>
      </div>
      </main>
      </div>
    </div>
  );
};
