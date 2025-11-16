package com.example.api_gateway.services.auth_service.responses;

import com.example.api_gateway.services.auth_service.dto.RegisterResult;

public class RegisterResponse extends BaseApiResponse {

    public RegisterResult getRegisterResult() {
        if (this.data instanceof RegisterResult registerResult) {
            return registerResult;
        }
        return null;
    }
}
