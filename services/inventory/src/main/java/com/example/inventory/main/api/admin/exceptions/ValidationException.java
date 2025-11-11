package com.example.inventory.main.api.admin.exceptions;

import com.example.inventory.main.api.admin.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.HashMap;

@RestControllerAdvice
public class ValidationException {

    @ExceptionHandler(value = WebExchangeBindException.class)
    public ResponseEntity<ApiResponse> handleInvalidArg(WebExchangeBindException ex) {
        var errors = new HashMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(e -> errors.put(e.getObjectName(), e.getDefaultMessage()));
        return ApiResponse.badRequest(errors);
    }
}
