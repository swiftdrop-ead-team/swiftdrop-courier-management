import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { LoginPage } from '../pages/auth/LoginPage';
import { AuthProvider } from '../hooks/useAuth';

const renderLoginPage = () => {
  return render(
    <BrowserRouter>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </BrowserRouter>
  );
};

describe('LoginPage Component', () => {
  it('renders login heading and essential inputs', () => {
    renderLoginPage();

    expect(screen.getByText('Welcome back')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('name@company.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in to account/i })).toBeInTheDocument();
  });

  it('renders Continue with Google OAuth button', () => {
    renderLoginPage();

    expect(screen.getByText(/continue with google/i)).toBeInTheDocument();
  });

  it('renders evaluator quick fill credential buttons', () => {
    renderLoginPage();

    expect(screen.getByText(/admin role/i)).toBeInTheDocument();
    expect(screen.getByText(/customer role/i)).toBeInTheDocument();
    expect(screen.getByText(/merchant \(approved\)/i)).toBeInTheDocument();
    expect(screen.getByText(/courier role/i)).toBeInTheDocument();
  });

  it('toggles password visibility when eye icon clicked', () => {
    renderLoginPage();

    const passwordInput = screen.getByPlaceholderText('••••••••') as HTMLInputElement;
    expect(passwordInput.type).toBe('password');

    const toggleButton = passwordInput.parentElement?.querySelector('button');
    if (toggleButton) {
      fireEvent.click(toggleButton);
      expect(passwordInput.type).toBe('text');

      fireEvent.click(toggleButton);
      expect(passwordInput.type).toBe('password');
    }
  });
});
