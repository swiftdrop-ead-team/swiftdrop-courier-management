import React from 'react';
import { Link } from 'react-router-dom';
const roles = ['admin', 'merchant', 'courier', 'customer'];
export const DashboardPreviewPortal = () => (
  <main className="min-h-screen bg-slate-50 px-6 py-16 text-navy-900">
    <div className="mx-auto max-w-4xl">
      <p className="mb-3 text-sm font-semibold text-blue-600">SwiftDrop · Temporary preview</p>
      <h1 className="text-4xl font-bold">Dashboard portal</h1>
      <p className="mt-4 text-slate-500">Explore the frontend without signing in. Delivery previews share sample bookings and updates in this browser tab.</p>
      <p className="mt-3 text-sm text-slate-500">Try Customer → Send a parcel, Admin → Dispatch & workloads, then Courier → Assigned Deliveries. Admin account approvals use a separate sample directory.</p>
      <Link to="/tracking-preview/SD-DEMO-1001" className="mt-4 inline-block text-sm font-semibold text-blue-700">Try public tracking preview →</Link>
      <div className="mt-10 grid gap-5 sm:grid-cols-2">
        {roles.map((role) => <Link key={role} to={role === 'customer' ? '/customer/dashboard-preview/overview' : `/dashboard-preview/${role}`} className="rounded-2xl border border-slate-200 bg-white p-7 shadow-sm transition hover:border-blue-400 hover:shadow-md">
          <h2 className="text-xl font-bold capitalize">{role} dashboard</h2>
          <p className="mt-3 text-sm text-blue-600">Open preview →</p>
        </Link>)}
      </div>
      <Link to="/" className="mt-8 inline-block text-sm text-slate-500">Back to home</Link>
    </div>
  </main>
);
