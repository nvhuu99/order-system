package com.example.api_gateway.services.auth_service.dto;

import java.util.List;

public record RegisterResult(
    String id,
    String username,
    String email,
    List<String> roles
) {
}
