package com.example.inventory.services.product_reservations.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListRequest extends com.example.inventory.repositories.product_reservations.dto.ListRequest {
}
