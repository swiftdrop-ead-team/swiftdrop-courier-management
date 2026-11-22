import { api } from "./api";

/** The backend accepts a message and a bounded recent transcript. */
export type AssistantHistoryMessage = { role: "user" | "assistant"; text: string };

export async function askAssistant(
  message: string,
  signal?: AbortSignal,
  history: AssistantHistoryMessage[] = [],
): Promise<string> {
  const trimmed = message.trim();
  if (!trimmed) throw new Error("Enter a message before sending.");
  const payload: { message: string; history?: AssistantHistoryMessage[] } = { message: trimmed };
  if (history.length) payload.history = history.slice(-8);
  const { data } = await api.post<{ response?: unknown }>("/assistant/chat", payload, { signal });
  if (typeof data?.response !== "string" || !data.response.trim()) {
    throw new Error("The assistant returned an empty response. Please try again.");
  }
  return data.response.trim();
}

export function assistantError(error: unknown): string {
  const failure = error as { code?: string; response?: { status?: number } };
  switch (failure?.response?.status) {
    case 401: return "Your session has expired. Sign in again to use the assistant.";
    case 403: return "Your account does not have access to the AI Assistant.";
    case 429: return "Too many requests. Wait a moment before trying again.";
    case 502:
    case 503: return "The AI Assistant is unavailable right now. Please try again later.";
  }
  if ((failure?.response?.status || 0) >= 500) return "The AI Assistant is unavailable right now. Please try again later.";
  if (failure?.code === "ECONNABORTED") return "The reply took too long. Please try again.";
  if (failure?.code === "ERR_NETWORK") return "Unable to reach the AI Assistant. Check your connection and try again.";
  return error instanceof Error ? error.message : "Unable to get a reply. Please try again.";
}
