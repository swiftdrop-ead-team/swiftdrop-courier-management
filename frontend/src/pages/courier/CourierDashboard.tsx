import React, { useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { SoftBadge, SoftCard, SoftStatCard } from '../../components/ui/SoftUI';
import {
  Truck,
  MapPin,
  CheckCircle2,
  Clock,
  Power,
  Layers,
  Navigation,
  Zap,
} from 'lucide-react';

export const CourierDashboard: React.FC = () => {
  const { user } = useAuth();
  const [isOnline, setIsOnline] = useState(true);

  return (
    <div className="space-y-8 animate-fade-in">

      {/* ══ Header Card ══ */}
      <SoftCard className="p-6 sm:p-8 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-6">
        <div className="space-y-1">
          <div className="flex items-center gap-3">
            {/* Calm blue icon bubble */}
            <div className="w-10 h-10 rounded-xl bg-blue-100 flex items-center justify-center shadow-sm">
              <Truck className="w-5 h-5 text-navy-900" />
            </div>
            <h2 className="text-2xl font-bold text-navy-900">Courier Dispatch Console</h2>
          </div>
          <p className="text-xs text-slate-500 ml-13">
            Courier:{' '}
            <span className="font-semibold text-slate-800">{user?.fullName}</span> — Zone: Bay
            Area Central Hub
          </p>
        </div>

        {/* Online / Offline toggle */}
        <button
          onClick={() => setIsOnline(!isOnline)}
          className={`flex items-center gap-2.5 px-5 py-2.5 rounded-full font-bold text-xs shadow-sm transition-all ${
            isOnline
              ? 'bg-green-50 text-green-800 border border-green-200 hover:bg-green-100'
              : 'bg-slate-100 text-slate-600 border border-slate-300 hover:bg-slate-200'
          }`}
        >
          <Power className={`w-4 h-4 ${isOnline ? 'text-green-500' : 'text-slate-400'}`} />
          {isOnline && <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse" />}
          {isOnline ? 'ONLINE: ACCEPTING DROPS' : 'OFFLINE'}
        </button>
      </SoftCard>

      {/* ══ Status Alert ══ */}
      {isOnline ? (
        <div className="alert-success text-sm">
          <Zap className="w-4 h-4 text-green-600 shrink-0" />
          <span><strong>You are online.</strong> Dispatch is actively routing new deliveries to your manifest.</span>
        </div>
      ) : (
        <div className="alert-warning text-sm">
          <Clock className="w-4 h-4 text-amber-600 shrink-0" />
          <span><strong>You are offline.</strong> Go online to start receiving delivery assignments.</span>
        </div>
      )}

      {/* ══ Metrics
            primary → yellow   (assigned drops — brand accent)
            success → green    (delivered — positive outcome)
            info    → blue     (distance — informational / map context)
      ══ */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <SoftStatCard
          label="Assigned Manifest Drops"
          value="0"
          helper="Current run sequence"
          icon={Layers}
          color="primary"
        />
        <SoftStatCard
          label="Delivered Today"
          value="0"
          helper="100% on-time rating"
          icon={CheckCircle2}
          color="success"
        />
        <SoftStatCard
          label="Estimated Distance"
          value="0.0 km"
          helper="Route optimization active"
          icon={MapPin}
          color="info"
        />
      </div>

      {/* ══ Assigned Deliveries Manifest ══ */}
      <SoftCard className="p-6 sm:p-8">
        <div className="flex justify-between items-center pb-6 border-b border-blue-100">
          <div>
            <h3 className="text-lg font-bold text-navy-900">Current Run Manifest</h3>
            <p className="text-xs text-slate-500">
              Scheduled for Member 5's Dispatch & Route Optimization Service
            </p>
          </div>
          {/* Warning badge for standby */}
          <SoftBadge color="warning">Route Standby</SoftBadge>
        </div>

        {/* Empty state */}
        <div className="py-16 text-center max-w-sm mx-auto space-y-3">
          <div className="w-14 h-14 rounded-2xl bg-blue-50 border border-blue-200 flex items-center justify-center mx-auto shadow-sm">
            <Layers className="w-7 h-7 text-blue-600" />
          </div>
          <h4 className="font-bold text-navy-900 text-sm">No Active Parcels Assigned</h4>
          <p className="text-xs text-slate-500 leading-relaxed">
            When sorting hubs batch-assign delivery manifests to your vehicle, turn-by-turn
            waypoints and recipient signatures will show here.
          </p>
          <div className="flex justify-center pt-2">
            <div className="flex items-center gap-1.5 text-[11px] text-slate-500 font-medium bg-blue-50 px-3 py-1.5 rounded-full border border-blue-200">
              <Navigation className="w-3 h-3 text-blue-600" />
              <span>Awaiting hub dispatch</span>
            </div>
          </div>
        </div>
      </SoftCard>
    </div>
  );
};
