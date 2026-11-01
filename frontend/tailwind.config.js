/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Deep blue tones from palette (#00274C, #11296B) replacing dark neutrals
        navy: {
          50: '#eef4fb',
          100: '#d9e7f6',
          200: '#b8d2ee',
          300: '#8ab6e2',
          400: '#4c8ac9',
          500: '#276ab0',
          600: '#1b4d8c',
          700: '#153c70',
          800: '#11296B', // Palette Deep Blue (#11296B)
          900: '#00274C', // Palette Midnight Navy (#00274C)
          950: '#001a35',
        },
        blue: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#11296B', // Palette Deep Blue (#11296B)
          900: '#00274C', // Palette Midnight Navy (#00274C)
          950: '#001a35',
        },
        // Yellow tones from palette (#FFDB57, #FFCB05) replacing green/teal colors
        gold: {
          50: '#fffef0',
          100: '#fff9cc',
          200: '#fff199',
          300: '#ffe866',
          400: '#FFDB57', // Palette Soft Light Yellow (#FFDB57)
          500: '#FFCB05', // Palette Vibrant Golden Yellow (#FFCB05)
          600: '#d9a700',
          700: '#b28400',
          800: '#8c6504',
          900: '#6e4f02',
        },
        yellow: {
          50: '#fffef0',
          100: '#fff9cc',
          200: '#fff199',
          300: '#ffe866',
          400: '#FFDB57', // Palette Soft Light Yellow (#FFDB57)
          500: '#FFCB05', // Palette Vibrant Golden Yellow (#FFCB05)
          600: '#d9a700',
          700: '#b28400',
          800: '#8c6504',
          900: '#6e4f02',
        },
        // Teal mapped to yellow palette for complete style alignment
        teal: {
          50: '#fffef0',
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
        // Neutral light gray surface from palette (#EDEDED)
        surface: {
          50: '#fcfcfc',
          100: '#f7f7f7',
          200: '#EDEDED', // Palette Light Gray (#EDEDED)
          300: '#dfdfdf',
          400: '#cccccc',
          500: '#9e9e9e',
        },
        slate: {
          50: '#f8fafc',
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
        brand: {
          navy: '#00274C',
          blue: '#11296B',
          yellow: '#FFCB05',
          yellowLight: '#FFDB57',
          surface: '#EDEDED',
          bg: '#EDEDED',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-in-out',
        'slide-up': 'slideUp 0.4s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
}
