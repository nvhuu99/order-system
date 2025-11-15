package com.example.auth_service.services.auth.dto;

import com.example.auth_service.repositories.configs.validators.ValidRoles;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BasicAuthRequest {

    @NotEmpty
    @Length(min = 2, max = 50)
    String username;

    @NotEmpty
    @Length(min = 3, max = 30)
    String password;

    @NotEmpty
    @ValidRoles
    List<String> roles;
}
