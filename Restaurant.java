package model;

import java.util.UUID;

public class Restaurant {
    private UUID id;
    private String name;

    // سازنده پیش‌فرض برای Gson
    public Restaurant() {
    }

    public Restaurant(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters و Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
