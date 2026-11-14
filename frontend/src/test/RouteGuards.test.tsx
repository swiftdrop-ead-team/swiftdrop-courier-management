import React from "react";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, expect, it, vi } from "vitest";
import { ProtectedRoute, RoleRoute } from "../routes/RouteGuards";
const auth = vi.hoisted(() => ({
  isAuthenticated: false,
  isLoading: false,
  user: { id: "courier-1" },
  hasRole: vi.fn(() => false),
  getDefaultDashboardRoute: vi.fn(() => "/courier/dashboard"),
}));
vi.mock("../hooks/useAuth", () => ({ useAuth: () => auth }));
beforeEach(() => {
  auth.isAuthenticated = false;
  auth.isLoading = false;
  auth.hasRole.mockReturnValue(false);
});
function app() {
  return render(
    <MemoryRouter initialEntries={["/admin"]}>
      <Routes>
        <Route path="/login" element={<h1>Sign in</h1>} />
        <Route path="/courier/dashboard" element={<h1>Courier home</h1>} />
        <Route element={<ProtectedRoute />}>
          <Route element={<RoleRoute requiredRole="ADMIN" />}>
            <Route path="/admin" element={<h1>Admin tools</h1>} />
          </Route>
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}
it("redirects unauthenticated visitors to sign in", () => {
  app();
  expect(screen.getByRole("heading", { name: "Sign in" })).toBeInTheDocument();
});
it("blocks a courier from administrator tools", () => {
  auth.isAuthenticated = true;
  app();
  expect(
    screen.getByRole("heading", { name: "Courier home" }),
  ).toBeInTheDocument();
  expect(screen.queryByText("Admin tools")).not.toBeInTheDocument();
});
it("allows the administrator role to enter administrator tools", () => {
  auth.isAuthenticated = true;
  auth.hasRole.mockReturnValue(true);
  app();
  expect(
    screen.getByRole("heading", { name: "Admin tools" }),
  ).toBeInTheDocument();
});
