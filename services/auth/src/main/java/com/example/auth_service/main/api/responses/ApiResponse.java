package com.example.auth_service.main.api.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse {
    protected Integer status;
    protected Boolean success;
    protected String message;
    protected Object data;
    protected Object errors;

    public static ResponseEntity<ApiResponse> ok(Object data) {
        return ResponseEntity.ok(new ApiResponse(200, true, "", data, null));
    }

    public static ResponseEntity<ApiResponse> created(Object data) {
        return new ResponseEntity<>(new ApiResponse(201, true, "", data, null), HttpStatus.valueOf(201));
    }

    public static ResponseEntity<ApiResponse> noContent() {
        return new ResponseEntity<>(new ApiResponse(204, true, "", null, null), HttpStatus.valueOf(204));
    }

    public static ResponseEntity<ApiResponse> unAuthorized(String message) {
        return new ResponseEntity<>(new ApiResponse(401, false, message, null, null), HttpStatus.valueOf(401));
    }

    public static ResponseEntity<ApiResponse> permissionDenied(String message) {
        return new ResponseEntity<>(new ApiResponse(403, false, message, null, null), HttpStatus.valueOf(403));
    }

    public static ResponseEntity<ApiResponse> expired() {
        return new ResponseEntity<>(new ApiResponse(419, false, null, null, null), HttpStatus.valueOf(419));
    }

    public static ResponseEntity<ApiResponse> notFound(String message) {
        return new ResponseEntity<>(new ApiResponse(404, false, message, null, null), HttpStatus.valueOf(404));
    }

    public static ResponseEntity<ApiResponse> badRequest(String message, Object errors) {
        return new ResponseEntity<>(new ApiResponse(400, false, message, null, errors), HttpStatus.valueOf(400));
    }

    public static ResponseEntity<ApiResponse> internalServerError(String message) {
        return new ResponseEntity<>(new ApiResponse(500, false, message, null, null), HttpStatus.valueOf(500));
    }
}
