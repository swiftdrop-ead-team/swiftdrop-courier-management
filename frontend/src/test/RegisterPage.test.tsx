import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { RegisterPage } from '../pages/auth/RegisterPage';
import { AuthProvider } from '../hooks/useAuth';

const renderRegisterPage = () => {
  return render(
    <BrowserRouter>
      <AuthProvider>
        <RegisterPage />
      </AuthProvider>
    </BrowserRouter>
  );
};

describe('RegisterPage Component', () => {
  it('renders customer registration title and inputs', () => {
    renderRegisterPage();

    expect(screen.getByText('Create Customer Account')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Jane Doe')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('jane@example.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('+94771234567')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /complete customer registration/i })).toBeInTheDocument();
  });

  it('contains mandatory terms and conditions acceptance checkbox', () => {
    renderRegisterPage();

    const termsCheckbox = screen.getByRole('checkbox');
    expect(termsCheckbox).toBeInTheDocument();
    expect(termsCheckbox).not.toBeChecked();
  });
});
