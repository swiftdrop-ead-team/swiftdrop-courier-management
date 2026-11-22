import { afterEach, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { ThemeProvider } from '@mui/material/styles';
import theme from 'assets/theme';
import { FloatingAIAssistant } from '../components/ai/FloatingAIAssistant';
import { api } from '../services/api';

afterEach(() => vi.restoreAllMocks());
function open(preview = false) {
  render(<ThemeProvider theme={theme}><FloatingAIAssistant preview={preview} /></ThemeProvider>);
  const launcher = screen.getByRole('button', { name: 'Open AI Assistant' });
  fireEvent.click(launcher);
  return launcher;
}

it('focuses the message on opening and returns focus on Escape', () => {
  const launcher = open();
  expect(screen.getByRole('textbox', { name: 'Message' })).toHaveFocus();
  expect(launcher).toHaveAttribute('aria-expanded', 'true');
  fireEvent.keyDown(screen.getByRole('textbox', { name: 'Message' }), { key: 'Escape' });
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(launcher).toHaveFocus();
});

it('handles loading and a successful response without duplicate submissions', async () => {
  let finish!: (value: unknown) => void;
  const post = vi.spyOn(api, 'post').mockImplementation(() => new Promise(resolve => { finish = resolve; }) as any);
  open();
  const input = screen.getByRole('textbox', { name: 'Message' });
  fireEvent.change(input, { target: { value: 'Packing advice?' } });
  fireEvent.keyDown(input, { key: 'Enter' });
  fireEvent.submit(screen.getByRole('form', { name: 'Send an AI message' }));
  expect(post).toHaveBeenCalledTimes(1);
  expect(screen.getByRole('status', { name: 'Waiting for AI response' })).toBeInTheDocument();
  finish({ data: { response: 'Use a sturdy box.' } });
  await waitFor(() => expect(screen.getByText('Use a sturdy box.')).toBeInTheDocument());
  expect(input).toHaveFocus();
});

it('displays provider errors with a retry action', async () => {
  vi.spyOn(api, 'post').mockRejectedValue({ response: { status: 502 } });
  open();
  const input = screen.getByRole('textbox', { name: 'Message' });
  fireEvent.change(input, { target: { value: 'Help with delivery' } });
  fireEvent.keyDown(input, { key: 'Enter' });
  await waitFor(() => expect(screen.getByText(/unavailable right now/i)).toBeInTheDocument());
  expect(screen.getByRole('button', { name: /retry/i })).toBeEnabled();
});

it('keeps preview chat disabled without issuing API calls', () => {
  const post = vi.spyOn(api, 'post');
  open(true);
  expect(within(screen.getByRole('dialog')).getByRole('textbox', { name: 'Message' })).toBeDisabled();
  expect(post).not.toHaveBeenCalled();
});
