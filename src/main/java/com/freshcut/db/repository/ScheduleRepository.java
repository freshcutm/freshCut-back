package com.freshcut.db.repository;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.freshcut.db.model.Schedule;

public interface ScheduleRepository extends MongoRepository<Schedule, String> {
    List<Schedule> findByBarberIdAndDayOfWeek(String barberId, DayOfWeek dayOfWeek);
    List<Schedule> findByBarberId(String barberId);
}