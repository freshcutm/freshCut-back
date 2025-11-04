package com.freshcut.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.freshcut.db.model.Barber;
import com.freshcut.db.model.ServiceItem;
import com.freshcut.db.repository.BarberRepository;
import com.freshcut.db.repository.ServiceItemRepository;
import com.freshcut.util.LruCache;

@Service
public class CatalogService {
    private final BarberRepository barberRepository;
    private final ServiceItemRepository serviceRepository;
    private final LruCache<String, List<Barber>> barberCache = new LruCache<>(2, 60_000);
    private final LruCache<String, List<ServiceItem>> serviceCache = new LruCache<>(2, 60_000);

    public CatalogService(BarberRepository barberRepository, ServiceItemRepository serviceRepository) {
        this.barberRepository = barberRepository;
        this.serviceRepository = serviceRepository;
    }

    public List<Barber> listActiveBarbers() {
        List<Barber> cached = barberCache.get("barbers");
        if (cached != null) return cached;
        List<Barber> fresh = barberRepository.findAll().stream().filter(Barber::isActive).collect(Collectors.toList());
        barberCache.put("barbers", fresh);
        return fresh;
    }

    public List<ServiceItem> listActiveServices() {
        List<ServiceItem> cached = serviceCache.get("services");
        if (cached != null) return cached;
        List<ServiceItem> fresh = serviceRepository.findAll().stream().filter(ServiceItem::isActive).collect(Collectors.toList());
        serviceCache.put("services", fresh);
        return fresh;
    }

    public void invalidate() {
        barberCache.clear();
        serviceCache.clear();
    }
}