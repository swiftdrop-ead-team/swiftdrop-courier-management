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
  FileText,
  XCircle,
} from 'lucide-react';

export const MerchantDashboard: React.FC = () => {
  const { user } = useAuth();
  const [copiedKey, setCopiedKey] = useState(false);

  const isApproved = user?.merchantApprovalStatus === 'APPROVED';
  const isPending  = user?.merchantApprovalStatus === 'PENDING' || !user?.merchantApprovalStatus;
  const isRejected = user?.merchantApprovalStatus === 'REJECTED';

  const copyApiKey = () => {
    navigator.clipboard.writeText('sd_live_9a8f21b74c8e3921049281a');
    setCopiedKey(true);
    setTimeout(() => setCopiedKey(false), 2000);
  };

  return (
    <div className="space-y-8 animate-fade-in">

      {/* ══ Business Header ══ */}
      <SoftCard className="p-6 sm:p-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            {/* Calm blue icon bubble */}
            <div className="w-10 h-10 rounded-xl bg-blue-100 flex items-center justify-center shadow-sm">
              <Building2 className="w-5 h-5 text-navy-900" />
            </div>
            <h2 className="text-2xl font-bold text-navy-900">Commercial Merchant Portal</h2>
          </div>
          <p className="text-xs text-slate-500 ml-13">
            Account Owner:{' '}
            <span className="font-semibold text-slate-800">{user?.fullName}</span>{' '}
            ({user?.email})
          </p>
        </div>

        {/* Status badge — semantic */}
        {isApproved && (
          <div className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-green-50 border border-green-200 text-green-800 text-xs font-bold shadow-sm">
            <CheckCircle2 className="w-4 h-4 text-green-600" />
            <span>COMMERCIAL ACCOUNT: APPROVED</span>
          </div>
        )}
        {isPending && (
          <div className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs font-bold shadow-sm animate-pulse">
            <Clock className="w-4 h-4 text-amber-600" />
            <span>APPROVAL STATUS: PENDING REVIEW</span>
          </div>
        )}
        {isRejected && (
          <div className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs font-bold shadow-sm">
            <XCircle className="w-4 h-4 text-red-600" />
            <span>APPROVAL STATUS: REJECTED</span>
          </div>
        )}
      </SoftCard>

      {/* ══ Pending Banner — amber ══ */}
      {isPending && (
        <div className="p-6 rounded-2xl bg-amber-50 border border-amber-200 space-y-3">
          <div className="flex items-start gap-3">
            <div className="w-9 h-9 rounded-xl bg-amber-100 flex items-center justify-center shrink-0 mt-0.5">
              <Clock className="w-5 h-5 text-amber-600" />
            </div>
            <div className="space-y-1">
              <h4 className="font-bold text-sm text-amber-900">
                Application Under Verification by SwiftDrop Administrator
              </h4>
              <p className="text-xs text-amber-800 leading-relaxed">
                Thank you for applying to become a SwiftDrop merchant partner. Your registration
                details are currently undergoing compliance review. Commercial features will unlock
                automatically once approved.
              </p>
              <div className="pt-2 text-[11px] text-amber-700 font-semibold flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
                <span>Tip: Sign in as Admin (admin@swiftdrop.io) to approve this account in real time.</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ══ Approved Banner — yellow brand ══ */}
      {isApproved && (
        <div className="p-4 rounded-xl bg-blue-50 border border-blue-200 text-navy-900 text-xs flex items-center gap-3">
          <ShieldCheck className="w-5 h-5 text-blue-600 shrink-0" />
          <span className="font-medium">
            Your commercial merchant credentials are fully verified. All bulk shipping endpoints and merchant rates are enabled.
          </span>
        </div>
      )}

      {/* ══ Rejected Banner — red ══ */}
      {isRejected && (
        <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 text-red-500 shrink-0" />
          <span>
            Your merchant application was not approved. Please contact{' '}
            <strong>support@swiftdrop.io</strong> for clarification.
          </span>
        </div>
      )}

      {/* ══ Overview Metrics
            primary → yellow bubble (brand)
            warning → amber  (pending pickups — attention needed)
            success → green  (volume/health)
            info    → blue   (rate/quota — informational only)
      ══ */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-5">
        <SoftStatCard label="Monthly Shipments"    value="0"        helper="Current billing cycle" icon={Package}    color="primary" />
        <SoftStatCard label="Pending Pickups"      value="0"        helper="Awaiting hub pickup"   icon={Clock}      color="warning" />
        <SoftStatCard label="Volume Tier"          value="Tier 1"   helper="5% base rebate"        icon={TrendingUp} color="success" />
        <SoftStatCard label="API Ingestion Rate"   value="100 req/s" helper="Rate limit quota"     icon={ShieldCheck} color="info"   />
      </div>

      {/* ══ API Key Provisioning ══ */}
      <SoftCard className="p-6 sm:p-8 space-y-4">
        <div className="flex items-center gap-3 text-navy-900 font-bold text-base">
          <div className="w-8 h-8 rounded-lg bg-blue-100 flex items-center justify-center">
            <Key className="w-4 h-4 text-navy-900" />
          </div>
          <span>E-Commerce Integration API Key</span>
        </div>
        <p className="text-xs text-slate-500">
          Use this secret key to integrate SwiftDrop into your Shopify, WooCommerce, or custom
          storefront.
        </p>

        {isApproved ? (
          <div className="flex flex-col sm:flex-row items-center gap-3">
            <div className="w-full sm:flex-1 p-3 rounded-xl bg-blue-50 border border-blue-200 font-mono text-xs text-navy-900 break-all select-all">
              sd_live_9a8f21b74c8e3921049281a
            </div>
            <button
              onClick={copyApiKey}
              className={`w-full sm:w-auto px-4 py-3 rounded-xl font-bold text-xs flex items-center justify-center gap-2 transition-all shrink-0 shadow-sm ${
                copiedKey
                  ? 'bg-green-600 hover:bg-green-700 text-white'
                  : 'bg-blue-600 hover:bg-blue-700 text-white'
              }`}
            >
              {copiedKey ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
              <span>{copiedKey ? 'Copied!' : 'Copy Key'}</span>
            </button>
          </div>
        ) : (
          <div className="p-4 rounded-xl bg-slate-50 border border-dashed border-slate-300 text-slate-500 text-xs flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-amber-500 shrink-0" />
            <span>API keys are locked until commercial verification is approved by administrator.</span>
          </div>
        )}
      </SoftCard>

      {/* ══ Bulk Orders Placeholder ══ */}
      <SoftCard className="p-6 sm:p-8">
        <div className="flex justify-between items-center pb-6 border-b border-blue-100">
          <div>
            <h3 className="text-lg font-bold text-navy-900">Bulk Manifest Ingestion</h3>
            <p className="text-xs text-slate-500">Scheduled for Member 4's Merchant Microservice</p>
          </div>
          {/* Yellow CTA button */}
          <button
            disabled={!isApproved}
            className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed text-white text-xs font-bold flex items-center gap-1.5 transition-colors shadow-sm"
          >
            <FileSpreadsheet className="w-4 h-4" />
            <span>Upload Manifest CSV</span>
          </button>
        </div>

        <div className="py-12 text-center text-xs text-slate-500 max-w-sm mx-auto space-y-2">
          <div className="w-12 h-12 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center mx-auto">
            <FileText className="w-6 h-6 text-blue-500" />
          </div>
          <p className="font-semibold text-slate-700">No bulk orders queued</p>
          <p className="text-[11px] text-slate-400">
            Upload CSV shipment manifests to batch-create hundreds of parcel waybills simultaneously.
          </p>
        </div>
      </SoftCard>
    </div>
  );
};
