import React, { useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { 
  Package, 
  Send, 
  MapPin, 
  Clock, 
  Search, 
  Plus, 
  CheckCircle2, 
  AlertCircle,
  ArrowUpRight,
  ShieldCheck,
  Calendar
} from 'lucide-react';

export const CustomerDashboard: React.FC = () => {
  const { user } = useAuth();
  const [showBookingModal, setShowBookingModal] = useState(false);
  const [bookingSuccess, setBookingSuccess] = useState(false);
  const [recipientName, setRecipientName] = useState('');
  const [deliveryAddress, setDeliveryAddress] = useState('');
  const [parcelWeight, setParcelWeight] = useState('1.5');
  const [deliveryType, setDeliveryType] = useState('STANDARD');

  const handleBookDelivery = (e: React.FormEvent) => {
    e.preventDefault();
    setBookingSuccess(true);
    setTimeout(() => {
      setShowBookingModal(false);
      setBookingSuccess(false);
      setRecipientName('');
      setDeliveryAddress('');
    }, 2000);
  };

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Welcome Greeting Banner */}
      <div className="p-6 sm:p-8 rounded-3xl bg-gradient-to-r from-navy-900 via-navy-800 to-teal-900 text-white shadow-xl relative overflow-hidden">
        <div className="relative z-10 max-w-2xl space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-teal-500/20 text-teal-300 text-xs font-semibold">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Retail Customer Portal</span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            Hello, {user?.fullName}!
          </h2>
          <p className="text-sm text-slate-300 leading-relaxed">
            Ready to dispatch or track your shipments? Book a new pickup or check real-time consignment statuses below.
          </p>

          <div className="flex flex-wrap gap-3 pt-2">
            <button
              onClick={() => setShowBookingModal(true)}
              className="px-5 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-semibold text-xs shadow-md flex items-center gap-2 transition-all"
            >
              <Plus className="w-4 h-4" />
              <span>Book New Delivery</span>
            </button>
            <a
              href="#tracking-widget"
              className="px-5 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white font-semibold text-xs transition-all flex items-center gap-2"
            >
              <Search className="w-4 h-4" />
              <span>Track Consignment</span>
            </a>
          </div>
        </div>
      </div>

      {/* Quick Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="p-5 rounded-2xl bg-white border border-slate-200/80 shadow-sm flex items-center gap-4">
          <div className="p-3 rounded-xl bg-teal-50 text-teal-700">
            <Package className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs font-medium text-slate-500">Active Shipments</span>
            <h4 className="text-2xl font-bold text-navy-900">0</h4>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-white border border-slate-200/80 shadow-sm flex items-center gap-4">
          <div className="p-3 rounded-xl bg-blue-50 text-blue-700">
            <Clock className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs font-medium text-slate-500">Scheduled Pickups</span>
            <h4 className="text-2xl font-bold text-navy-900">0</h4>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-white border border-slate-200/80 shadow-sm flex items-center gap-4">
          <div className="p-3 rounded-xl bg-emerald-50 text-emerald-700">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs font-medium text-slate-500">Completed Deliveries</span>
            <h4 className="text-2xl font-bold text-navy-900">0</h4>
          </div>
        </div>
      </div>

      {/* Deliveries Section & Empty State */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 sm:p-8">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center pb-6 border-b border-slate-100 gap-4">
          <div>
            <h3 className="text-lg font-bold text-navy-900">Recent Parcel Activity</h3>
            <p className="text-xs text-slate-500">Your dispatched items and incoming delivery history</p>
          </div>
          <button
            onClick={() => setShowBookingModal(true)}
            className="px-4 py-2 rounded-lg bg-navy-900 hover:bg-navy-800 text-white text-xs font-semibold flex items-center gap-1.5"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>New Waybill</span>
          </button>
        </div>

        {/* Empty State Presentation */}
        <div className="py-16 text-center max-w-sm mx-auto space-y-4">
          <div className="w-16 h-16 rounded-2xl bg-slate-50 border border-slate-100 text-slate-400 flex items-center justify-center mx-auto">
            <Package className="w-8 h-8" />
          </div>
          <div className="space-y-1">
            <h4 className="text-base font-bold text-navy-900">No active shipments yet</h4>
            <p className="text-xs text-slate-500 leading-relaxed">
              When you send a parcel or receive packages via SwiftDrop, your real-time tracking checkpoints will appear right here.
            </p>
          </div>
          <button
            onClick={() => setShowBookingModal(true)}
            className="px-5 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-700 text-white font-semibold text-xs shadow-sm transition-all"
          >
            Create Your First Shipment
          </button>
        </div>
      </div>

      {/* Booking Modal / Action */}
      {showBookingModal && (
        <div className="fixed inset-0 z-50 bg-navy-950/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl border border-slate-100 animate-slide-up space-y-5">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2 text-navy-900 font-bold text-base">
                <Send className="w-5 h-5 text-teal-600" />
                <span>Book Parcel Shipment</span>
              </div>
              <button
                onClick={() => setShowBookingModal(false)}
                className="text-slate-400 hover:text-slate-600 text-sm font-semibold"
              >
                ✕
              </button>
            </div>

            {bookingSuccess ? (
              <div className="py-8 text-center space-y-3">
                <CheckCircle2 className="w-12 h-12 text-teal-600 mx-auto" />
                <h4 className="font-bold text-navy-900">Waybill Registered!</h4>
                <p className="text-xs text-slate-500">
                  Consignment order dispatched. Scheduled for pickup.
                </p>
              </div>
            ) : (
              <form onSubmit={handleBookDelivery} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Recipient Full Name
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Michael Scott"
                    value={recipientName}
                    onChange={(e) => setRecipientName(e.target.value)}
                    className="w-full px-3.5 py-2.5 text-xs rounded-xl border border-slate-200 focus:border-teal-600 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Destination Address
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. 1725 Slough Avenue, Scranton, PA"
                    value={deliveryAddress}
                    onChange={(e) => setDeliveryAddress(e.target.value)}
                    className="w-full px-3.5 py-2.5 text-xs rounded-xl border border-slate-200 focus:border-teal-600 outline-none"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Weight (kg)
                    </label>
                    <input
                      type="number"
                      step="0.1"
                      value={parcelWeight}
                      onChange={(e) => setParcelWeight(e.target.value)}
                      className="w-full px-3.5 py-2.5 text-xs rounded-xl border border-slate-200 focus:border-teal-600 outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Urgency Tier
                    </label>
                    <select
                      value={deliveryType}
                      onChange={(e) => setDeliveryType(e.target.value)}
                      className="w-full px-3.5 py-2.5 text-xs rounded-xl border border-slate-200 focus:border-teal-600 outline-none"
                    >
                      <option value="STANDARD">Standard (2-3 days)</option>
                      <option value="EXPRESS">Express (Next Day)</option>
                      <option value="SAME_DAY">Same-Day Rush</option>
                    </select>
                  </div>
                </div>

                <div className="pt-2">
                  <button
                    type="submit"
                    className="w-full py-3 rounded-xl bg-teal-600 hover:bg-teal-700 text-white font-semibold text-xs shadow-md transition-all"
                  >
                    Confirm & Generate Waybill
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
