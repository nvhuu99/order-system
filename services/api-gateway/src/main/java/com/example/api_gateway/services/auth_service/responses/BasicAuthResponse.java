package com.example.api_gateway.services.auth_service.responses;

import com.example.api_gateway.services.auth_service.dto.AuthTokens;

public class BasicAuthResponse extends BaseApiResponse {

    public AuthTokens getAuthTokens() {
        if (this.data instanceof AuthTokens authTokens) {
            return authTokens;
        }
        return null;
    }

    public String getAccessToken() {
        var authTokens = getAuthTokens();
        return (authTokens != null) ? authTokens.accessToken() : null;
    }

    public String getRefreshToken() {
        var authTokens = getAuthTokens();
        return (authTokens != null) ? authTokens.refreshToken() : null;
    }
}
