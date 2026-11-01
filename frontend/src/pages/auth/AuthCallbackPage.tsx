import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { setAccessToken } from '../../services/api';
import { authService } from '../../services/authService';
import { Loader2, AlertCircle } from 'lucide-react';

export const AuthCallbackPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { refreshUser, getDefaultDashboardRoute } = useAuth();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = searchParams.get('token');
    const err = searchParams.get('error');

    if (err) {
      setError(`OAuth authorization error: ${err}`);
      return;
    }

    if (!token) {
      setError('No authorization token received from OAuth provider.');
      return;
    }

    const processLogin = async () => {
      try {
        setAccessToken(token);
        const currentUser = await authService.getCurrentUser();
        await refreshUser();
        const target = getDefaultDashboardRoute(currentUser.roles);
        navigate(target, { replace: true });
      } catch (e: any) {
        setError('Failed to establish authenticated session from OAuth credentials.');
      }
    };

    processLogin();
  }, [searchParams, navigate, refreshUser, getDefaultDashboardRoute]);

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6 bg-[#EDEDED]">
        <div className="max-w-md w-full bg-white p-8 rounded-2xl shadow-xl border border-slate-100 text-center space-y-4">
          <AlertCircle className="w-12 h-12 text-red-500 mx-auto" />
          <h2 className="text-xl font-bold text-navy-900">OAuth Sign-In Failed</h2>
          <p className="text-xs text-slate-600">{error}</p>
          <button
            onClick={() => navigate('/login')}
            className="w-full py-2.5 px-4 bg-navy-900 hover:bg-navy-800 text-white font-semibold text-xs rounded-xl"
          >
            Return to Login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-6 bg-[#EDEDED]">
      <Loader2 className="w-10 h-10 text-yellow-500 animate-spin mb-4" />
      <h2 className="text-lg font-bold text-navy-900">Finalizing Google Authentication...</h2>
      <p className="text-xs text-slate-500 mt-1">Establishing secure session with SwiftDrop Identity Service</p>
    </div>
  );
};

