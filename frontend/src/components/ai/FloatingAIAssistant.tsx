import { useEffect, useRef, useState, useCallback } from "react";
import { Box, IconButton, Tooltip, Typography, alpha } from "@mui/material";
import { X, Minimize2, Send, Bot, Sparkles, UserRound } from "lucide-react";
import { askAssistant, assistantError } from "@/services/assistantService";

/* ================================================================
   Types
   ================================================================ */

type Message = { id: number; role: "user" | "assistant"; text: string; failed?: boolean };
type Failure = { id: number; text: string; error: string };

/* ================================================================
   Floating AI Assistant Widget
   ================================================================ */

export function FloatingAIAssistant({ preview = false }: { preview?: boolean }) {
  const [open, setOpen] = useState(false);
  const [minimized, setMinimized] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [draft, setDraft] = useState("");
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<Failure | null>(null);

  const sequence = useRef(0);
  const locked = useRef(false);
  const mounted = useRef(true);
  const request = useRef<AbortController>();
  const scrollRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement | HTMLTextAreaElement>(null);

  // Cleanup on unmount
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      request.current?.abort();
    };
  }, []);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, busy]);

  // Focus input when not busy
  useEffect(() => {
    if (!busy && open && !minimized && request.current) {
      inputRef.current?.focus();
    }
  }, [busy, open, minimized]);

  const send = useCallback(async (retry?: Failure) => {
    const text = (retry?.text ?? draft).trim();
    if (!text || locked.current || preview) return;
    locked.current = true;
    setBusy(true);
    setFailure(null);
    const id = retry?.id ?? ++sequence.current;

    if (retry) {
      setMessages(previous =>
        previous.map(message => message.id === id ? { ...message, failed: false } : message)
      );
    } else {
      setMessages(previous => [...previous, { id, role: "user", text }]);
      setDraft("");
    }

    const controller = new AbortController();
    request.current = controller;

    try {
      const reply = await askAssistant(text, controller.signal);
      if (mounted.current) {
        setMessages(previous => [
          ...previous,
          { id: ++sequence.current, role: "assistant", text: reply },
        ]);
      }
    } catch (error) {
      if (mounted.current && !controller.signal.aborted) {
        setMessages(previous =>
          previous.map(message => message.id === id ? { ...message, failed: true } : message)
        );
        setFailure({ id, text, error: assistantError(error) });
      }
    } finally {
      locked.current = false;
      if (mounted.current) setBusy(false);
    }
  }, [draft, preview]);

  const handleKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault();
      void send();
    }
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    void send();
  };

  const toggleOpen = () => {
    if (!open) {
      setOpen(true);
      setMinimized(false);
    } else if (!minimized) {
      setMinimized(true);
    } else {
      setMinimized(false);
    }
  };

  const handleClose = () => {
    setOpen(false);
    setMinimized(false);
  };

  const handleRestore = () => {
    setMinimized(false);
  };

  return (
    <>
      {/* Floating Action Button */}
      <Tooltip title={open ? "Close AI Assistant" : "Ask AI Assistant"} placement="left">
        <Box
          component="button"
          aria-label={open ? "Close AI Assistant" : "Open AI Assistant"}
          onClick={toggleOpen}
          sx={{
            position: "fixed",
            bottom: { xs: 20, sm: 24 },
            right: { xs: 16, sm: 24 },
            zIndex: 1200,
            width: 56,
            height: 56,
            borderRadius: "50%",
            display: "grid",
            placeItems: "center",
            background: "linear-gradient(135deg, #ffcb05 0%, #ffdb57 100%)",
            color: "#00274c",
            boxShadow: "0 4px 16px rgba(255, 203, 5, 0.35), 0 2px 6px rgba(0, 0, 0, 0.1)",
            cursor: "pointer",
            border: "none",
            transition: "transform 200ms ease, box-shadow 200ms ease, opacity 200ms ease",
            transform: open && !minimized ? "rotate(90deg)" : "rotate(0deg)",
            "&:hover": {
              boxShadow: "0 6px 24px rgba(255, 203, 5, 0.5), 0 2px 8px rgba(0, 0, 0, 0.12)",
              transform: open && !minimized ? "rotate(90deg) scale(1.08)" : "scale(1.08)",
            },
            "&:active": {
              transform: open && !minimized ? "rotate(90deg) scale(0.95)" : "scale(0.95)",
            },
            "@keyframes assistantFloat": {
              "0%, 100%": { transform: "translateY(0)" },
              "50%": { transform: "translateY(-4px)" },
            },
            "@media (prefers-reduced-motion: reduce)": {
              "&:not(:hover)": { animation: "none" },
            },
          }}
        >
          {open && !minimized ? (
            <X size={24} />
          ) : (
            <Box sx={{ position: "relative" }}>
              <Bot size={26} />
              <Box
                sx={{
                  position: "absolute",
                  top: -2,
                  right: -4,
                  width: 10,
                  height: 10,
                  borderRadius: "50%",
                  bgcolor: "#16a34a",
                  border: "2px solid #ffcb05",
                }}
              />
            </Box>
          )}
        </Box>
      </Tooltip>

      {/* Chat Panel */}
      {open && (
        <Box
          sx={{
            position: "fixed",
            bottom: { xs: 84, sm: 88 },
            right: { xs: 16, sm: 24 },
            top: { xs: 64, sm: "auto" },
            width: { xs: "calc(100vw - 32px)", sm: 400 },
            maxWidth: 420,
            height: { xs: "calc(100vh - 100px)", sm: 560 },
            maxHeight: { xs: "calc(100vh - 100px)", sm: 620 },
            zIndex: 1199,
            display: "flex",
            flexDirection: "column",
            bgcolor: "#ffffff",
            borderRadius: "16px",
            boxShadow: "0 20px 60px rgba(0, 39, 76, 0.18), 0 4px 16px rgba(0, 0, 0, 0.08)",
            border: "1px solid #e2e8f0",
            overflow: "hidden",
            transition: minimized
              ? "height 250ms ease, width 250ms ease, opacity 200ms ease"
              : "height 300ms ease, width 300ms ease, opacity 250ms ease",
            ...(minimized
              ? {
                  height: 56,
                  width: { xs: "calc(100vw - 32px)", sm: 400 },
                  maxHeight: 56,
                  cursor: "pointer",
                }
              : {}),
            "@keyframes chatSlideUp": {
              "0%": { opacity: 0, transform: "translateY(16px) scale(0.96)" },
              "100%": { opacity: 1, transform: "translateY(0) scale(1)" },
            },
            "@media (prefers-reduced-motion: reduce)": {
              animation: "none !important",
            },
          }}
          onClick={minimized ? handleRestore : undefined}
          role="dialog"
          aria-label="AI Assistant chat"
        >
          {/* Header */}
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              gap: 1,
              px: 2,
              py: 1.5,
              bgcolor: "linear-gradient(135deg, #00274c 0%, #08213f 100%)",
              color: "#ffffff",
              flexShrink: 0,
              borderBottom: "1px solid rgba(255, 255, 255, 0.08)",
            }}
          >
            <Box
              sx={{
                width: 32,
                height: 32,
                borderRadius: "10px",
                display: "grid",
                placeItems: "center",
                bgcolor: "rgba(255, 203, 5, 0.15)",
                color: "#ffcb05",
                flexShrink: 0,
              }}
            >
              <Sparkles size={18} />
            </Box>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography
                variant="subtitle2"
                sx={{
                  fontWeight: 700,
                  fontSize: "0.85rem",
                  color: "#ffffff !important",
                  lineHeight: 1.3,
                }}
              >
                SwiftDrop AI Assistant
              </Typography>
              <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
                <Box
                  sx={{
                    width: 6,
                    height: 6,
                    borderRadius: "50%",
                    bgcolor: preview ? "#f59e0b" : "#16a34a",
                  }}
                />
                <Typography
                  variant="caption"
                  sx={{
                    fontSize: "0.7rem",
                    color: "rgba(203, 213, 225, 0.8) !important",
                  }}
                >
                  {preview ? "Preview mode" : "Online"}
                </Typography>
              </Box>
            </Box>
            <Box sx={{ display: "flex", gap: 0.5, flexShrink: 0 }}>
              {!minimized && (
                <IconButton
                  size="small"
                  onClick={(e) => { e.stopPropagation(); setMinimized(true); }}
                  aria-label="Minimize chat"
                  sx={{
                    color: "rgba(203, 213, 225, 0.7)",
                    "&:hover": { bgcolor: "rgba(255, 255, 255, 0.1)", color: "#ffffff" },
                  }}
                >
                  <Minimize2 size={16} />
                </IconButton>
              )}
              <IconButton
                size="small"
                onClick={(e) => { e.stopPropagation(); handleClose(); }}
                aria-label="Close chat"
                sx={{
                  color: "rgba(203, 213, 225, 0.7)",
                  "&:hover": { bgcolor: "rgba(255, 255, 255, 0.1)", color: "#ffffff" },
                }}
              >
                <X size={16} />
              </IconButton>
            </Box>
          </Box>

          {/* Body (hidden when minimized) */}
          {!minimized && (
            <>
              {/* Messages Area */}
              <Box
                ref={scrollRef}
                role="log"
                aria-label="AI conversation"
                aria-live="polite"
                aria-relevant="additions text"
                aria-atomic={false}
                sx={{
                  flex: 1,
                  overflowY: "auto",
                  minWidth: 0,
                  p: 2,
                  bgcolor: "#f8fafc",
                  display: "flex",
                  flexDirection: "column",
                  gap: 1.5,
                }}
              >
                {/* Welcome message when no messages */}
                {messages.length === 0 && (
                  <Box
                    sx={{
                      m: "auto",
                      width: "100%",
                      maxWidth: 340,
                      textAlign: "center",
                      py: 3,
                    }}
                  >
                    <Box
                      sx={{
                        display: "grid",
                        placeItems: "center",
                        width: 48,
                        height: 48,
                        mx: "auto",
                        mb: 2,
                        borderRadius: "14px",
                        bgcolor: alpha("#ffcb05", 0.12),
                        color: "#00274c",
                      }}
                    >
                      <Bot size={24} aria-hidden="true" />
                    </Box>
                    <Typography
                      variant="h6"
                      sx={{
                        fontWeight: 700,
                        fontSize: "0.95rem",
                        color: "#00274c",
                        mb: 0.75,
                      }}
                    >
                      How can I help?
                    </Typography>
                    <Typography
                      variant="body2"
                      sx={{
                        fontSize: "0.8rem",
                        color: "#64748b",
                        mb: 2,
                        lineHeight: 1.5,
                      }}
                    >
                      Ask about packing, delivery stages, pricing, or preparing a shipment.
                    </Typography>
                    {preview && (
                      <Box
                        sx={{
                          p: 1.5,
                          borderRadius: "10px",
                          bgcolor: alpha("#2563eb", 0.08),
                          border: "1px solid rgba(37, 99, 235, 0.15)",
                          textAlign: "left",
                        }}
                      >
                        <Typography
                          variant="caption"
                          sx={{
                            fontSize: "0.72rem",
                            color: "#1d4ed8",
                            lineHeight: 1.5,
                          }}
                        >
                          AI replies are unavailable in dashboard previews. Sign in to use the live assistant.
                        </Typography>
                      </Box>
                    )}
                  </Box>
                )}

                {/* Messages */}
                {messages.map(message => (
                  <Box
                    component="article"
                    key={message.id}
                    aria-label={message.role === "user" ? "Your message" : "AI Assistant message"}
                    sx={{
                      display: "flex",
                      gap: 1,
                      flexDirection: message.role === "user" ? "row-reverse" : "row",
                      alignSelf: message.role === "user" ? "flex-end" : "flex-start",
                      maxWidth: "88%",
                      minWidth: 0,
                    }}
                  >
                    <Box
                      aria-hidden="true"
                      sx={{
                        flexShrink: 0,
                        mt: 0.5,
                        width: 28,
                        height: 28,
                        borderRadius: "8px",
                        display: "grid",
                        placeItems: "center",
                        bgcolor: message.role === "user" ? "#ffffff" : alpha("#ffcb05", 0.12),
                        color: message.role === "user" ? "#00274c" : "#00274c",
                        border: message.role === "user" ? "1px solid #e2e8f0" : "none",
                      }}
                    >
                      {message.role === "user" ? (
                        <UserRound size={14} />
                      ) : (
                        <Bot size={16} />
                      )}
                    </Box>
                    <Box
                      sx={{
                        minWidth: 0,
                        p: 1.5,
                        borderRadius: "12px",
                        bgcolor:
                          message.role === "user"
                            ? alpha("#ffcb05", 0.12)
                            : "#ffffff",
                        border: "1px solid #e2e8f0",
                        boxShadow: "0 1px 3px rgba(0, 0, 0, 0.04)",
                      }}
                    >
                      <Typography
                        variant="caption"
                        sx={{
                          fontWeight: 700,
                          display: "block",
                          mb: 0.5,
                          fontSize: "0.68rem",
                          color: "#64748b",
                        }}
                      >
                        {message.role === "user" ? "You" : "SwiftDrop AI"}
                      </Typography>
                      <Typography
                        variant="body2"
                        sx={{
                          whiteSpace: "pre-wrap",
                          overflowWrap: "anywhere",
                          lineHeight: 1.6,
                          fontSize: "0.82rem",
                          color: "#1e293b",
                        }}
                      >
                        {message.text}
                      </Typography>
                      {message.failed && (
                        <Typography
                          variant="caption"
                          sx={{
                            display: "block",
                            mt: 1,
                            fontSize: "0.7rem",
                            color: "#64748b",
                          }}
                        >
                          No reply received
                        </Typography>
                      )}
                    </Box>
                  </Box>
                ))}

                {/* Typing indicator */}
                {busy && (
                  <Box
                    role="status"
                    aria-label="Waiting for AI response"
                    sx={{
                      display: "flex",
                      alignItems: "center",
                      gap: 1,
                      color: "#64748b",
                      px: 1,
                      "@keyframes assistantPulse": {
                        "0%, 80%, 100%": { opacity: 0.3 },
                        "40%": { opacity: 1 },
                      },
                      "@media (prefers-reduced-motion: reduce)": {
                        "& .assistant-dot": { animation: "none" },
                      },
                    }}
                  >
                    <Bot size={16} aria-hidden="true" />
                    <Typography
                      variant="body2"
                      sx={{ fontSize: "0.78rem", color: "#64748b" }}
                    >
                      Getting a reply
                    </Typography>
                    {[0, 1, 2].map(index => (
                      <Box
                        key={index}
                        component="span"
                        className="assistant-dot"
                        aria-hidden="true"
                        sx={{
                          width: 5,
                          height: 5,
                          borderRadius: "50%",
                          bgcolor: "#64748b",
                          animation: "assistantPulse 1.2s infinite",
                          animationDelay: `${index * 0.15}s`,
                        }}
                      />
                    ))}
                  </Box>
                )}
              </Box>

              {/* Error alert */}
              {failure && (
                <Box
                  sx={{
                    mx: 2,
                    mb: 1,
                    p: 1.25,
                    borderRadius: "10px",
                    bgcolor: alpha("#dc2626", 0.06),
                    border: "1px solid rgba(220, 38, 38, 0.15)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    gap: 1,
                  }}
                >
                  <Typography
                    variant="caption"
                    sx={{ fontSize: "0.72rem", color: "#dc2626", flex: 1 }}
                  >
                    {failure.error}
                  </Typography>
                  <Box
                    component="button"
                    onClick={() => void send(failure)}
                    disabled={busy || preview}
                    sx={{
                      flexShrink: 0,
                      px: 1.5,
                      py: 0.5,
                      borderRadius: "6px",
                      bgcolor: "#dc2626",
                      color: "#ffffff",
                      fontSize: "0.7rem",
                      fontWeight: 600,
                      border: "none",
                      cursor: busy || preview ? "not-allowed" : "pointer",
                      opacity: busy || preview ? 0.5 : 1,
                      display: "flex",
                      alignItems: "center",
                      gap: 0.5,
                    }}
                  >
                    <RefreshIcon size={12} />
                    Retry
                  </Box>
                </Box>
              )}

              {/* Input Area */}
              <Box
                component="form"
                onSubmit={handleSubmit}
                aria-label="Send an AI message"
                sx={{
                  p: 1.5,
                  borderTop: "1px solid #e2e8f0",
                  bgcolor: "#ffffff",
                  flexShrink: 0,
                }}
              >
                <Box sx={{ display: "flex", gap: 1, alignItems: "flex-end" }}>
                  <Box
                    component="textarea"
                    aria-label="Message"
                    placeholder="Ask a delivery question..."
                    ref={inputRef}
                    value={draft}
                    disabled={busy || preview}
                    onChange={event => setDraft(event.target.value)}
                    onKeyDown={handleKeyDown}
                    rows={2}
                    sx={{
                      flex: 1,
                      minWidth: 0,
                      p: 1.25,
                      borderRadius: "12px",
                      border: "1px solid #e2e8f0",
                      bgcolor: "#f8fafc",
                      fontSize: "0.82rem",
                      lineHeight: 1.5,
                      color: "#1e293b",
                      outline: "none",
                      resize: "none",
                      fontFamily: "inherit",
                      transition: "border-color 200ms ease, box-shadow 200ms ease",
                      "&:focus": {
                        borderColor: "#ffcb05",
                        boxShadow: "0 0 0 3px rgba(255, 203, 5, 0.15)",
                      },
                      "&:disabled": {
                        opacity: 0.5,
                        cursor: "not-allowed",
                      },
                      "&::placeholder": {
                        color: "#94a3b8",
                      },
                    }}
                  />
                  <IconButton
                    type="submit"
                    disabled={!draft.trim() || busy || preview}
                    aria-label="Send message"
                    sx={{
                      width: 40,
                      height: 40,
                      flexShrink: 0,
                      borderRadius: "12px",
                      bgcolor: draft.trim() && !busy && !preview
                        ? "linear-gradient(135deg, #ffcb05 0%, #ffdb57 100%)"
                        : "#e2e8f0",
                      color: draft.trim() && !busy && !preview ? "#00274c" : "#94a3b8",
                      "&:hover": {
                        bgcolor: draft.trim() && !busy && !preview
                          ? "linear-gradient(135deg, #ffdb57 0%, #ffe866 100%)"
                          : "#e2e8f0",
                      },
                      "&:disabled": {
                        bgcolor: "#e2e8f0",
                        color: "#94a3b8",
                      },
                    }}
                  >
                    <Send size={18} />
                  </IconButton>
                </Box>
                <Typography
                  variant="caption"
                  sx={{
                    display: "block",
                    mt: 0.75,
                    fontSize: "0.65rem",
                    color: "#94a3b8",
                    textAlign: "center",
                  }}
                >
                  Enter to send · Shift+Enter for new line
                </Typography>
              </Box>
            </>
          )}
        </Box>
      )}
    </>
  );
}

/* Tiny refresh icon for retry button */
function RefreshIcon({ size = 16 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 2v6h-6" />
      <path d="M3 12a9 9 0 0 1 15-6.7L21 8" />
      <path d="M3 22v-6h6" />
      <path d="M21 12a9 9 0 0 1-15 6.7L3 16" />
    </svg>
  );
}
