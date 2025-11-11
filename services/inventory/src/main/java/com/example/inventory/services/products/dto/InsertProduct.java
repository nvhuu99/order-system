package com.example.inventory.services.products.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InsertProduct {

    @NotEmpty
    @Length(min = 3, max = 255, message = "product name must be between 3-255 characters")
    String name;

    @NotNull
    @Min(0)
    BigDecimal price;

    @NotNull
    @Min(0)
    Integer stock;
}
