package com.example.auth_service.main.api.exceptions;

import com.example.auth_service.main.api.controllers.AuthController;
import com.example.auth_service.main.api.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @ExceptionHandler(value = AuthenticationFailureException.class)
    public ResponseEntity<ApiResponse> handleAuthFailure(AuthenticationFailureException exception) {
        log.error(exception.getMessage());
        return ApiResponse.unAuthorized("");
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleInvalidArg(MethodArgumentNotValidException exception) {
        var errs = new HashMap<String, String>();
        for (var err: exception.getAllErrors()) {
            try {
                errs.put(((FieldError)err).getField(), err.getDefaultMessage());
            } catch (Exception ignored) {
            }
        }
        log.error(exception.getMessage());
        return ApiResponse.badRequest("Invalid arguments", errs);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse> handleUnknownException(Exception exception) {
        log.error(exception.getMessage());
        return ApiResponse.internalServerError("Internal Server Error");
    }
}
