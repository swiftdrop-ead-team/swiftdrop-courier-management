/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // ── Brand navy / deep blue ──────────────────────────────
        navy: {
          50:  '#eef4fb',
          100: '#d9e7f6',
          200: '#b8d2ee',
          300: '#8ab6e2',
          400: '#4c8ac9',
          500: '#276ab0',
          600: '#1b4d8c',
          700: '#153c70',
          800: '#11296B',
          900: '#00274C',
          950: '#001a35',
        },
        blue: {
          50:  '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#11296B',
          900: '#00274C',
          950: '#001a35',
        },

        // ── Brand yellow ────────────────────────────────────────
        gold: {
          50:  '#fffef0',
          100: '#fff9cc',
          200: '#fff199',
          300: '#ffe866',
          400: '#FFDB57',   // Soft Light Yellow
          500: '#FFCB05',   // Vibrant Golden Yellow
          600: '#d9a700',
          700: '#b28400',
          800: '#8c6504',
          900: '#6e4f02',
        },
        yellow: {
          50:  '#fffef0',
          100: '#fff9cc',
          200: '#fff199',
          300: '#ffe866',
          400: '#FFDB57',
          500: '#FFCB05',
          600: '#d9a700',
          700: '#b28400',
          800: '#8c6504',
          900: '#6e4f02',
        },
        teal: {
          50:  '#fffef0',
          100: '#fff9cc',
          200: '#fff199',
          300: '#ffe866',
          400: '#FFDB57',
          500: '#FFCB05',
          600: '#d9a700',
          700: '#b28400',
          800: '#8c6504',
          900: '#6e4f02',
          950: '#00274C',
        },

        // ── Ice-blue dashboard surface ───────────────────────────
        // A fresh, cool sky-blue used as the dashboard shell background
        iceblue: {
          50:  '#f0f7ff',   // near-white ice-blue
          100: '#e1effc',   // very light — main dashboard bg
          200: '#c8e1f8',   // slightly deeper — card hover tint
          300: '#a0c8f2',   // soft mid-blue
          400: '#6aaae8',
          500: '#3d87d6',
        },

        // ── Ultra-light grey surface similar to white (#f8fafc) ──
        surface: {
          50:  '#ffffff',
          100: '#f8fafc',
          200: '#f1f5f9',
          300: '#e2e8f0',
          400: '#cbd5e1',
          500: '#94a3b8',
        },
        slate: {
          50:  '#f8fafc',
          100: '#f1f5f9',
          200: '#e2e8f0',
          300: '#cbd5e1',
          400: '#94a3b8',
          500: '#64748B',
          600: '#475569',
          700: '#334155',
          800: '#1e293b',
          900: '#0f172a',
        },

        // ── Semantic shortcuts ───────────────────────────────────
        brand: {
          navy:        '#00274C',
          blue:        '#11296B',
          yellow:      '#FFCB05',
          yellowLight: '#FFDB57',
          surface:     '#EDEDED',
          bg:          '#EDEDED',
          iceblue:     '#e1effc',
        },
        ink: {
          DEFAULT: '#00274C',
          deep:    '#001a35',
          soft:    '#11296B',
        },
        accent: {
          DEFAULT: '#FFCB05',
          bright:  '#FFDB57',
          pale:    '#fff9cc',
        },
        canvas: '#EDEDED',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      animation: {
        'fade-in':  'fadeIn 0.3s ease-in-out',
        'slide-up': 'slideUp 0.4s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%':   { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%':   { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
}
