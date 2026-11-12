import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowRight,
  Building2,
  CheckCircle2,
  ChevronRight,
  Clock3,
  MapPin,
  Package,
  Search,
  ShieldCheck,
  Sparkles,
  Truck,
} from 'lucide-react';
import { ActionLink } from '../components/ui/ActionLink';
import { Button } from '../components/ui/Button';

const capabilities = [
  {
    icon: Package,
    label: 'For individuals',
    title: 'Send with confidence',
    description: 'Clear rates, simple pickup choices, and delivery updates that keep you in control.',
    href: '/register',
    action: 'Create a customer account',
  },
  {
    icon: Building2,
    label: 'For merchants',
    title: 'Scale without the noise',
    description: 'Move high-volume shipments with manifests, approvals, and a connected delivery network.',
    href: '/merchant/register',
    action: 'Explore merchant solutions',
  },
  {
    icon: Truck,
    label: 'For couriers',
    title: 'Own every stop',
    description: 'Organize routes, capture proof of delivery, and keep payout visibility in one place.',
    href: '/login',
    action: 'Open courier login',
  },
];

export const LandingPage: React.FC = () => {
  const [trackingNumber, setTrackingNumber] = useState('');
  const [trackingSearched, setTrackingSearched] = useState(false);

  const handleTrackSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    if (trackingNumber.trim()) {
      setTrackingSearched(true);
    }
  };

  return (
    <div className="bg-canvas text-slate-900">
      {import.meta.env.DEV && <div className="bg-blue-50 px-4 py-3 text-center text-sm"><Link to="/dashboard-preview" className="font-semibold text-blue-800">Open dashboard preview portal → No login needed</Link></div>}
      <section className="relative min-h-[calc(100vh-4rem)] overflow-hidden bg-ink text-white">
        <div className="absolute inset-0 bg-gradient-to-br from-ink-soft via-ink to-ink-deep" />
        <div className="absolute inset-0 opacity-20 [background-image:linear-gradient(rgba(255,255,255,0.06)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.06)_1px,transparent_1px)] [background-size:48px_48px]" />

        <div className="relative mx-auto grid min-h-[calc(100vh-4rem)] max-w-7xl items-center gap-8 px-4 pb-14 pt-12 sm:px-6 lg:grid-cols-[0.86fr_1.14fr] lg:gap-0 lg:px-8 lg:pb-0 lg:pt-0">
          <div className="relative z-20 max-w-xl lg:pb-10">
            <div className="mb-7 flex items-center gap-2 text-[11px] font-bold uppercase tracking-[0.18em] text-accent">
              <span className="h-px w-8 bg-accent" aria-hidden="true" />
              Service since day one
            </div>
            <h1 className="max-w-xl text-5xl font-black leading-[0.98] tracking-[-0.03em] text-white sm:text-6xl lg:text-[4.5rem]">
              We move what matters,
              <span className="block text-accent">every single day.</span>
            </h1>
            <p className="mt-7 max-w-md text-sm leading-6 text-slate-300 sm:text-base">
              From a single parcel to a full merchant operation, SwiftDrop makes every delivery feel clear, connected, and cared for.
            </p>
            <div className="mt-9 flex flex-wrap items-center gap-4">
              <ActionLink to="/register" className="rounded-full bg-gradient-to-r from-accent-bright to-accent px-7 py-3.5 text-sm hover:-translate-y-0.5">
                Start shipping <ArrowRight className="h-4 w-4" aria-hidden="true" />
              </ActionLink>
              <Link to="/merchant/register" className="inline-flex items-center gap-2 text-sm font-bold text-white transition-colors hover:text-accent">
                <Building2 className="h-4 w-4 text-accent" aria-hidden="true" />
                Merchant solutions
              </Link>
            </div>
            <Link
              to="/customer/dashboard-preview"
              className="mt-5 inline-flex items-center gap-2 text-xs font-semibold text-slate-300 transition-colors hover:text-accent"
            >
              Preview customer dashboard
              <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
            </Link>

            <div className="mt-12 flex items-end gap-3">
              <div className="w-28 border border-white/10 bg-white/10 px-4 py-4 backdrop-blur-sm">
                <p className="text-3xl font-black text-white">24/7</p>
                <p className="mt-1 text-[10px] uppercase leading-4 tracking-wide text-slate-300">Shipment visibility</p>
              </div>
              <div className="w-36 border border-accent/50 bg-accent px-4 py-4 text-ink">
                <p className="text-3xl font-black">99.98%</p>
                <p className="mt-1 text-[10px] uppercase leading-4 tracking-wide text-ink/70">Service availability</p>
              </div>
            </div>
          </div>

          <div className="relative min-h-[560px] self-end lg:min-h-[700px]">
            <div className="absolute right-0 top-[7%] h-[78%] w-[76%] bg-gradient-to-br from-accent-bright to-accent sm:w-[70%]" />
            <div className="absolute right-[13%] top-[15%] h-72 w-72 rounded-full border border-white/20 sm:h-96 sm:w-96" />
            <div className="absolute right-[22%] top-[24%] h-56 w-56 rounded-full border-[22px] border-gold-600/80 sm:h-72 sm:w-72" />
            <div className="absolute right-[31%] top-[33%] h-40 w-40 rounded-full border-[18px] border-gold-400/80 sm:h-52 sm:w-52" />
            <div className="absolute bottom-[4%] right-[4%] h-28 w-28 border-b-2 border-r-2 border-accent" aria-hidden="true" />
            <div className="absolute right-[6%] top-[5%] h-24 w-24 border-r-2 border-t-2 border-accent" aria-hidden="true" />
            <div className="absolute right-[9%] top-[10%] z-20 hidden border border-white/20 bg-ink-soft/80 px-4 py-2 text-[10px] font-bold uppercase tracking-[0.16em] text-white backdrop-blur-sm sm:block">
              SwiftDrop / Live route
            </div>
            <img
              src="https://images.unsplash.com/photo-1558981806-ec527fa84c39?auto=format&fit=crop&w=1200&q=88"
              alt="Motorcycle prepared for a SwiftDrop delivery route"
              onError={(event) => {
                event.currentTarget.src = 'https://images.unsplash.com/photo-1519003722824-194d4455a60c?auto=format&fit=crop&w=1200&q=88';
              }}
              className="absolute bottom-0 right-[1%] z-10 h-[88%] w-[94%] object-cover object-center drop-shadow-[0_24px_24px_rgba(0,0,0,0.28)] sm:right-[4%] sm:w-[84%]"
            />
            <div className="absolute bottom-8 left-0 z-20 hidden border border-white/10 bg-ink-soft/90 px-5 py-4 backdrop-blur-sm sm:block">
              <p className="text-3xl font-black text-white">10+</p>
              <p className="mt-1 text-[10px] uppercase tracking-wide text-slate-300">Connected services</p>
            </div>
          </div>
        </div>
      </section>

      <section id="tracking" className="border-b border-slate-200 bg-white py-8">
        <div className="mx-auto flex max-w-7xl flex-col gap-6 px-4 sm:px-6 md:flex-row md:items-center md:justify-between lg:px-8">
          <div className="flex items-start gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-accent-pale text-yellow-800"><Search className="h-5 w-5" aria-hidden="true" /></div>
            <div><p className="text-xs font-bold uppercase tracking-[0.16em] text-yellow-800">Track a shipment</p><p className="mt-1 text-sm text-slate-500">Know the next handoff before it happens.</p></div>
          </div>
          <form onSubmit={handleTrackSubmit} className="flex w-full max-w-xl flex-col gap-2 sm:flex-row">
            <label htmlFor="tracking-number" className="sr-only">Consignment number</label>
            <input id="tracking-number" value={trackingNumber} onChange={(event) => setTrackingNumber(event.target.value)} placeholder="Enter SD-9842-8721-LK" autoComplete="off" className="min-h-12 flex-1 rounded-full border border-slate-200 bg-slate-50 px-5 text-sm outline-none transition focus:border-accent-bright focus:bg-white focus:ring-4 focus:ring-accent-pale" />
            <Button type="submit" variant="secondary" className="rounded-full px-6"><Search className="h-4 w-4" aria-hidden="true" />Track parcel</Button>
          </form>
        </div>
        {trackingSearched && <div className="mx-auto mt-5 max-w-7xl px-4 sm:px-6 lg:px-8"><div role="status" className="flex items-start gap-2 text-xs text-amber-700"><Clock3 className="h-4 w-4" aria-hidden="true" />Tracking for <span className="font-mono font-bold">{trackingNumber}</span> is scheduled for the tracking service rollout.</div></div>}
      </section>

      <section id="services" className="bg-canvas py-20 sm:py-24">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="max-w-xl"><p className="text-xs font-bold uppercase tracking-[0.18em] text-yellow-800">Made for the whole journey</p><h2 className="mt-3 text-3xl font-black tracking-tight text-ink sm:text-4xl">One network. Every kind of delivery.</h2></div>
          <div className="mt-12 grid gap-5 lg:grid-cols-3">
            {capabilities.map(({ icon: Icon, label, title, description, href, action }, index) => <article key={title} className={`flex min-h-[320px] flex-col justify-between rounded-lg border p-7 ${index === 1 ? 'border-ink bg-ink text-white' : 'border-slate-200 bg-white'}`}><div><div className={`flex h-11 w-11 items-center justify-center rounded-full ${index === 1 ? 'bg-accent text-ink' : 'bg-accent-pale text-yellow-800'}`}><Icon className="h-5 w-5" aria-hidden="true" /></div><p className={`mt-8 text-[11px] font-bold uppercase tracking-[0.16em] ${index === 1 ? 'text-accent' : 'text-yellow-800'}`}>{label}</p><h3 className="mt-3 text-2xl font-black tracking-tight">{title}</h3><p className={`mt-3 text-sm leading-6 ${index === 1 ? 'text-slate-300' : 'text-slate-600'}`}>{description}</p></div><Link to={href} className={`mt-8 inline-flex items-center gap-2 border-t pt-5 text-sm font-bold ${index === 1 ? 'border-white/15 text-accent' : 'border-slate-200 text-ink'}`}>{action}<ChevronRight className="h-4 w-4" aria-hidden="true" /></Link></article>)}
          </div>
        </div>
      </section>

      <section className="bg-white py-20"><div className="mx-auto grid max-w-7xl gap-10 px-4 sm:px-6 lg:grid-cols-2 lg:px-8"><div><p className="text-xs font-bold uppercase tracking-[0.18em] text-yellow-800">The SwiftDrop way</p><h2 className="mt-3 text-3xl font-black tracking-tight text-ink sm:text-4xl">Every delivery deserves a better handoff.</h2></div><div className="grid gap-6 sm:grid-cols-3">{[['01', 'Book', 'Simple details and clear rates.'], ['02', 'Move', 'Every team sees the same status.'], ['03', 'Arrive', 'Proof, confirmation, done.']].map(([number, title, description]) => <div key={number} className="border-t-2 border-accent pt-4"><span className="font-mono text-xs font-bold text-yellow-800">{number}</span><h3 className="mt-5 font-black text-ink">{title}</h3><p className="mt-2 text-sm leading-6 text-slate-500">{description}</p></div>)}</div></div></section>

      <section className="bg-ink py-16 text-white"><div className="mx-auto flex max-w-7xl flex-col items-start justify-between gap-8 px-4 sm:px-6 md:flex-row md:items-center lg:px-8"><div><p className="text-xs font-bold uppercase tracking-[0.18em] text-accent">Ready to move?</p><h2 className="mt-3 text-3xl font-black tracking-tight sm:text-4xl">Let’s make delivery feel easy.</h2></div><div className="flex flex-col gap-3 sm:flex-row"><ActionLink to="/register" className="rounded-full px-7">Create an account <ArrowRight className="h-4 w-4" aria-hidden="true" /></ActionLink><ActionLink to="/login" variant="outline" className="rounded-full border-white/20 bg-transparent text-white hover:bg-white/10 hover:text-white">Sign in</ActionLink></div></div></section>
    </div>
  );
};
