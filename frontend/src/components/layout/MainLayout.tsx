import React from 'react';
import { Outlet } from 'react-router-dom';
import { Navbar } from '../ui/Navbar';
import { Footer } from '../ui/Footer';

export const MainLayout: React.FC = () => {
  return (
    <div className="min-h-screen flex flex-col bg-[#EDEDED]">
      <Navbar />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
};

