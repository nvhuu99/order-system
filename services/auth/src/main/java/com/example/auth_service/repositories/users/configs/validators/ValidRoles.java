package com.example.auth_service.repositories.users.configs.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidRolesValidator.class)
@Documented
public @interface ValidRoles {
    String message() default "roles contain unknown value(s)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

