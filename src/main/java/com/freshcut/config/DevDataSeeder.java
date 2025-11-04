package com.freshcut.config;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.freshcut.db.model.Barber;
import com.freshcut.db.model.Schedule;
import com.freshcut.db.model.ServiceItem;
import com.freshcut.db.repository.BarberRepository;
import com.freshcut.db.repository.ScheduleRepository;
import com.freshcut.db.repository.ServiceItemRepository;

@Configuration
public class DevDataSeeder {

    @Bean
    @SuppressWarnings("unused")
    CommandLineRunner seedData(BarberRepository barberRepo, ServiceItemRepository serviceRepo, ScheduleRepository scheduleRepo) {
        return args -> {
            // Sembrar/asegurar servicios base (idempotente)
            ensureService(serviceRepo, "Corte clásico", 30, 1500);
            ensureService(serviceRepo, "Fade medio", 45, 2000);
            ensureService(serviceRepo, "Barba", 20, 1000);

            // Nuevos servicios solicitados
            ensureService(serviceRepo, "Corte de barba", 25, 1200);
            ensureService(serviceRepo, "Mascarillas faciales", 15, 800);
            ensureService(serviceRepo, "Kings Cut", 60, 3000);

            // Sembrar un barbero demo si no hay ninguno (solo desarrollo)
            if (barberRepo.count() == 0) {
                Barber demo = new Barber();
                demo.setName("Barbero Demo");
                demo.setActive(true);
                Barber saved = barberRepo.save(demo);

                // Crear horarios por defecto para el barbero demo
                for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
                    Schedule s = new Schedule();
                    s.setBarberId(saved.getId());
                    s.setDayOfWeek(day);
                    s.setStartTime(LocalTime.of(9, 0));
                    s.setEndTime(LocalTime.of(18, 0));
                    scheduleRepo.save(s);
                }
            }

            // Asegurar horarios para barberos existentes si no hay ninguno aún
            if (scheduleRepo.count() == 0) {
                List<String> barberIds = barberRepo.findAll().stream().map(Barber::getId).toList();
                for (String barberId : barberIds) {
                    for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
                        Schedule s = new Schedule();
                        s.setBarberId(barberId);
                        s.setDayOfWeek(day);
                        s.setStartTime(LocalTime.of(9, 0));
                        s.setEndTime(LocalTime.of(18, 0));
                        scheduleRepo.save(s);
                    }
                }
            }
        };
    }

    private void ensureService(ServiceItemRepository repo, String name, int durationMinutes, int priceCents) {
        repo.findByNameAndActiveTrue(name).orElseGet(() -> {
            ServiceItem s = new ServiceItem();
            s.setName(name);
            s.setDurationMinutes(durationMinutes);
            s.setPriceCents(priceCents);
            s.setActive(true);
            return repo.save(s);
        });
    }
}