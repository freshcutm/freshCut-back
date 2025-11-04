package com.freshcut.db.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.freshcut.db.model.User;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}