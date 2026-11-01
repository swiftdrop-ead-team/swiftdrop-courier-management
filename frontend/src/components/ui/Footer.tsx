import React from 'react';
import { Logo } from './Logo';
import { ShieldCheck, Clock } from 'lucide-react';
import { Link } from 'react-router-dom';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-navy-900 text-white pt-16 pb-12 border-t border-navy-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10 pb-12 border-b border-navy-800">
          {/* Col 1: Brand & Tagline */}
          <div className="space-y-4">
            <Logo variant="light" size="lg" />
            <p className="text-sm text-slate-300 leading-relaxed">
              Delivering trust, every mile. Next-generation microservices parcel logistics platform built for modern commerce.
            </p>
            <div className="flex items-center gap-3 text-xs text-yellow-400 font-medium">
              <span className="flex items-center gap-1">
                <ShieldCheck className="w-4 h-4 text-yellow-400" /> Enterprise Security
              </span>
              <span>•</span>
              <span className="flex items-center gap-1">
                <Clock className="w-4 h-4 text-yellow-400" /> 99.9% On-Time
              </span>
            </div>
          </div>

          {/* Col 2: Platform Links */}
          <div>
            <h4 className="text-sm font-semibold tracking-wider uppercase text-yellow-400 mb-4">
              Platform
            </h4>
            <ul className="space-y-2.5 text-sm text-slate-300">
              <li><Link to="/" className="hover:text-yellow-400 transition-colors">Parcel Tracking</Link></li>
              <li><Link to="/register" className="hover:text-yellow-400 transition-colors">Individual Shipping</Link></li>
              <li><Link to="/merchant/register" className="hover:text-yellow-400 transition-colors">Merchant Solutions</Link></li>
              <li><a href="#services" className="hover:text-yellow-400 transition-colors">Rate Calculator</a></li>
            </ul>
          </div>

          {/* Col 3: Microservices Team */}
          <div>
            <h4 className="text-sm font-semibold tracking-wider uppercase text-yellow-400 mb-4">
              Architecture
            </h4>
            <ul className="space-y-2.5 text-sm text-slate-300">
              <li><span className="text-slate-400">Spring Cloud Gateway</span> (8080)</li>
              <li><span className="text-slate-400">Identity Service</span> (8081)</li>
              <li><span className="text-slate-400">PostgreSQL Persistence</span></li>
              <li><span className="text-slate-400">Google OAuth 2.0</span></li>
              <li><span className="text-slate-400">Dual-Token JWT Security</span></li>
            </ul>
          </div>

          {/* Col 4: University Project Notice */}
          <div>
            <h4 className="text-sm font-semibold tracking-wider uppercase text-yellow-400 mb-4">
              Project Information
            </h4>
            <p className="text-xs text-slate-300 leading-relaxed mb-3">
              SwiftDrop is developed as an Enterprise Application Development group assignment by Team 10.
            </p>
            <div className="p-3 rounded-lg bg-navy-800 border border-navy-700 text-xs text-slate-300">
              <p className="font-semibold text-white">Repository:</p>
              <a 
                href="https://github.com/swiftdrop-ead-team/swiftdrop-courier-management" 
                target="_blank" 
                rel="noreferrer"
                className="text-yellow-400 hover:text-yellow-300 hover:underline break-all"
              >
                swiftdrop-ead-team/swiftdrop-courier-management
              </a>
            </div>
          </div>
        </div>

        {/* Bottom bar */}
        <div className="pt-8 flex flex-col sm:flex-row justify-between items-center text-xs text-slate-400 gap-4">
          <p>© {new Date().getFullYear()} SwiftDrop Logistics System. All rights reserved.</p>
          <p className="flex items-center gap-1">
            Built with Spring Boot 3.4, React, and PostgreSQL.
          </p>
        </div>
      </div>
    </footer>
  );
};
