package com.example.auth_service.services.users.dto;

import com.example.auth_service.repositories.configs.validators.ValidRoles;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SaveUser {

    @Length(min = 36, max = 36)
    String id;

    @NotEmpty
    @Length(min = 6, max = 50)
    String username;

    @NotEmpty
    @Length(min = 3, max = 255)
    @Email(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "Invalid email")
    String email;

    @NotEmpty
    @Length(min = 8, max = 30)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "Password must contain uppercase, lowercase, number, and special character"
    )
    String password;

    @NotEmpty
    @ValidRoles
    List<String> roles = new ArrayList<>();
}