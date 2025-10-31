package com.example.inventory.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ReservationStatus {
    OK("OK"),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK"),
    EXPIRED("EXPIRED");

    private final String value;

    ReservationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static ReservationStatus fromValue(String value) {
        for (ReservationStatus c : values()) {
            if (c.value.equalsIgnoreCase(value)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
