package com.example.inventory.main.api.admin.responses;

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
    private final Object errors;

    public static ResponseEntity<ApiResponse> ok(Object data) {
        return ResponseEntity.ok(new ApiResponse(true, "", data, null));
    }

    public static ResponseEntity<ApiResponse> created(Object data) {
        return new ResponseEntity(new ApiResponse(true, "", data, null), HttpStatus.CREATED);
    }

    public static ResponseEntity<ApiResponse> internalServerError(String message) {
        if (message == null) {
            message = "Internal Server Error";
        }
        return ResponseEntity.internalServerError().body(new ApiResponse(false, message, null, null));
    }

    public static ResponseEntity<ApiResponse> notFound() {
        return new ResponseEntity(new ApiResponse(false, "", null, null), HttpStatus.NOT_FOUND);
    }

    public static ResponseEntity<ApiResponse> badRequest(Object errors) {
        return new ResponseEntity<>(new ApiResponse(false, null, null, errors), HttpStatus.BAD_REQUEST);
    }
}
