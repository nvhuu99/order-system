package com.example.api_gateway.services.auth_service.dto;

import java.util.List;

public record RegisterRequest (
    String id,
    String username,
    String email,
    String password,
    List<String> roles
){
}