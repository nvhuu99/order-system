package com.example.api_gateway.services.auth_service.dto;

public record RefreshAccessTokenRequest (String accessToken, String refreshToken) {}
