import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { authService } from '../../services/authService';
import { Logo } from '../../components/ui/Logo';
import { Lock, Eye, EyeOff, ArrowRight, AlertCircle, CheckCircle2, Loader2 } from 'lucide-react';

const resetSchema = z.object({
  newPassword: z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "Passwords do not match",
  path: ["confirmPassword"],
});

type ResetFormData = z.infer<typeof resetSchema>;

export const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const navigate = useNavigate();

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [success, setSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetFormData>({
    resolver: zodResolver(resetSchema),
  });

  const onSubmit = async (data: ResetFormData) => {
    if (!token) {
      setErrorMessage('Reset token is missing from URL query.');
      return;
    }
    setErrorMessage(null);
    try {
      await authService.resetPassword(token, data.newPassword, data.confirmPassword);
      setSuccess(true);
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Password reset failed. Token may have expired.');
    }
  };

  return (
    <div className="min-h-screen py-16 px-4 flex items-center justify-center bg-[#EDEDED]">
      <div className="max-w-md w-full bg-white rounded-3xl shadow-xl border border-slate-100 p-8 sm:p-10">
        <div className="text-center space-y-2 mb-8">
          <div className="flex justify-center mb-4">
            <Link to="/">
              <Logo size="md" />
            </Link>
          </div>
          <h1 className="text-2xl font-extrabold text-navy-900 tracking-tight">
            Create New Password
          </h1>
          <p className="text-xs text-slate-500">
            Please choose a secure new password for your SwiftDrop account.
          </p>
        </div>

        {success ? (
          <div className="p-5 rounded-2xl bg-yellow-500/10 border border-yellow-500/30 text-navy-900 space-y-4 animate-fade-in text-center">
            <div className="w-12 h-12 rounded-full bg-yellow-400/20 text-yellow-600 flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <h3 className="font-bold text-base">Password Updated Successfully</h3>
            <p className="text-xs text-yellow-700">
              Your password has been changed. You can now sign in with your new credentials.
            </p>
            <div className="pt-2">
              <Link
                to="/login"
                className="w-full inline-flex items-center justify-center py-2.5 px-4 rounded-xl bg-navy-900 hover:bg-navy-800 text-white font-semibold text-xs shadow-sm"
              >
                Proceed to Sign In
              </Link>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {errorMessage && (
              <div className="p-3.5 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
                <span>{errorMessage}</span>
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wide">
                New Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder="••••••••"
                  {...register('newPassword')}
                  className="w-full pl-10 pr-10 py-2.5 rounded-xl border border-slate-200 focus:border-yellow-500 focus:ring-2 focus:ring-yellow-500/20 text-sm outline-none transition-all"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.newPassword && (
                <p className="text-xs text-red-600 mt-1">{errors.newPassword.message}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wide">
                Confirm New Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  placeholder="••••••••"
                  {...register('confirmPassword')}
                  className="w-full pl-10 pr-10 py-2.5 rounded-xl border border-slate-200 focus:border-yellow-500 focus:ring-2 focus:ring-yellow-500/20 text-sm outline-none transition-all"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400"
                >
                  {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.confirmPassword && (
                <p className="text-xs text-red-600 mt-1">{errors.confirmPassword.message}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full mt-2 py-3 px-4 rounded-xl bg-navy-900 hover:bg-navy-800 text-white font-semibold text-sm shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-70"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Updating Password...</span>
                </>
              ) : (
                <>
                  <span>Confirm New Password</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>
        )}
      </div>
    </div>
  );
};
