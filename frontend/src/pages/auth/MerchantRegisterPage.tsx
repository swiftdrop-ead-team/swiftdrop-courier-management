import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../../hooks/useAuth';
import { Logo } from '../../components/ui/Logo';
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
  AlertCircle, 
  Loader2, 
  Clock, 
  ShieldAlert 
} from 'lucide-react';

const merchantSchema = z.object({
  businessName: z.string().min(2, 'Business name must be at least 2 characters'),
  contactPerson: z.string().min(2, 'Contact person name is required'),
  email: z.string().email('Please enter a valid business email address'),
  businessAddress: z.string().min(5, 'Business address is required'),
  phoneNumber: z.string().regex(/^\+?[0-9]{7,15}$/, 'Please enter a valid phone number with digits'),
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
    <div className="min-h-screen py-12 px-4 sm:px-6 lg:px-8 flex items-center justify-center bg-[#F8FAFC]">
      <div className="max-w-2xl w-full bg-white rounded-3xl shadow-xl border border-slate-100 p-8 sm:p-12">
        {/* Header */}
        <div className="text-center space-y-3 mb-8">
          <div className="flex justify-center">
            <Link to="/">
              <Logo size="md" />
            </Link>
          </div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-50 text-blue-700 text-xs font-semibold uppercase tracking-wider">
            <Building2 className="w-3.5 h-3.5" />
            <span>Commercial Merchant Partner Application</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-navy-900 tracking-tight">
            Register Your Business
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 max-w-md mx-auto">
            Access bulk manifest dispatch, automated API keys, and tiered volume discounts.
          </p>
        </div>

        {/* Workflow Notice Banner */}
        <div className="mb-6 p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-start gap-3">
          <Clock className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <p className="font-bold">Important Approval Workflow Notice</p>
            <p className="text-amber-800 leading-relaxed">
              In accordance with enterprise compliance policies, all new merchant registrations are initialized in a <span className="font-semibold underline">PENDING</span> status until reviewed and approved by a SwiftDrop Administrator.
            </p>
          </div>
        </div>

        {/* Server Error Alert */}
        {serverError && (
          <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-800 text-sm flex items-start gap-3 animate-fade-in">
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
            <div className="flex-1">
              <p className="font-semibold">Application Submission Error</p>
              <p className="text-xs text-red-700 mt-0.5">{serverError}</p>
            </div>
          </div>
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
                      : 'border-slate-200 focus:border-teal-600 focus:ring-2 focus:ring-teal-100'
                  }`}
                />
              </div>
              {errors.businessName && (
                <p className="text-xs text-red-600 mt-1">{errors.businessName.message}</p>
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
                      : 'border-slate-200 focus:border-teal-600 focus:ring-2 focus:ring-teal-100'
                  }`}
                />
              </div>
              {errors.contactPerson && (
                <p className="text-xs text-red-600 mt-1">{errors.contactPerson.message}</p>
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
                      : 'border-slate-200 focus:border-teal-600 focus:ring-2 focus:ring-teal-100'
                  }`}
                />
              </div>
              {errors.email && (
                <p className="text-xs text-red-600 mt-1">{errors.email.message}</p>
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
                  placeholder="+18005550199"
                  {...register('phoneNumber')}
                  className={`w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-slate-900 placeholder-slate-400 outline-none transition-all ${
                    errors.phoneNumber
                      ? 'border-red-300 focus:border-red-500'
                      : 'border-slate-200 focus:border-teal-600 focus:ring-2 focus:ring-teal-100'
                  }`}
                />
              </div>
              {errors.phoneNumber && (
                <p className="text-xs text-red-600 mt-1">{errors.phoneNumber.message}</p>
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
                    : 'border-slate-200 focus:border-teal-600 focus:ring-2 focus:ring-teal-100'
                }`}
              />
            </div>
            {errors.businessAddress && (
              <p className="text-xs text-red-600 mt-1">{errors.businessAddress.message}</p>
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
                      : 'border-slate-200 focus:border-teal-600 focus:ring-2 focus:ring-teal-100'
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
                <p className="text-xs text-red-600 mt-1">{errors.password.message}</p>
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
                      : 'border-slate-200 focus:border-teal-600 focus:ring-2 focus:ring-teal-100'
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
                <p className="text-xs text-red-600 mt-1">{errors.confirmPassword.message}</p>
              )}
            </div>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full mt-4 py-3.5 px-4 rounded-xl bg-navy-900 hover:bg-navy-800 active:bg-navy-950 text-white font-semibold text-sm shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
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
            <Link to="/register" className="font-semibold text-teal-700 hover:underline">
              Register as Individual Customer
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};
