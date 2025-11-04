package com.freshcut.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freshcut.db.model.Barber;
import com.freshcut.db.model.ServiceItem;
import com.freshcut.service.CatalogService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/barbers")
    public List<Barber> listBarbers() {
        return catalogService.listActiveBarbers();
    }

    @GetMapping("/services")
    public List<ServiceItem> listServices() {
        return catalogService.listActiveServices();
    }
}