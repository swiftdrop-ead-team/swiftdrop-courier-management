import React, { useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { SoftBadge, SoftCard, SoftStatCard } from '../../components/ui/SoftUI';
import { 
  Truck, 
  MapPin, 
  CheckCircle2, 
  Clock, 
  Navigation, 
  PhoneCall, 
  Camera, 
  Power,
  Shield,
  Layers
} from 'lucide-react';

export const CourierDashboard: React.FC = () => {
  const { user } = useAuth();
  const [isOnline, setIsOnline] = useState(true);

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header & Online Status Toggle */}
      <SoftCard className="p-6 sm:p-8 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-6">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Truck className="w-5 h-5 text-amber-600" />
            <h2 className="text-2xl font-bold text-navy-900">Courier Dispatch Console</h2>
          </div>
          <p className="text-xs text-slate-500">
            Courier: <span className="font-semibold text-slate-800">{user?.fullName}</span> - Zone: Bay Area Central Hub
          </p>
        </div>

        {/* Toggle Online Status */}
        <button
          onClick={() => setIsOnline(!isOnline)}
          className={`flex items-center gap-2.5 px-5 py-2.5 rounded-full font-bold text-xs shadow-sm transition-all ${
            isOnline
              ? 'bg-yellow-500/10 text-navy-900 border border-yellow-500/40 hover:bg-yellow-500/20'
              : 'bg-slate-100 text-slate-600 border border-slate-300 hover:bg-slate-200'
          }`}
        >
          <Power className={`w-4 h-4 ${isOnline ? 'text-yellow-500' : 'text-slate-400'}`} />
          <span>{isOnline ? 'ONLINE: ACCEPTING DROPS' : 'OFFLINE'}</span>
        </button>
      </SoftCard>

      {/* Real-Time Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <SoftStatCard
          label="Assigned Manifest Drops"
          value="0"
          helper="Current run sequence"
          icon={Layers}
          color="info"
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
          color="warning"
        />
      </div>

      {/* Assigned Deliveries Manifest Placeholder */}
      <SoftCard className="p-6 sm:p-8">
        <div className="flex justify-between items-center pb-6 border-b border-slate-100">
          <div>
            <h3 className="text-lg font-bold text-navy-900">Current Run Manifest</h3>
            <p className="text-xs text-slate-500">Scheduled for Member 5's Dispatch & Route Optimization Service</p>
          </div>
          <SoftBadge color="warning">Route Standby</SoftBadge>
        </div>

        {/* Empty state */}
        <div className="py-16 text-center max-w-sm mx-auto space-y-3">
          <div className="w-14 h-14 rounded-2xl bg-amber-50 text-amber-700 flex items-center justify-center mx-auto">
            <Layers className="w-7 h-7" />
          </div>
          <h4 className="font-bold text-navy-900 text-sm">No Active Parcels Assigned</h4>
          <p className="text-xs text-slate-500 leading-relaxed">
            When sorting hubs batch-assign delivery manifests to your vehicle, turn-by-turn waypoints and recipient signatures will show here.
          </p>
        </div>
      </SoftCard>
    </div>
  );
};
