package com.freshcut.db.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.freshcut.db.model.ChatLog;

public interface ChatLogRepository extends MongoRepository<ChatLog, String> {
    java.util.List<ChatLog> findByEmailAndSavedOrderByCreatedAtDesc(String email, boolean saved);
    java.util.Optional<ChatLog> findTopByEmailOrderByCreatedAtDesc(String email);
}
