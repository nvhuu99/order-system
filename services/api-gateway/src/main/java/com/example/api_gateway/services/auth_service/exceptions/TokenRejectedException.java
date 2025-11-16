package com.example.api_gateway.services.auth_service.exceptions;

import com.example.api_gateway.services.auth_service.responses.BaseApiResponse;

public class TokenRejectedException extends AuthServiceException {
    public TokenRejectedException(BaseApiResponse response) {
        super(response);
    }
}