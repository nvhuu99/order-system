package com.example.api_gateway.services.auth_service.responses;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseApiResponse {
    protected Integer status;
    protected Boolean success;
    protected String message;
    protected Object data;
    protected Map<String, String> errors = new HashMap<>();

    public static BaseApiResponse fromJson(String json) throws Exception {
        return new ObjectMapper().readValue(json, BaseApiResponse.class);
    }

    public Boolean success() { return getSuccess(); }

    public Map<String, String> getErrors() {
        if (errors != null) {
            return errors;
        }
        return new HashMap<>();
    }
}
