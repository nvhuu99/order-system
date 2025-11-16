package com.example.api_gateway.services.auth_service.exceptions;

import com.example.api_gateway.services.auth_service.responses.BaseApiResponse;
import feign.Response;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

@Getter
public class AuthServiceException extends RuntimeException {

    private final BaseApiResponse response;

    public AuthServiceException(BaseApiResponse response) {
        super(response.getMessage());
        this.response = response;
    }

    public static AuthServiceException mapFromResponse(Response response) throws Exception {
        var apiResponse = BaseApiResponse.fromJson(new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8));
        return switch (response.status()) {
            case 400 -> new TokenRejectedException(apiResponse);
            case 401 -> new UnauthorizedException(apiResponse);
            case 403 -> new AuthorityDeniedException(apiResponse);
            case 404 -> new InvalidParametersException(apiResponse);
            case 419 -> new TokenExpiredException(apiResponse);
            default -> new AuthServiceException(apiResponse);
        };
    }
}
