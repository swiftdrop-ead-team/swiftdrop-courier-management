/// <reference types="vitest" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
      components: path.resolve(__dirname, "./src/soft-ui/components"),
      examples: path.resolve(__dirname, "./src/soft-ui/examples"),
      assets: path.resolve(__dirname, "./src/soft-ui/assets"),
      context: path.resolve(__dirname, "./src/soft-ui/context"),
    },
  },
  server: {
    port: 3000,
    proxy: {
      "/api": {
        target: process.env.VITE_GATEWAY_URL || "http://localhost:8080",
        changeOrigin: true,
      },
      "/oauth2": {
        target: process.env.VITE_GATEWAY_URL || "http://localhost:8080",
        changeOrigin: true,
      },
      "/login/oauth2": {
        target: process.env.VITE_GATEWAY_URL || "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
  test: {
    // Coverage instrumentation and MUI transition-heavy workflows need more than
    // the 5s default on constrained Windows runners.
    testTimeout: 30000,
    globals: true,
    environment: "jsdom",
    setupFiles: "./src/test/setup.ts",
    coverage: {
      provider: "v8",
      reporter: ["text", "html", "json-summary", "lcov"],
      reportsDirectory: "./reports/coverage",
      include: ["src/**/*.{ts,tsx}"],
      exclude: ["src/test/**", "src/**/*.d.ts", "src/types/**"],
    },
  },
});
