package com.example.api_gateway.services.auth_service.dto;

import java.util.List;

public record BasicAuthRequest(String username, String password, List<String> roles){
}
