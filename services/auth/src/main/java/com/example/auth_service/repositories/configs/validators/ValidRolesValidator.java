package com.example.auth_service.repositories.configs.validators;

import com.example.auth_service.repositories.entities.Role;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.*;
import java.util.stream.Collectors;

public class ValidRolesValidator implements ConstraintValidator<ValidRoles, Collection<String>> {

    private Set<String> allowed;

    @Override
    public void initialize(ValidRoles constraintAnnotation) {
        allowed = Arrays.stream(Role.values())
            .map(Enum::name)
            .collect(Collectors.toSet())
        ;
    }

    @Override
    public boolean isValid(Collection<String> values, ConstraintValidatorContext context) {
        if (values == null) {
            return true;
        }
        for (String v : values) {
            if (v == null || ! allowed.contains(v.trim().toUpperCase())) {
                return false;
            }
        }
        return true;
    }
}
