package com.freshcut.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.freshcut.dto.BookingRequest;
import com.freshcut.db.model.Barber;
import com.freshcut.db.model.Booking;
import com.freshcut.db.model.Schedule;
import com.freshcut.db.model.ServiceItem;
import com.freshcut.db.repository.BarberRepository;
import com.freshcut.db.repository.BookingRepository;
import com.freshcut.db.repository.ScheduleRepository;
import com.freshcut.db.repository.ServiceItemRepository;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final BarberRepository barberRepository;
    private final ServiceItemRepository serviceRepository;

    public BookingService(
            BookingRepository bookingRepository,
            ScheduleRepository scheduleRepository,
            BarberRepository barberRepository,
            ServiceItemRepository serviceRepository) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.barberRepository = barberRepository;
        this.serviceRepository = serviceRepository;
    }

    public Booking create(BookingRequest req) {
        if (req.getStartTime() == null) {
            throw new IllegalArgumentException("La hora de inicio es obligatoria");
        }
        // Resolve service to compute duration and price
        ServiceItem service = serviceRepository.findByNameAndActiveTrue(req.getService())
                .orElseThrow(() -> new IllegalArgumentException("Servicio inválido o inactivo"));

        LocalDateTime startDateTime = req.getStartTime();
        LocalDateTime computedEnd = startDateTime.plusMinutes(service.getDurationMinutes());

        // Overlap check (by barber name as stored in Booking)
        List<Booking> conflicts = bookingRepository.findOverlapping(req.getBarber(), computedEnd, startDateTime);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Conflicto: el barbero ya tiene una reserva en ese horario");
        }

        // Schedule availability check (by barberId)
        Barber barber = barberRepository.findByNameAndActiveTrue(req.getBarber())
                .orElseThrow(() -> new IllegalArgumentException("Barbero inválido o inactivo"));
        DayOfWeek day = startDateTime.getDayOfWeek();
        LocalTime start = startDateTime.toLocalTime();
        LocalTime end = computedEnd.toLocalTime();
        List<Schedule> slots = scheduleRepository.findByBarberIdAndDayOfWeek(barber.getId(), day);
        boolean fits = slots.stream().anyMatch(s -> !start.isBefore(s.getStartTime()) && !end.isAfter(s.getEndTime()));
        if (!fits) {
            throw new IllegalStateException("El horario solicitado no encaja en la disponibilidad del barbero");
        }

        Booking b = new Booking(req.getClientName(), req.getBarber(), req.getService(), startDateTime, computedEnd, service.getPriceCents());
        return bookingRepository.save(b);
    }

    public List<Booking> list() {
        return bookingRepository.findAll();
    }

    // Añadido: listar reservas del cliente autenticado por email
    public List<Booking> listByClientName(String clientName) {
        return bookingRepository.findByClientName(clientName);
    }

    public Optional<Booking> findOne(String id) {
        return bookingRepository.findById(id);
    }

    public Booking update(String id, BookingRequest req) {
        Booking existing = bookingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        // Resolve service for duration and validate
        ServiceItem service = serviceRepository.findByNameAndActiveTrue(req.getService())
                .orElseThrow(() -> new IllegalArgumentException("Servicio inválido o inactivo"));

        LocalDateTime startDateTime = req.getStartTime();
        LocalDateTime computedEnd = startDateTime.plusMinutes(service.getDurationMinutes());

        // Overlap check excluding current booking
        List<Booking> conflicts = bookingRepository.findOverlapping(req.getBarber(), computedEnd, startDateTime);
        boolean hasConflict = conflicts.stream().anyMatch(b -> !b.getId().equals(existing.getId()));
        if (hasConflict) {
            throw new IllegalStateException("Conflicto: el barbero ya tiene una reserva en ese horario");
        }

        // Schedule availability check
        Barber barber = barberRepository.findByNameAndActiveTrue(req.getBarber())
                .orElseThrow(() -> new IllegalArgumentException("Barbero inválido o inactivo"));
        DayOfWeek day = startDateTime.getDayOfWeek();
        LocalTime start = startDateTime.toLocalTime();
        LocalTime end = computedEnd.toLocalTime();
        List<Schedule> slots = scheduleRepository.findByBarberIdAndDayOfWeek(barber.getId(), day);
        boolean fits = slots.stream().anyMatch(s -> !start.isBefore(s.getStartTime()) && !end.isAfter(s.getEndTime()));
        if (!fits) {
            throw new IllegalStateException("El horario solicitado no encaja en la disponibilidad del barbero");
        }

        existing.setClientName(req.getClientName());
        existing.setBarber(req.getBarber());
        existing.setService(req.getService());
        existing.setStartTime(startDateTime);
        existing.setEndTime(computedEnd);
        existing.setPriceCents(service.getPriceCents());
        return bookingRepository.save(existing);
    }

    public Booking cancel(String id) {
        Booking existing = bookingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        existing.setStatus("CANCELLED");
        return bookingRepository.save(existing);
    }

    // NUEVO: marcar como completada
    public Booking complete(String id) {
        Booking existing = bookingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        existing.setStatus("COMPLETED");
        return bookingRepository.save(existing);
    }

    public void delete(String id) {
        if (!bookingRepository.existsById(id)) {
            throw new IllegalArgumentException("Reserva no encontrada");
        }
        bookingRepository.deleteById(id);
    }
}