package com.freshcut.db.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chat_logs")
public class ChatLog {
    @Id
    private String id;
    private String email; // opcional, si viene token
    private List<Msg> messages;
    private String faceDescription; // opcional
    private String reply;
    private String rejectReason; // opcional: motivo de rechazo por irrelevancia/no rostro
    private boolean saved = false; // marcado por el usuario cuando guarda el chat
    private Instant createdAt = Instant.now();

    public static class Msg {
        private String role;
        private String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<Msg> getMessages() { return messages; }
    public void setMessages(List<Msg> messages) { this.messages = messages; }
    public String getFaceDescription() { return faceDescription; }
    public void setFaceDescription(String faceDescription) { this.faceDescription = faceDescription; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public boolean isSaved() { return saved; }
    public void setSaved(boolean saved) { this.saved = saved; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}