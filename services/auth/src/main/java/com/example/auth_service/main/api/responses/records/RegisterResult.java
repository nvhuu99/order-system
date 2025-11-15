package com.example.auth_service.main.api.responses.records;

import java.util.List;

public record RegisterResult(
    String id,
    String username,
    String email,
    List<String> roles
) {
}
