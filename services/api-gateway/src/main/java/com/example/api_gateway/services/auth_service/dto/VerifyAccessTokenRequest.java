package com.example.api_gateway.services.auth_service.dto;

public record VerifyAccessTokenRequest(String accessToken, String resourcePrefix) {
}
