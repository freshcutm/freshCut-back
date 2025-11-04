package com.freshcut.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public class ChatDtos {
    public static class Message {
        private String role; // 'user' | 'assistant'
        private String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class ChatRequest {
        @NotEmpty
        private List<Message> messages;
        private String faceDescription; // opcional

        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }
        public String getFaceDescription() { return faceDescription; }
        public void setFaceDescription(String faceDescription) { this.faceDescription = faceDescription; }
    }

    public static class ChatResponse {
        private String reply;

        public ChatResponse() {}
        public ChatResponse(String reply) { this.reply = reply; }
        public String getReply() { return reply; }
        public void setReply(String reply) { this.reply = reply; }
    }
}