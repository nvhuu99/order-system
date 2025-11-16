package com.example.auth_service.repositories.users.entities;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {

    ADMIN("ADMIN"),
    CUSTOMER("CUSTOMER");

    private final String name;

    Role(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    public static Role from(String value) {
        for (Role r : values()) {
            if (r.name.equalsIgnoreCase(value)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
