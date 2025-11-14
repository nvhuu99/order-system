package com.example.inventory.repositories.product_reservations.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListRequest {

    public final static Integer NO_LIMIT = -1;

    String userId;

    String productId;

    Integer limit = NO_LIMIT;

    Integer page = 1;
}
