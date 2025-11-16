package com.example.api_gateway.services.auth_service.exceptions;

import com.example.api_gateway.services.auth_service.responses.BaseApiResponse;

public class AuthorityDeniedException extends AuthServiceException {
    public AuthorityDeniedException(BaseApiResponse response) {
        super(response);
    }
}