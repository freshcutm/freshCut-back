package com.freshcut.db.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.freshcut.db.model.Booking;

public interface BookingRepository extends MongoRepository<Booking, String> {

    @Query("{ 'barber' : ?0, 'startTime' : { $lt: ?1 }, 'endTime' : { $gt: ?2 } }")
    List<Booking> findOverlapping(String barber, LocalDateTime newEnd, LocalDateTime newStart);

    List<Booking> findByBarber(String barber);

    // Añadido: buscar por cliente autenticado (email)
    List<Booking> findByClientName(String clientName);
}