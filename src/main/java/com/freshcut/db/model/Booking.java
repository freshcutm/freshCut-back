package com.freshcut.db.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "bookings")
public class Booking {
    @Id
    private String id;
    private String clientName;
    private String barber;
    private String service;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer priceCents;
    private String status; // CONFIRMED, CANCELLED

    public Booking() {}

    public Booking(String clientName, String barber, String service, LocalDateTime startTime, LocalDateTime endTime) {
        this.clientName = clientName;
        this.barber = barber;
        this.service = service;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Booking(String clientName, String barber, String service, LocalDateTime startTime, LocalDateTime endTime, Integer priceCents) {
        this.clientName = clientName;
        this.barber = barber;
        this.service = service;
        this.startTime = startTime;
        this.endTime = endTime;
        this.priceCents = priceCents;
        this.status = "CONFIRMED";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getBarber() { return barber; }
    public void setBarber(String barber) { this.barber = barber; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getPriceCents() { return priceCents; }
    public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}