import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { adminService } from '../../services/authService';
import { User, PendingMerchant } from '../../types/auth';
import { SoftBadge, SoftCard, SoftStatCard } from '../../components/ui/SoftUI';
import { 
  Users, 
  CheckCircle, 
  XCircle, 
  ShieldCheck, 
  Clock, 
  RefreshCw, 
  Building2, 
  AlertCircle,
  FileCheck2,
  Search,
  Check,
  X
} from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  const { user } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [pendingMerchants, setPendingMerchants] = useState<PendingMerchant[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionInProgress, setActionInProgress] = useState<string | null>(null);
  const [feedbackMessage, setFeedbackMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);
  const [userSearchTerm, setUserSearchTerm] = useState('');

  const loadData = useCallback(async () => {
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
      setFeedbackMessage({
        text: 'Failed to fetch admin data from Identity Service.',
        type: 'error',
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleApprove = async (recordId: string) => {
    setActionInProgress(recordId);
    try {
      await adminService.approveMerchant(recordId, 'Approved via Admin Dashboard');
      setFeedbackMessage({
        text: 'Merchant registration approved successfully! Status updated to APPROVED.',
        type: 'success',
      });
      // Refresh list
      await loadData();
    } catch (err: any) {
      setFeedbackMessage({
        text: err.response?.data?.message || 'Failed to approve merchant.',
        type: 'error',
      });
    } finally {
      setActionInProgress(null);
    }
  };

  const handleReject = async (recordId: string) => {
    setActionInProgress(recordId);
    try {
      await adminService.rejectMerchant(recordId, 'Rejected during compliance review');
      setFeedbackMessage({
        text: 'Merchant registration rejected.',
        type: 'success',
      });
      // Refresh list
      await loadData();
    } catch (err: any) {
      setFeedbackMessage({
        text: err.response?.data?.message || 'Failed to reject merchant.',
        type: 'error',
      });
    } finally {
      setActionInProgress(null);
    }
  };

  const filteredUsers = users.filter((u) => 
    u.fullName.toLowerCase().includes(userSearchTerm.toLowerCase()) ||
    u.email.toLowerCase().includes(userSearchTerm.toLowerCase()) ||
    u.roles.some((r) => r.toLowerCase().includes(userSearchTerm.toLowerCase()))
  );

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Admin Header */}
      <div className="soft-ui-hero">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-6 h-6 text-yellow-400" />
            <h2 className="text-2xl font-extrabold tracking-tight">
              Platform Administration
            </h2>
          </div>
          <p className="text-xs text-slate-300">
            Welcome, Administrator <span className="font-semibold text-white">{user?.fullName}</span>. Complete control over registered accounts and merchant verifications.
          </p>
        </div>

        <button
          onClick={loadData}
          disabled={loading}
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-navy-800 hover:bg-navy-700 border border-slate-700 text-xs font-semibold transition-all disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh Data</span>
        </button>
      </div>

      {/* Feedback Alert */}
      {feedbackMessage && (
        <div className={`p-4 rounded-xl text-xs flex items-center gap-2 animate-fade-in ${
          feedbackMessage.type === 'success' 
            ? 'bg-yellow-500/10 border border-yellow-500/30 text-navy-900' 
            : 'bg-red-50 border border-red-200 text-red-900'
        }`}>
          {feedbackMessage.type === 'success' ? (
            <CheckCircle className="w-4 h-4 text-yellow-500 shrink-0" />
          ) : (
            <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
          )}
          <span className="font-medium">{feedbackMessage.text}</span>
        </div>
      )}

      {/* Admin Summary Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-5">
        <SoftStatCard label="Total Registered Users" value={users.length} helper="Database user accounts" icon={Users} color="info" />
        <SoftStatCard label="Pending Merchant Approvals" value={pendingMerchants.length} helper="Action required" icon={Clock} color="warning" />
        <SoftStatCard label="Field Couriers" value={users.filter(u => u.roles.includes('COURIER')).length} helper="Active personnel" icon={ShieldCheck} color="success" />
        <SoftStatCard label="Identity Service Status" value="HEALTHY" helper="Spring Boot identity service" icon={CheckCircle} color="success" />
      </div>

      {/* ==================================================================== */}
      {/* 1. Pending Merchant Approvals Table (Live Action)                    */}
      {/* ==================================================================== */}
      <SoftCard id="merchants" className="p-6 sm:p-8 space-y-5">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center pb-4 border-b border-slate-100 gap-2">
          <div>
            <div className="flex items-center gap-2">
              <Clock className="w-5 h-5 text-amber-500" />
              <h3 className="text-lg font-bold text-navy-900">
                Pending Merchant Approvals ({pendingMerchants.length})
              </h3>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Review and approve newly registered commercial merchants to grant shipping access.
            </p>
          </div>
        </div>

        {pendingMerchants.length === 0 ? (
          <div className="py-12 text-center text-xs text-slate-500 space-y-2">
            <FileCheck2 className="w-10 h-10 text-yellow-500 mx-auto" />
            <p className="font-semibold text-slate-700">All merchant applications reviewed</p>
            <p className="text-[11px] text-slate-400">No pending commercial registrations at this time.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 uppercase tracking-wider font-semibold border-y border-slate-200">
                <tr>
                  <th className="py-3 px-4">Business & Contact</th>
                  <th className="py-3 px-4">Email & Phone</th>
                  <th className="py-3 px-4">Registered Address</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Administrative Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {pendingMerchants.map((merchant) => (
                  <tr key={merchant.id} className="hover:bg-slate-50/80 transition-colors">
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
                      <SoftBadge color="warning">PENDING</SoftBadge>
                    </td>
                    <td className="py-3.5 px-4 text-right space-x-2 whitespace-nowrap">
                      <button
                        onClick={() => handleApprove(merchant.id)}
                        disabled={actionInProgress === merchant.id}
                        className="px-3 py-1.5 rounded-lg bg-yellow-500 hover:bg-yellow-600 text-white font-semibold text-xs inline-flex items-center gap-1 shadow-sm disabled:opacity-50"
                      >
                        <Check className="w-3.5 h-3.5" />
                        <span>Approve</span>
                      </button>
                      <button
                        onClick={() => handleReject(merchant.id)}
                        disabled={actionInProgress === merchant.id}
                        className="px-3 py-1.5 rounded-lg bg-red-50 hover:bg-red-100 border border-red-200 text-red-700 font-semibold text-xs inline-flex items-center gap-1 disabled:opacity-50"
                      >
                        <X className="w-3.5 h-3.5" />
                        <span>Reject</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </SoftCard>

      {/* ==================================================================== */}
      {/* 2. Registered Users Management Table                                 */}
      {/* ==================================================================== */}
      <SoftCard id="users" className="p-6 sm:p-8 space-y-5">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center pb-4 border-b border-slate-100 gap-4">
          <div>
            <div className="flex items-center gap-2">
              <Users className="w-5 h-5 text-navy-900" />
              <h3 className="text-lg font-bold text-navy-900">
                Registered Platform Users ({users.length})
              </h3>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Live records persisted in PostgreSQL table <code className="font-mono text-yellow-600">users</code>
            </p>
          </div>

          <div className="relative w-full sm:w-64">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3 pointer-events-none" />
            <input
              type="text"
              placeholder="Search user by name, email..."
              value={userSearchTerm}
              onChange={(e) => setUserSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3.5 py-2 text-xs rounded-xl border border-slate-200 focus:border-yellow-500 outline-none"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-500 uppercase tracking-wider font-semibold border-y border-slate-200">
              <tr>
                <th className="py-3 px-4">User Details</th>
                <th className="py-3 px-4">Email</th>
                <th className="py-3 px-4">Phone</th>
                <th className="py-3 px-4">Assigned Roles</th>
                <th className="py-3 px-4">Merchant Status</th>
                <th className="py-3 px-4">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredUsers.map((u) => (
                <tr key={u.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="py-3.5 px-4 font-semibold text-navy-900">
                    {u.fullName}
                  </td>
                  <td className="py-3.5 px-4 font-mono text-slate-600">
                    {u.email}
                  </td>
                  <td className="py-3.5 px-4 text-slate-500">
                    {u.phoneNumber || '-'}
                  </td>
                  <td className="py-3.5 px-4">
                    <div className="flex flex-wrap gap-1">
                      {u.roles.map((r) => (
                        <span 
                          key={r}
                          className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${
                            r === 'ADMIN' ? 'bg-purple-100 text-purple-800' :
                            r === 'MERCHANT' ? 'bg-blue-100 text-blue-800' :
                            r === 'COURIER' ? 'bg-amber-100 text-amber-800' :
                            'bg-yellow-400/20 text-yellow-700'
                          }`}
                        >
                          {r}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="py-3.5 px-4">
                    {u.merchantApprovalStatus ? (
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        u.merchantApprovalStatus === 'APPROVED' ? 'bg-yellow-500/20 text-navy-900' :
                        u.merchantApprovalStatus === 'PENDING' ? 'bg-amber-100 text-amber-800' :
                        'bg-red-100 text-red-800'
                      }`}>
                        {u.merchantApprovalStatus}
                      </span>
                    ) : (
                      <span className="text-slate-400">N/A</span>
                    )}
                  </td>
                  <td className="py-3.5 px-4">
                    <span className="inline-flex items-center gap-1 text-yellow-600 font-semibold text-[11px]">
                      <span className="w-1.5 h-1.5 rounded-full bg-yellow-500"></span>
                      <span>ACTIVE</span>
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
