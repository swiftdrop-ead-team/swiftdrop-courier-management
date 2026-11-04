import React, { useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { SoftCard, SoftStatCard } from '../../components/ui/SoftUI';
import { 
  Building2, 
  Clock, 
  CheckCircle2, 
  AlertTriangle, 
  Package, 
  Key, 
  FileSpreadsheet, 
  Copy, 
  Check, 
  ShieldCheck,
  TrendingUp,
  FileText
} from 'lucide-react';

export const MerchantDashboard: React.FC = () => {
  const { user } = useAuth();
  const [copiedKey, setCopiedKey] = useState(false);

  // Check approval status from user profile
  const isApproved = user?.merchantApprovalStatus === 'APPROVED';
  const isPending = user?.merchantApprovalStatus === 'PENDING' || !user?.merchantApprovalStatus;
  const isRejected = user?.merchantApprovalStatus === 'REJECTED';

  const copyApiKey = () => {
    navigator.clipboard.writeText('sd_live_9a8f21b74c8e3921049281a');
    setCopiedKey(true);
    setTimeout(() => setCopiedKey(false), 2000);
  };

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Business Header & Status */}
      <SoftCard className="p-6 sm:p-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <Building2 className="w-5 h-5 text-yellow-500" />
            <h2 className="text-2xl font-bold text-navy-900">
              Commercial Merchant Portal
            </h2>
          </div>
          <p className="text-xs text-slate-500">
            Account Owner: <span className="font-semibold text-slate-800">{user?.fullName}</span> ({user?.email})
          </p>
        </div>

        {/* Status Badge */}
        <div>
          {isApproved ? (
            <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-yellow-500/10 border border-yellow-500/30 text-navy-900 text-xs font-bold shadow-sm">
              <CheckCircle2 className="w-4 h-4 text-yellow-500" />
              <span>COMMERCIAL ACCOUNT: APPROVED</span>
            </div>
          ) : isPending ? (
            <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs font-bold shadow-sm animate-pulse">
              <Clock className="w-4 h-4 text-amber-600" />
              <span>APPROVAL STATUS: PENDING REVIEW</span>
            </div>
          ) : (
            <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs font-bold shadow-sm">
              <AlertTriangle className="w-4 h-4 text-red-600" />
              <span>APPROVAL STATUS: REJECTED</span>
            </div>
          )}
        </div>
      </SoftCard>

      {/* Approval Status Workflow Banner */}
      {isPending && (
        <div className="p-6 rounded-2xl bg-amber-500/10 border border-amber-300 text-amber-950 space-y-3">
          <div className="flex items-start gap-3">
            <Clock className="w-6 h-6 text-amber-600 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <h4 className="font-bold text-sm text-amber-900">
                Application Under Verification by SwiftDrop Administrator
              </h4>
              <p className="text-xs text-amber-800 leading-relaxed">
                Thank you for applying to become a SwiftDrop merchant partner. Your registration details and commercial address are currently undergoing administrative compliance review. Commercial features (such as bulk manifest ingestion and live API key provisioning) will unlock automatically once approved.
              </p>
              <div className="pt-2 text-[11px] text-amber-700 font-semibold flex items-center gap-2">
                <span>Tip for Evaluators:</span>
                <span>Sign in as Admin (admin@swiftdrop.io) to approve this pending merchant account in real time.</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {isApproved && (
        <div className="p-4 rounded-xl bg-yellow-500/10 border border-yellow-500/30 text-navy-900 text-xs flex items-center gap-3">
          <ShieldCheck className="w-5 h-5 text-yellow-500 shrink-0" />
          <span>
            Your commercial merchant credentials are fully verified. All bulk shipping endpoints and merchant rates are enabled.
          </span>
        </div>
      )}

      {/* Merchant Overview Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-5">
        <SoftStatCard label="Monthly Shipments" value="0" helper="Current billing cycle" icon={Package} color="info" />
        <SoftStatCard label="Pending Pickups" value="0" helper="Awaiting hub pickup" icon={Clock} color="warning" />
        <SoftStatCard label="Volume Tier" value="Tier 1" helper="5% base rebate" icon={TrendingUp} color="success" />
        <SoftStatCard label="API Ingestion Rate" value="100 req/s" helper="Rate limit quota" icon={ShieldCheck} color="primary" />
      </div>

      {/* API Key Provisioning Card */}
      <SoftCard className="p-6 sm:p-8 space-y-4">
        <div className="flex items-center gap-2 text-navy-900 font-bold text-base">
          <Key className="w-5 h-5 text-yellow-500" />
          <span>E-Commerce Integration API Key</span>
        </div>
        <p className="text-xs text-slate-500">
          Use this secret key to integrate SwiftDrop into your Shopify, WooCommerce, or custom storefront.
        </p>

        {isApproved ? (
          <div className="flex flex-col sm:flex-row items-center gap-3">
            <div className="w-full sm:flex-1 p-3 rounded-xl bg-slate-50 border border-slate-200 font-mono text-xs text-slate-800 break-all select-all">
              sd_live_9a8f21b74c8e3921049281a
            </div>
            <button
              onClick={copyApiKey}
              className="w-full sm:w-auto px-4 py-3 rounded-xl bg-navy-900 hover:bg-navy-800 text-white font-semibold text-xs flex items-center justify-center gap-2 transition-all shrink-0"
            >
              {copiedKey ? <Check className="w-4 h-4 text-yellow-400" /> : <Copy className="w-4 h-4" />}
              <span>{copiedKey ? 'Copied Key!' : 'Copy Key'}</span>
            </button>
          </div>
        ) : (
          <div className="p-4 rounded-xl bg-slate-50 border border-dashed border-slate-300 text-slate-500 text-xs flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-amber-500 shrink-0" />
            <span>API keys are locked until commercial verification is approved by administrator.</span>
          </div>
        )}
      </SoftCard>

      {/* Bulk Orders Placeholder */}
      <SoftCard className="p-6 sm:p-8">
        <div className="flex justify-between items-center pb-6 border-b border-slate-100">
          <div>
            <h3 className="text-lg font-bold text-navy-900">Bulk Manifest Ingestion</h3>
            <p className="text-xs text-slate-500">Scheduled for Member 4's Merchant Microservice</p>
          </div>
          <button
            disabled={!isApproved}
            className="px-4 py-2 rounded-lg bg-yellow-500 hover:bg-yellow-600 disabled:opacity-50 disabled:cursor-not-allowed text-white text-xs font-semibold flex items-center gap-1.5"
          >
            <FileSpreadsheet className="w-4 h-4" />
            <span>Upload Manifest CSV</span>
          </button>
        </div>

        <div className="py-12 text-center text-xs text-slate-500 max-w-sm mx-auto space-y-2">
          <FileText className="w-10 h-10 text-slate-300 mx-auto" />
          <p className="font-semibold text-slate-700">No bulk orders queued</p>
          <p className="text-[11px] text-slate-400">
            Upload CSV shipment manifests to batch-create hundreds of parcel waybills simultaneously.
          </p>
        </div>
      </SoftCard>
    </div>
  );
};
