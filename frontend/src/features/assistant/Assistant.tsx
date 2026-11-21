import { useEffect, useRef, useState } from "react";
import { Alert, Box, Tooltip } from "@mui/material";
import { alpha, useTheme } from "@mui/material/styles";
import { Bot, RefreshCw, Send, UserRound } from "lucide-react";
import { ActionButton, Field, Panel, WorkspaceText as Text } from "@/features/delivery/components";
import { askAssistant, assistantError } from "@/services/assistantService";

type Message = { id: number; role: "user" | "assistant"; text: string; failed?: boolean };
type Failure = { id: number; text: string; error: string };
const prompts = [
  "How should I pack a fragile parcel?",
  "What should I check before booking a delivery?",
  "What does out for delivery mean?",
];

export function Assistant({ preview = false }: { preview?: boolean }) {
  const { palette } = useTheme();
  const [messages, setMessages] = useState<Message[]>([]);
  const [draft, setDraft] = useState("");
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<Failure | null>(null);
  const sequence = useRef(0);
  const locked = useRef(false);
  const mounted = useRef(true);
  const request = useRef<AbortController>();
  const scroll = useRef<HTMLDivElement>(null);
  const input = useRef<HTMLInputElement | HTMLTextAreaElement>(null);

  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; request.current?.abort(); };
  }, []);
  useEffect(() => {
    if (scroll.current) scroll.current.scrollTop = scroll.current.scrollHeight;
  }, [messages, busy]);

  useEffect(() => {
    if (!busy && request.current) input.current?.focus();
  }, [busy]);

  const send = async (retry?: Failure) => {
    const text = (retry?.text ?? draft).trim();
    if (!text || locked.current || preview) return;
    locked.current = true;
    setBusy(true); setFailure(null);
    const id = retry?.id ?? ++sequence.current;
    if (retry) setMessages(previous => previous.map(message => message.id === id ? { ...message, failed: false } : message));
    else { setMessages(previous => [...previous, { id, role: "user", text }]); setDraft(""); }
    const controller = new AbortController();
    request.current = controller;
    try {
      const reply = await askAssistant(text, controller.signal);
      if (mounted.current) setMessages(previous => [...previous, { id: ++sequence.current, role: "assistant", text: reply }]);
    } catch (error) {
      if (mounted.current && !controller.signal.aborted) {
        setMessages(previous => previous.map(message => message.id === id ? { ...message, failed: true } : message));
        setFailure({ id, text, error: assistantError(error) });
      }
    } finally {
      locked.current = false;
      if (mounted.current) setBusy(false);
    }
  };

  return <Panel title="Your conversation" subtitle="General delivery guidance. Use Tracking for live shipment details.">
    {preview && <Alert severity="info" sx={{ mb: 2 }}>AI replies are unavailable in dashboard previews. Sign in to use the live assistant.</Alert>}
    <Box ref={scroll} role="log" aria-label="AI conversation" aria-live="polite" aria-relevant="additions text" aria-atomic={false}
      sx={{ height: { xs: 360, sm: 420, lg: 480 }, overflowY: "auto", minWidth: 0, p: { xs: 1.5, sm: 2.5 },
        bgcolor: "brand.surface", border: "1px solid", borderColor: "brand.border", borderRadius: "var(--ui-radius-card)",
        display: "flex", flexDirection: "column", gap: 2 }}>
      {!messages.length && <Box sx={{ m: "auto", width: "100%", maxWidth: 620, textAlign: "center", py: 2 }}>
        <Box sx={{ display: "grid", placeItems: "center", width: 52, height: 52, mx: "auto", mb: 2,
          borderRadius: 3, bgcolor: alpha(palette.primary.main, 0.16), color: "brand.navy" }}><Bot size={26} aria-hidden="true" /></Box>
        <Text component="h3" variant="h6" fontWeight={700}>How can I help with your delivery questions?</Text>
        <Text variant="body2" color="text.secondary" sx={{ mt: 1, mb: 2 }}>Ask about packing, delivery stages, or preparing a shipment.</Text>
        <Box sx={{ display: "grid", gap: 1 }}>
          {prompts.map(prompt => <ActionButton key={prompt} disabled={preview} onClick={() => { setDraft(prompt); input.current?.focus(); }}
            sx={{ width: "100%", textAlign: "left", justifyContent: "flex-start", whiteSpace: "normal", bgcolor: "white" }}>{prompt}</ActionButton>)}
        </Box>
      </Box>}
      {messages.map(message => <Box component="article" key={message.id} aria-label={message.role === "user" ? "Your message" : "AI Assistant message"}
        sx={{ display: "flex", gap: 1, flexDirection: message.role === "user" ? "row-reverse" : "row", alignSelf: message.role === "user" ? "flex-end" : "flex-start", maxWidth: { xs: "100%", sm: "88%" }, minWidth: 0 }}>
        <Box aria-hidden="true" sx={{ flexShrink: 0, mt: 0.5, width: 30, height: 30, borderRadius: 2, display: "grid", placeItems: "center", bgcolor: "white", color: "brand.navy" }}>
          {message.role === "user" ? <UserRound size={16} /> : <Bot size={18} />}
        </Box>
        <Box sx={{ minWidth: 0, p: { xs: 1.5, sm: 2 }, border: "1px solid", borderColor: "brand.border", borderRadius: "var(--ui-radius-card)",
          bgcolor: message.role === "user" ? alpha(palette.primary.main, 0.15) : "white", boxShadow: "var(--ui-shadow-control)" }}>
          <Text variant="caption" fontWeight={700} display="block" mb={0.5}>{message.role === "user" ? "You" : "SwiftDrop AI"}</Text>
          <Text variant="body2" sx={{ whiteSpace: "pre-wrap", overflowWrap: "anywhere", lineHeight: 1.65 }}>{message.text}</Text>
          {message.failed && <Text variant="caption" color="text.secondary" display="block" mt={1}>No reply received</Text>}
        </Box>
      </Box>)}
      {busy && <Box role="status" aria-label="Waiting for AI response" sx={{ display: "flex", alignItems: "center", gap: 1, color: "brand.textMuted", px: 1,
        "@keyframes assistantPulse": { "0%, 80%, 100%": { opacity: 0.3 }, "40%": { opacity: 1 } },
        "@media (prefers-reduced-motion: reduce)": { "& .assistant-dot": { animation: "none" } } }}>
        <Bot size={18} aria-hidden="true" /><Text variant="body2">Getting a reply</Text>
        {[0, 1, 2].map(index => <Box key={index} component="span" className="assistant-dot" aria-hidden="true" sx={{ width: 5, height: 5, borderRadius: "50%", bgcolor: "currentColor", animation: "assistantPulse 1.2s infinite", animationDelay: `${index * 0.15}s` }} />)}
      </Box>}
    </Box>
    {failure && <Alert severity="error" sx={{ mt: 2 }} action={<ActionButton size="small" disabled={busy || preview} startIcon={<RefreshCw size={14} />} onClick={() => void send(failure)}>Retry</ActionButton>}>{failure.error}</Alert>}
    <Box component="form" onSubmit={event => { event.preventDefault(); void send(); }} aria-label="Send an AI message" sx={{ mt: 1 }}>
      <Field label="Message" placeholder="Ask a delivery question..." multiline minRows={2} maxRows={6} inputRef={input} value={draft} disabled={busy || preview}
        onChange={event => setDraft(event.target.value)}
        onKeyDown={event => { if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) { event.preventDefault(); void send(); } }} />
      <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", justifyContent: "space-between", gap: 1.5 }}>
        <Text variant="caption" color="text.secondary" sx={{ flex: "1 1 220px" }}>Messages are sent individually. This page keeps only a temporary transcript.</Text>
        <Tooltip title="Send message. Enter to send; Shift+Enter for a new line."><span>
          <ActionButton type="submit" color="primary" variant="contained" startIcon={<Send size={16} />} disabled={!draft.trim() || busy || preview} aria-label="Send message">
            {busy ? "Waiting for reply" : "Send message"}
          </ActionButton>
        </span></Tooltip>
      </Box>
    </Box>
  </Panel>;
}
