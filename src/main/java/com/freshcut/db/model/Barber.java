package com.freshcut.db.model;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "barbers")
public class Barber {
    @Id
    private String id;
    private String name;
    private List<String> specialties;
    // Nuevo: bio, años de experiencia y tipos de cortes
    private String bio;
    private Integer experienceYears;
    private List<String> cutTypes;
    private boolean active = true;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getSpecialties() { return specialties; }
    public void setSpecialties(List<String> specialties) { this.specialties = specialties; }
    // Nuevo getters/setters
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
    public List<String> getCutTypes() { return cutTypes; }
    public void setCutTypes(List<String> cutTypes) { this.cutTypes = cutTypes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}