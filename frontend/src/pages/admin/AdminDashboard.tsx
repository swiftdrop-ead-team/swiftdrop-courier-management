import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { adminService } from '../../services/authService';
import { User, PendingMerchant } from '../../types/auth';
import { SoftBadge, SoftCard, SoftStatCard } from '../../components/ui/SoftUI';
import {
  Users,
  CheckCircle,
  ShieldCheck,
  Clock,
  RefreshCw,
  AlertCircle,
  FileCheck2,
  Search,
  Check,
  X,
  Activity,
} from 'lucide-react';

export const AdminDashboard: React.FC<{ previewMode?: boolean }> = ({ previewMode = false }) => {
  const { user } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [pendingMerchants, setPendingMerchants] = useState<PendingMerchant[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionInProgress, setActionInProgress] = useState<string | null>(null);
  const [feedbackMessage, setFeedbackMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);
  const [userSearchTerm, setUserSearchTerm] = useState('');

  const loadData = useCallback(async () => {
    if (previewMode) {
      setUsers([{ id: 'preview-admin', fullName: 'Preview Admin', email: 'admin@example.com', roles: ['ADMIN'], status: 'ACTIVE' }]);
      setLoading(false);
      return;
    }
    setLoading(true);
    setFeedbackMessage(null);
    try {
      const [usersList, merchantsList] = await Promise.all([
        adminService.getAllUsers(),
        adminService.getPendingMerchants(),
      ]);
      setUsers(usersList);
      setPendingMerchants(merchantsList);
    } catch (err: any) {
      setFeedbackMessage({ text: 'Failed to fetch admin data from Identity Service.', type: 'error' });
    } finally {
      setLoading(false);
    }
  }, [previewMode]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleApprove = async (recordId: string) => {
    setActionInProgress(recordId);
    try {
      await adminService.approveMerchant(recordId, 'Approved via Admin Dashboard');
      setFeedbackMessage({ text: 'Merchant registration approved successfully!', type: 'success' });
      await loadData();
    } catch (err: any) {
      setFeedbackMessage({ text: err.response?.data?.message || 'Failed to approve merchant.', type: 'error' });
    } finally { setActionInProgress(null); }
  };

  const handleReject = async (recordId: string) => {
    setActionInProgress(recordId);
    try {
      await adminService.rejectMerchant(recordId, 'Rejected during compliance review');
      setFeedbackMessage({ text: 'Merchant registration rejected.', type: 'success' });
      await loadData();
    } catch (err: any) {
      setFeedbackMessage({ text: err.response?.data?.message || 'Failed to reject merchant.', type: 'error' });
    } finally { setActionInProgress(null); }
  };

  const filteredUsers = users.filter((u) =>
    u.fullName.toLowerCase().includes(userSearchTerm.toLowerCase()) ||
    u.email.toLowerCase().includes(userSearchTerm.toLowerCase()) ||
    u.roles.some((r) => r.toLowerCase().includes(userSearchTerm.toLowerCase()))
  );

  const roleBadgeClass = (role: string) => ({
    ADMIN:    'bg-purple-100 text-purple-800 border border-purple-200',
    MERCHANT: 'bg-blue-100   text-blue-800   border border-blue-200',
    COURIER:  'bg-amber-100  text-amber-800  border border-amber-200',
  }[role] ?? 'bg-yellow-100 text-yellow-900 border border-yellow-300');

  const merchantStatusClass = (status: string) => ({
    APPROVED: 'bg-green-100  text-green-800  border border-green-200',
    PENDING:  'bg-amber-100  text-amber-800  border border-amber-200',
    REJECTED: 'bg-red-100    text-red-800    border border-red-200',
  }[status] ?? 'bg-slate-100 text-slate-500');

  return (
    <div className="space-y-8 animate-fade-in">

      {/* ══ Hero Banner — yellow ══ */}
      <div className="soft-ui-hero">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            {/* Shield icon in navy to contrast the yellow bg */}
            <div className="w-8 h-8 rounded-lg bg-navy-900/10 flex items-center justify-center">
              <ShieldCheck className="w-5 h-5 text-navy-900" />
            </div>
            <h2 className="text-2xl font-extrabold tracking-tight text-navy-900">
              Platform Administration
            </h2>
          </div>
          <p className="text-sm text-navy-800/70 ml-10">
            Welcome,{' '}
            <span className="font-bold text-navy-900">{user?.fullName}</span>. Full control
            over accounts and merchant verifications.
          </p>
        </div>

        <button
          onClick={loadData}
          disabled={loading}
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-navy-900 hover:bg-navy-800 text-blue-200 text-xs font-bold transition-all disabled:opacity-50 shadow-md"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh Data</span>
        </button>
      </div>

      {/* ══ Feedback Alert ══ */}
      {feedbackMessage && (
        <div className={`flex items-center gap-3 p-4 rounded-xl text-sm font-medium animate-fade-in ${
          feedbackMessage.type === 'success' ? 'alert-success' : 'alert-danger'
        }`}>
          {feedbackMessage.type === 'success'
            ? <CheckCircle className="w-5 h-5 shrink-0" />
            : <AlertCircle className="w-5 h-5 shrink-0" />}
          <span>{feedbackMessage.text}</span>
        </div>
      )}

      {/* ══ Summary Stats
            primary → yellow icon (most used)
            warning → amber  (pending needs attention)
            success → green  (healthy count)
            info    → blue   (only for the service health — informational)
      ══ */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-5">
        <SoftStatCard label="Total Registered Users"    value={users.length}                                              helper="Database user accounts"      icon={Users}       color="primary" />
        <SoftStatCard label="Pending Approvals"         value={pendingMerchants.length}                                   helper="Action required"              icon={Clock}       color="warning" />
        <SoftStatCard label="Field Couriers"            value={users.filter(u => u.roles.includes('COURIER')).length}    helper="Active personnel"             icon={ShieldCheck} color="success" />
        <SoftStatCard label="Identity Service"          value="HEALTHY"                                                   helper="Spring Boot identity service" icon={Activity}    color="info"    />
      </div>

      {/* ══════════════════════════════════════════════════
          1 · Pending Merchant Approvals
      ══════════════════════════════════════════════════ */}
      <SoftCard id="merchants" className="p-6 sm:p-8 space-y-5">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center pb-4 border-b border-blue-100 gap-2">
          <div>
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-amber-400 ring-2 ring-amber-100 animate-pulse" />
              <h3 className="text-lg font-bold text-navy-900">
                Pending Merchant Approvals ({pendingMerchants.length})
              </h3>
            </div>
            <p className="text-xs text-slate-500 mt-0.5 ml-5">
              Review and approve newly registered commercial merchants.
            </p>
          </div>
        </div>

        {pendingMerchants.length === 0 ? (
          <div className="py-12 text-center space-y-2">
            <div className="w-14 h-14 rounded-2xl bg-green-50 text-green-600 flex items-center justify-center mx-auto">
              <FileCheck2 className="w-7 h-7" />
            </div>
            <p className="font-semibold text-slate-700 text-sm">All merchant applications reviewed</p>
            <p className="text-xs text-slate-400">No pending registrations at this time.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 uppercase tracking-wider font-semibold border-y border-slate-200">
                <tr>
                  <th className="py-3 px-4">Business & Contact</th>
                  <th className="py-3 px-4">Email & Phone</th>
                  <th className="py-3 px-4">Registered Address</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Administrative Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-blue-50">
                {pendingMerchants.map((merchant) => (
                  <tr key={merchant.id} className="hover:bg-blue-50/50 transition-colors">
                    <td className="py-3.5 px-4">
                      <div className="font-bold text-navy-900">{merchant.businessName}</div>
                      <div className="text-slate-500">{merchant.contactPerson}</div>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-600">
                      <div>{merchant.email}</div>
                      <div className="text-[11px] text-slate-400">{merchant.phoneNumber}</div>
                    </td>
                    <td className="py-3.5 px-4 text-slate-600 max-w-xs truncate">
                      {merchant.businessAddress}
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-200">
                        <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
                        PENDING
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-right space-x-2 whitespace-nowrap">
                      {/* Approve → green */}
                      <button
                        onClick={() => handleApprove(merchant.id)}
                        disabled={actionInProgress === merchant.id}
                        className="px-3 py-1.5 rounded-lg bg-green-600 hover:bg-green-700 text-white font-semibold text-xs inline-flex items-center gap-1 shadow-sm disabled:opacity-50 transition-colors"
                      >
                        <Check className="w-3.5 h-3.5" /><span>Approve</span>
                      </button>
                      {/* Reject → red */}
                      <button
                        onClick={() => handleReject(merchant.id)}
                        disabled={actionInProgress === merchant.id}
                        className="px-3 py-1.5 rounded-lg bg-red-50 hover:bg-red-100 border border-red-200 text-red-700 font-semibold text-xs inline-flex items-center gap-1 disabled:opacity-50 transition-colors"
                      >
                        <X className="w-3.5 h-3.5" /><span>Reject</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </SoftCard>

      {/* ══════════════════════════════════════════════════
          2 · Registered Users
      ══════════════════════════════════════════════════ */}
      <SoftCard id="users" className="p-6 sm:p-8 space-y-5">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center pb-4 border-b border-blue-100 gap-4">
          <div>
            <div className="flex items-center gap-2">
              <div className="w-7 h-7 rounded-lg bg-blue-100 flex items-center justify-center">
                <Users className="w-4 h-4 text-navy-900" />
              </div>
              <h3 className="text-lg font-bold text-navy-900">
                Registered Platform Users ({users.length})
              </h3>
            </div>
            <p className="text-xs text-slate-500 mt-0.5 ml-9">
              Live records persisted in PostgreSQL table{' '}
              <code className="font-mono text-blue-700 bg-blue-50 px-1 py-0.5 rounded">users</code>
            </p>
          </div>

          <div className="relative w-full sm:w-64">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3 pointer-events-none" />
            <input
              type="text"
              placeholder="Search by name, email, role…"
              value={userSearchTerm}
              onChange={(e) => setUserSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3.5 py-2 text-xs rounded-xl border border-blue-200 bg-white focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 outline-none transition-all"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-600 uppercase tracking-wider font-semibold border-y border-slate-200">
              <tr>
                <th className="py-3 px-4">User Details</th>
                <th className="py-3 px-4">Email</th>
                <th className="py-3 px-4">Phone</th>
                <th className="py-3 px-4">Assigned Roles</th>
                <th className="py-3 px-4">Merchant Status</th>
                <th className="py-3 px-4">Account Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-blue-50">
              {filteredUsers.map((u) => (
                <tr key={u.id} className="hover:bg-blue-50/40 transition-colors">
                  <td className="py-3.5 px-4 font-semibold text-navy-900">{u.fullName}</td>
                  <td className="py-3.5 px-4 font-mono text-slate-600">{u.email}</td>
                  <td className="py-3.5 px-4 text-slate-500">{u.phoneNumber || '—'}</td>
                  <td className="py-3.5 px-4">
                    <div className="flex flex-wrap gap-1">
                      {u.roles.map((r) => (
                        <span key={r} className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${roleBadgeClass(r)}`}>
                          {r}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="py-3.5 px-4">
                    {u.merchantApprovalStatus ? (
                      <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold ${merchantStatusClass(u.merchantApprovalStatus)}`}>
                        {u.merchantApprovalStatus}
                      </span>
                    ) : (
                      <span className="text-slate-400">N/A</span>
                    )}
                  </td>
                  <td className="py-3.5 px-4">
                    <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold text-green-700">
                      <span className="w-1.5 h-1.5 rounded-full bg-green-500" />
                      ACTIVE
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </SoftCard>
    </div>
  );
};
