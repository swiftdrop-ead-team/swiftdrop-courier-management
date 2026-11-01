# SwiftDrop — React Frontend Application

## Assigned Member: Member 1
**Branch**: `feature/frontend`  
**Port**: `3000` (Local Dev) / `80` (Docker Container)  
**Technology Stack**: React 18, TypeScript, Vite 6, Tailwind CSS, Lucide React, React Router 6, React Hook Form, Zod, TanStack Query, Axios

---

## 1. Visual Identity & Design System
- **Midnight Navy**: `#10243A` (`navy-900`)
- **Premium Teal**: `#0D9488` (`teal-600`)
- **Soft Background**: `#F8FAFC`
- **Slate Gray**: `#64748B`
- **Accent Blue**: `#2563EB`
- **Tagline**: *"Delivering trust, every mile."*

---

## 2. Implemented Pages & Routes

| Route | Access | Component | Purpose |
|:---|:---|:---|:---|
| `/` | Public | `LandingPage` | Platform overview, track parcel search, customer & merchant CTAs |
| `/login` | Guest Only | `LoginPage` | Split-screen desktop login, Google OAuth button, quick demo credentials |
| `/register` | Guest Only | `RegisterPage` | Customer registration with validation & terms agreement |
| `/merchant/register` | Guest Only | `MerchantRegisterPage` | Business registration with PENDING compliance approval state |
| `/forgot-password` | Guest Only | `ForgotPasswordPage` | Request expiring password reset instructions |
| `/reset-password` | Guest Only | `ResetPasswordPage` | Reset password using verified token |
| `/auth/callback` | OAuth Flow | `AuthCallbackPage` | Google OAuth code flow callback & JWT session hydration |
| `/customer/dashboard` | `CUSTOMER` | `CustomerDashboard` | Waybill creation, tracking overview, recent deliveries |
| `/merchant/dashboard` | `MERCHANT` | `MerchantDashboard` | Business profile, approval status indicator, API integration keys |
| `/courier/dashboard` | `COURIER` | `CourierDashboard` | Online/Offline toggle, route waypoint manifest |
| `/admin/dashboard` | `ADMIN` | `AdminDashboard` | User directory, live Approve/Reject actions for pending merchants |

---

## 3. Running Locally

Install dependencies:
```bash
npm install
```

Start Vite development server:
```bash
npm run dev
```
Open `http://localhost:3000`.

Run automated tests:
```bash
npm test
```

Build production bundle:
```bash
npm run build
```
