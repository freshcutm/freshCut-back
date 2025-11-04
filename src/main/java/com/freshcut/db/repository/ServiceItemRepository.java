package com.freshcut.db.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.freshcut.db.model.ServiceItem;

public interface ServiceItemRepository extends MongoRepository<ServiceItem, String> {
    Optional<ServiceItem> findByNameAndActiveTrue(String name);
}