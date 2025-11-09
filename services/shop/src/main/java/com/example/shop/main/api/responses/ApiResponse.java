package com.example.shop.main.api.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Data
@AllArgsConstructor
public class ApiResponse {

    private final Boolean success;
    private final String message;
    private final Object data;

    public static ResponseEntity<ApiResponse> ok(Object data) {
        return ResponseEntity.ok(new ApiResponse(true, "", data));
    }

    public static ResponseEntity<ApiResponse> internalServerError(Throwable exception) {
        return ResponseEntity.internalServerError().body(new ApiResponse(false, exception.getMessage(), null));
    }

    public static ResponseEntity<ApiResponse> notFound() {
        return new ResponseEntity(new ApiResponse(false, "", null), HttpStatus.NOT_FOUND);
    }
}
