import { beforeEach, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { ThemeProvider } from "@mui/material/styles";
import theme from "assets/theme";
import { Assistant } from "@/features/assistant/Assistant";
import { api } from "@/services/api";
import { askAssistant } from "@/services/assistantService";

vi.mock("../hooks/useAuth", () => ({ useAuth: () => ({ user: {
  id: "assistant-user", fullName: "Test User", roles: ["ADMIN", "CUSTOMER", "MERCHANT", "COURIER"], merchantApprovalStatus: "APPROVED",
} }) }));
beforeEach(() => vi.restoreAllMocks());
function open(preview = false) { return render(<ThemeProvider theme={theme}><Assistant preview={preview} /></ThemeProvider>); }
function enter(text: string) { fireEvent.change(screen.getByLabelText("Message"), { target: { value: text } }); }

it("shows a welcome state and fills a suggested prompt without submitting it", () => {
  const post = vi.spyOn(api, "post"); open();
  expect(screen.getByRole("button", { name: "Send message" })).toBeDisabled();
  fireEvent.click(screen.getByRole("button", { name: "How should I pack a fragile parcel?" }));
  expect(screen.getByLabelText("Message")).toHaveValue("How should I pack a fragile parcel?");
  expect(post).not.toHaveBeenCalled();
});
it("sends the exact API payload, prevents duplicate requests, and displays returned text safely", async () => {
  let resolve!: (value: unknown) => void;
  const post = vi.spyOn(api, "post").mockImplementation(() => new Promise(done => { resolve = done; }) as any);
  open(); enter("  Packing advice?  ");
  fireEvent.keyDown(screen.getByLabelText("Message"), { key: "Enter", code: "Enter" });
  expect(screen.getByRole("status", { name: "Waiting for AI response" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Send message" })).toBeDisabled();
  fireEvent.submit(screen.getByRole("form", { name: "Send an AI message" }));
  expect(post).toHaveBeenCalledTimes(1);
  expect(post).toHaveBeenCalledWith("/assistant/chat", { message: "Packing advice?" }, { signal: expect.any(AbortSignal) });
  // HTTP fixture verifies rendering only; live/provider success is checked separately.
  await act(async () => resolve({ data: { response: "Reply from the API\n<script>plain text</script>" } }));
  const reply = await screen.findByRole("article", { name: "AI Assistant message" });
  expect(reply).toHaveTextContent("Reply from the API"); expect(reply.querySelector("script")).toBeNull();
  expect(screen.queryByRole("status", { name: "Waiting for AI response" })).not.toBeInTheDocument();
});
it("keeps Shift+Enter for new lines and rejects whitespace-only input", () => {
  const post = vi.spyOn(api, "post"); open(); enter(" ");
  fireEvent.submit(screen.getByRole("form", { name: "Send an AI message" }));
  enter("First line"); fireEvent.keyDown(screen.getByLabelText("Message"), { key: "Enter", shiftKey: true });
  expect(post).not.toHaveBeenCalled();
});
it("shows service-unavailable errors and manually retries without duplicating the user message", async () => {
  const post = vi.spyOn(api, "post").mockRejectedValueOnce({ response: { status: 503 } }).mockResolvedValueOnce({ data: { response: "Returned retry response" } });
  open(); enter("Delivery question"); fireEvent.click(screen.getByRole("button", { name: "Send message" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("AI Assistant is unavailable");
  expect(screen.queryByRole("article", { name: "AI Assistant message" })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Retry" }));
  expect(await screen.findByRole("article", { name: "AI Assistant message" })).toHaveTextContent("Returned retry response");
  expect(screen.getAllByRole("article", { name: "Your message" })).toHaveLength(1);
  expect(post).toHaveBeenCalledTimes(2);
});
it.each([[401, "session has expired"], [403, "does not have access"], [429, "Too many requests"]])("explains HTTP %s without inventing a reply", async (status, text) => {
  vi.spyOn(api, "post").mockRejectedValue({ response: { status } }); open(); enter("A question");
  fireEvent.click(screen.getByRole("button", { name: "Send message" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(String(text));
  expect(screen.queryByRole("article", { name: "AI Assistant message" })).not.toBeInTheDocument();
});
it.each([{}, { response: "  " }, { response: 123 }])("treats an invalid or empty backend response as an error: %j", async data => {
  vi.spyOn(api, "post").mockResolvedValue({ data }); open(); enter("A question");
  fireEvent.click(screen.getByRole("button", { name: "Send message" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("empty response");
  expect(screen.queryByRole("article", { name: "AI Assistant message" })).not.toBeInTheDocument();
});
it("aborts an outstanding request when leaving the page", () => {
  const post = vi.spyOn(api, "post").mockImplementation(() => new Promise(() => {}) as any);
  const page = open(); enter("A question"); fireEvent.click(screen.getByRole("button", { name: "Send message" }));
  const signal = post.mock.calls[0][2]?.signal as AbortSignal; page.unmount(); expect(signal.aborted).toBe(true);
});
it("does not send or simulate replies in dashboard previews", () => {
  const post = vi.spyOn(api, "post"); open(true);
  expect(screen.getByRole("alert")).toHaveTextContent("unavailable in dashboard previews");
  expect(screen.getByRole("button", { name: "Send message" })).toBeDisabled(); expect(post).not.toHaveBeenCalled();
});
it("rejects blank messages in the adapter before calling the API", async () => {
  const post = vi.spyOn(api, "post"); await expect(askAssistant("  ")).rejects.toThrow("Enter a message"); expect(post).not.toHaveBeenCalled();
});
