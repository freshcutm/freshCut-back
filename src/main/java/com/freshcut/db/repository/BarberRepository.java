package com.freshcut.db.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.freshcut.db.model.Barber;

public interface BarberRepository extends MongoRepository<Barber, String> {
    Optional<Barber> findByNameAndActiveTrue(String name);
}