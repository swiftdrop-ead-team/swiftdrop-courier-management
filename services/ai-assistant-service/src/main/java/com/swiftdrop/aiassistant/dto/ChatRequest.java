package com.swiftdrop.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatRequest {
    @NotBlank(message = "Message cannot be blank")
    @Size(max = 2000, message = "Message is too long")
    private String message;

    @Size(max = 8, message = "Too much conversation history")
    private List<ConversationMessage> history = new ArrayList<>();

    @Data
    public static class ConversationMessage {
        @Size(max = 10)
        private String role;

        @Size(max = 2000)
        private String text;
    }
}