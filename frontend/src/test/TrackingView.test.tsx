import React from "react";
import { act, cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { ThemeProvider } from "@mui/material/styles";
import theme from "assets/theme";
import axios from "axios";
import { TrackingView } from "../features/delivery/TrackingView";

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  onmessage: ((event: { data: string }) => void) | null = null;
  close = vi.fn();
  constructor(public url: string) {
    FakeEventSource.instances.push(this);
  }
}
const record = (
  number = "SD-TEST-1001",
  status = "CREATED",
  updatedAt = "2026-11-13T04:00:00Z",
) => ({
  trackingNumber: number,
  status,
  updatedAt,
  history: [{ status, timestamp: updatedAt }],
});
const view = (number = "SD-TEST-1001") => (
  <ThemeProvider theme={theme}>
    <TrackingView initialNumber={number} />
  </ThemeProvider>
);
beforeEach(() => {
  vi.restoreAllMocks();
  FakeEventSource.instances = [];
  vi.stubGlobal("EventSource", FakeEventSource);
});
afterEach(() => {
  cleanup();
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

it("receives live status events, ignores older data, and closes completed streams", async () => {
  vi.spyOn(axios, "get").mockResolvedValue({ data: record() });
  const rendered = render(view());
  expect(await screen.findByText("SD-TEST-1001")).toBeInTheDocument();
  const source = FakeEventSource.instances[0];
  act(() => {
    source.onopen?.();
    source.onmessage?.({
      data: JSON.stringify(
        record("SD-TEST-1001", "IN_TRANSIT", "2026-11-13T06:00:00Z"),
      ),
    });
  });
  expect(screen.getAllByText("In transit")).toHaveLength(2);
  act(() => source.onmessage?.({ data: JSON.stringify(record()) }));
  expect(screen.getAllByText("In transit")).toHaveLength(2);
  act(() =>
    source.onmessage?.({
      data: JSON.stringify(
        record("SD-TEST-1001", "DELIVERED", "2026-11-13T08:00:00Z"),
      ),
    }),
  );
  expect(screen.getByText(/Delivery closed/)).toBeInTheDocument();
  expect(source.close).toHaveBeenCalled();
  rendered.unmount();
});

it("polls anonymously when streaming is unavailable and stops after a terminal state", async () => {
  vi.useFakeTimers();
  vi.stubGlobal("EventSource", undefined);
  const get = vi
    .spyOn(axios, "get")
    .mockResolvedValueOnce({ data: record() })
    .mockResolvedValue({
      data: record("SD-TEST-1001", "DELIVERED", "2026-11-13T08:00:00Z"),
    });
  const rendered = render(view());
  await act(async () => {
    await Promise.resolve();
  });
  expect(get).toHaveBeenCalledWith(
    expect.stringContaining("/tracking/SD-TEST-1001"),
    expect.objectContaining({
      withCredentials: false,
      signal: expect.any(AbortSignal),
    }),
  );
  await act(async () => {
    vi.advanceTimersByTime(10000);
  });
  expect(get).toHaveBeenCalledTimes(2);
  expect(screen.getByText(/Delivery closed/)).toBeInTheDocument();
  await act(async () => {
    vi.advanceTimersByTime(20000);
  });
  expect(get).toHaveBeenCalledTimes(2);
  rendered.unmount();
});

it("discards a late response after the tracking number changes", async () => {
  let resolveOld!: (value: unknown) => void;
  vi.spyOn(axios, "get")
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveOld = resolve;
        }),
    )
    .mockResolvedValue({ data: record("SD-TEST-2002") });
  const rendered = render(view());
  rendered.rerender(view("SD-TEST-2002"));
  expect(await screen.findByText("SD-TEST-2002")).toBeInTheDocument();
  await act(async () => resolveOld({ data: record() }));
  expect(screen.queryByText("SD-TEST-1001")).not.toBeInTheDocument();
  expect(screen.getByText("SD-TEST-2002")).toBeInTheDocument();
});

it("shows a useful not-found state without exposing raw server errors", async () => {
  vi.spyOn(axios, "get").mockRejectedValue({
    response: { status: 404, data: { message: "internal database details" } },
  });
  render(view());
  expect(
    await screen.findByText(
      "No parcel was found. Check your tracking number and try again.",
    ),
  ).toBeInTheDocument();
  expect(
    screen.queryByText("internal database details"),
  ).not.toBeInTheDocument();
});
