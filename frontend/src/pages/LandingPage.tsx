import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  Package, 
  Truck, 
  ArrowRight, 
  Building2, 
  Sparkles, 
  CheckCircle2, 
  Search, 
  AlertCircle 
} from 'lucide-react';

export const LandingPage: React.FC = () => {
  const [trackingNumber, setTrackingNumber] = useState('');
  const [trackingSearched, setTrackingSearched] = useState(false);

  const handleTrackSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (trackingNumber.trim()) {
      setTrackingSearched(true);
    }
  };

  return (
    <div className="flex flex-col">
      {/* Hero Section */}
      <section className="relative overflow-hidden bg-gradient-to-b from-navy-950 via-navy-900 to-navy-900 text-white pt-20 pb-28">
        {/* Subtle background route grid art */}
        <div className="absolute inset-0 opacity-10 pointer-events-none bg-[radial-gradient(#FFCB05_1px,transparent_1px)] [background-size:24px_24px]"></div>

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            {/* Left Content Column */}
            <div className="lg:col-span-7 space-y-6">
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-yellow-500/10 border border-yellow-500/30 text-yellow-400 text-xs font-semibold tracking-wide uppercase">
                <Sparkles className="w-3.5 h-3.5 text-yellow-400" />
                <span>Enterprise Logistics Microservices Platform</span>
              </div>

              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight leading-tight">
                Delivering trust, <br />
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-yellow-400 via-yellow-300 to-yellow-200">
                  every single mile.
                </span>
              </h1>

              <p className="text-lg text-slate-300 max-w-xl leading-relaxed">
                SwiftDrop connects individual shippers, commercial merchants, sorting hubs, and field couriers through high-performance Spring Boot microservices and real-time tracking.
              </p>

              {/* CTAs */}
              <div className="flex flex-col sm:flex-row gap-4 pt-2">
                <Link
                  to="/register"
                  className="inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl font-bold text-navy-950 bg-yellow-500 hover:bg-yellow-400 shadow-lg shadow-yellow-500/20 transition-all hover:translate-y-[-1px]"
                >
                  <span>Ship a Parcel</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>

                <Link
                  to="/merchant/register"
                  className="inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl font-semibold text-slate-200 bg-navy-800/80 hover:bg-navy-800 border border-slate-700 transition-all"
                >
                  <Building2 className="w-4 h-4 text-yellow-400" />
                  <span>Merchant Portal</span>
                </Link>

                <Link
                  to="/login"
                  className="inline-flex items-center justify-center px-6 py-3.5 rounded-xl font-semibold text-slate-300 hover:text-white transition-all"
                >
                  <span>Sign In</span>
                </Link>
              </div>

              {/* Trust Metrics */}
              <div className="grid grid-cols-3 gap-6 pt-8 border-t border-slate-800/80">
                <div>
                  <div className="text-2xl font-bold text-white">100%</div>
                  <div className="text-xs text-slate-400 font-medium mt-0.5">Secure JWT Auth</div>
                </div>
                <div>
                  <div className="text-2xl font-bold text-yellow-400">10</div>
                  <div className="text-xs text-slate-400 font-medium mt-0.5">Collaborative Services</div>
                </div>
                <div>
                  <div className="text-2xl font-bold text-yellow-400">Sub-sec</div>
                  <div className="text-xs text-slate-400 font-medium mt-0.5">Gateway Latency</div>
                </div>
              </div>
            </div>

            {/* Right Interactive Tracking Box */}
            <div id="tracking" className="lg:col-span-5">
              <div className="bg-white rounded-2xl p-6 sm:p-8 shadow-2xl border border-slate-100 text-slate-800">
                <div className="flex items-center gap-3 mb-6">
                  <div className="p-2.5 rounded-xl bg-yellow-500/10 text-yellow-600">
                    <Search className="w-6 h-6 text-yellow-600" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-navy-900">Track Your Shipment</h3>
                    <p className="text-xs text-slate-500">Instant consignment & parcel status</p>
                  </div>
                </div>

                <form onSubmit={handleTrackSubmit} className="space-y-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1.5 uppercase tracking-wide">
                      Consignment / Waybill #
                    </label>
                    <div className="relative">
                      <input
                        type="text"
                        placeholder="e.g. SD-9842-8721-LK"
                        value={trackingNumber}
                        onChange={(e) => setTrackingNumber(e.target.value)}
                        className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:border-yellow-500 focus:ring-2 focus:ring-yellow-500/20 text-sm font-medium text-slate-900 placeholder-slate-400 outline-none transition-all"
                      />
                    </div>
                  </div>

                  <button
                    type="submit"
                    className="w-full py-3.5 px-4 rounded-xl bg-navy-900 hover:bg-navy-800 text-white font-semibold text-sm shadow-md transition-all flex items-center justify-center gap-2"
                  >
                    <Search className="w-4 h-4" />
                    <span>Search Shipment</span>
                  </button>
                </form>

                {/* Tracking Query Result / Status */}
                {trackingSearched && (
                  <div className="mt-5 p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 animate-fade-in">
                    <div className="flex items-start gap-3">
                      <AlertCircle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                      <div className="text-xs space-y-1">
                        <p className="font-bold">Tracking Service Scheduled for Phase 2</p>
                        <p className="text-amber-800 leading-relaxed">
                          Waybill <span className="font-mono font-semibold">{trackingNumber}</span> is formatted correctly. Real-time GPS and WebSocket tracking will connect to Member 7's dedicated Tracking Microservice.
                        </p>
                      </div>
                    </div>
                  </div>
                )}

                <div className="mt-6 pt-6 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
                  <span>Supported format: SD-XXXX-XXXX-XX</span>
                  <Link to="/login" className="text-yellow-600 font-semibold hover:underline">
                    View in Dashboard â†’
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Services & Capabilities Section */}
      <section id="services" className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <h2 className="text-xs font-bold text-yellow-600 uppercase tracking-widest mb-2">
              End-to-End Delivery Architecture
            </h2>
            <h3 className="text-3xl font-bold text-navy-900">
              Modern Logistics Engineered for Speed and Reliability
            </h3>
            <p className="text-sm text-slate-600 mt-3">
              Purpose-built microservices coordinating dispatch, dynamic pricing, and last-mile couriers.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {/* Card 1: Retail Customers */}
            <div className="p-8 rounded-2xl bg-slate-50 border border-slate-100 hover:border-slate-200 transition-all hover:shadow-lg hover:shadow-slate-200/50 flex flex-col justify-between">
              <div>
                <div className="w-12 h-12 rounded-xl bg-yellow-500/15 text-yellow-600 flex items-center justify-center mb-6">
                  <Package className="w-6 h-6" />
                </div>
                <h4 className="text-xl font-bold text-navy-900 mb-2">Individual Shippers</h4>
                <p className="text-sm text-slate-600 leading-relaxed mb-6">
                  Simple one-click parcel dispatch, transparent pricing calculations, instant waybill generation, and door-to-door delivery.
                </p>
                <ul className="space-y-2 text-xs text-slate-700">
                  <li className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-yellow-500" />
                    <span>Real-time shipping rate estimates</span>
                  </li>
                  <li className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-yellow-500" />
                    <span>Flexible drop-off or home pickup</span>
                  </li>
                  <li className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-yellow-500" />
                    <span>SMS and email delivery confirmation</span>
                  </li>
                </ul>
              </div>

              <div className="pt-6 mt-6 border-t border-slate-200/70">
                <Link to="/register" className="inline-flex items-center gap-1.5 text-sm font-bold text-yellow-600 hover:text-yellow-700">
                  <span>Sign Up as Customer</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            </div>

            {/* Card 2: Commercial Merchants */}
            <div className="p-8 rounded-2xl bg-navy-900 text-white relative overflow-hidden flex flex-col justify-between shadow-xl">
              <div className="absolute top-0 right-0 p-4">
                <span className="px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider bg-yellow-500/20 text-yellow-400 border border-yellow-500/30">
                  B2B Commerce
                </span>
              </div>

              <div>
                <div className="w-12 h-12 rounded-xl bg-yellow-500/20 text-yellow-400 flex items-center justify-center mb-6">
                  <Building2 className="w-6 h-6" />
                </div>
                <h4 className="text-xl font-bold text-white mb-2">Commercial Merchants</h4>
                <p className="text-sm text-slate-300 leading-relaxed mb-6">
                  High-volume parcel shipping, bulk manifest uploads, commercial invoice reconciliation, and automated courier scheduling.
                </p>
                <ul className="space-y-2 text-xs text-slate-300">
                  <li className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-yellow-400" />
                    <span>Admin verification & compliance review</span>
                  </li>
                  <li className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-yellow-400" />
                    <span>Dedicated Merchant API integration keys</span>
                  </li>
                  <li className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-yellow-400" />
                    <span>Tiered volume shipping discounts</span>
                  </li>
                </ul>
              </div>

              <div className="pt-6 mt-6 border-t border-navy-800">
                <Link to="/merchant/register" className="inline-flex items-center gap-1.5 text-sm font-semibold text-yellow-400 hover:text-yellow-300">
                  <span>Register Merchant Account</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            </div>

            {/* Card 3: Field Couriers */}
            <div className="p-8 rounded-2xl bg-slate-50 border border-slate-100 hover:border-slate-200 transition-all hover:shadow-lg hover:shadow-slate-200/50 flex flex-col justify-between">
              <div>
                <div className="w-12 h-12 rounded-xl bg-navy-100 text-navy-800 flex items-center justify-center mb-6">
                  <Truck className="w-6 h-6" />
                </div>
                <h4 className="text-xl font-bold text-navy-900 mb-2">Courier Network</h4>
                <p className="text-sm text-slate-600 leading-relaxed mb-6">
                  Optimized manifest routing, turn-by-turn navigation, digital proof-of-delivery capture, and instant payout tracking.
                </p>
                <ul className="space-y-2 text-xs text-slate-700">
                  <li className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-navy-700" />
                    <span>Smart zone dispatch allocation</span>
                  </li>
                  <li className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-navy-700" />
                    <span>Photo proof-of-delivery</span>
                  </li>
                  <li className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-navy-700" />
                    <span>Mobile-first courier console</span>
                  </li>
                </ul>
              </div>

              <div className="pt-6 mt-6 border-t border-slate-200/70">
                <Link to="/login" className="inline-flex items-center gap-1.5 text-sm font-semibold text-navy-800 hover:text-navy-900">
                  <span>Courier Login</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Banner Section */}
      <section className="bg-gradient-to-r from-navy-950 via-navy-900 to-navy-800 text-white py-14 border-y border-navy-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between gap-6 text-center md:text-left">
          <div>
            <h3 className="text-2xl font-bold">Ready to dispatch your first shipment?</h3>
            <p className="text-slate-300 text-sm mt-1">
              Join thousands of businesses and individuals trusting SwiftDrop every day.
            </p>
          </div>
          <div className="flex items-center gap-4">
            <Link
              to="/register"
              className="px-6 py-3 rounded-xl bg-yellow-500 text-navy-950 hover:bg-yellow-400 font-bold text-sm shadow-md transition-all"
            >
              Create Free Account
            </Link>
            <Link
              to="/login"
              className="px-6 py-3 rounded-xl border border-yellow-500/50 hover:bg-navy-800 text-yellow-400 font-semibold text-sm transition-all"
            >
              Sign In
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
};

