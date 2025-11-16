package com.example.auth_service.main.api.controllers;

import com.example.auth_service.main.api.responses.ApiResponse;
import com.example.auth_service.main.api.responses.records.RegisterResult;
import com.example.auth_service.services.auth.dto.RefreshAccessTokenRequest;
import com.example.auth_service.services.auth.dto.VerifyAccessTokenRequest;
import com.example.auth_service.services.auth.AuthenticationService;
import com.example.auth_service.services.auth.dto.BasicAuthRequest;
import com.example.auth_service.services.auth.exceptions.AuthorityDeniedException;
import com.example.auth_service.services.auth.exceptions.UnauthorizedException;
import com.example.auth_service.services.users.UserService;
import com.example.auth_service.services.users.dto.SaveUser;
import com.example.auth_service.utils.auth_jwt.exceptions.TokenExpiredException;
import com.example.auth_service.utils.auth_jwt.exceptions.TokenRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userSvc;
    
    @Autowired
    private AuthenticationService authSvc;


    @PostMapping("/basic-auth")
    public Mono<ResponseEntity<ApiResponse>> basicAuth(@Validated @RequestBody BasicAuthRequest body) {
        return authSvc
            .basicAuth(body)
            .map(ApiResponse::ok)
            .doOnSuccess(ex -> log.info("basic authentication success - username: {}", body.getUsername()))
            .doOnError(ex -> log.error("basic authentication failed - {}", ex.getMessage()))
            .onErrorResume(UnauthorizedException.class, ex -> Mono.just(ApiResponse.unAuthorized(ex.getMessage())))
            .onErrorResume(ex -> Mono.just(ApiResponse.badRequest(ex.getMessage(), null)))
        ;
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<ApiResponse>> register(@Validated @RequestBody SaveUser body) {
        return userSvc
            .save(body)
            .map(user -> {
                var data = new RegisterResult(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRoles()
                );
                return ApiResponse.ok(data);
            })
            .doOnSuccess(ok -> log.info("registered user successfully - username: {}", body.getUsername()))
            .doOnError(ex -> log.error("register user failed - {}", ex.getMessage()))
            .onErrorResume(ex -> Mono.just(ApiResponse.badRequest(ex.getMessage(), null)))
        ;
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verify(@Validated @RequestBody VerifyAccessTokenRequest body) {
        try {
            return authSvc
                .verifyAccessToken(body)
                .map(ok -> ApiResponse.ok(null))
                .doOnSuccess(ok -> log.info("verified access token successfully"))
                .doOnError(ex -> log.error("verify access token failed - {}", ex.getMessage()))
                .onErrorResume(TokenRejectedException.class, ex -> Mono.just(ApiResponse.badRequest(ex.getMessage(), null)))
                .onErrorResume(UnauthorizedException.class, ex -> Mono.just(ApiResponse.unAuthorized(ex.getMessage())))
                .onErrorResume(AuthorityDeniedException.class, ex -> Mono.just(ApiResponse.permissionDenied(ex.getMessage())))
                .onErrorResume(ex -> Mono.just(ApiResponse.internalServerError(null)))
                .block()
            ;
        } catch (Exception e) {
            log.error(e.getMessage());
            return ApiResponse.internalServerError("");
        }
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<ApiResponse>> refresh(@Validated @RequestBody RefreshAccessTokenRequest body) {
        return authSvc
            .refreshAccessToken(body)
            .map(ApiResponse::ok)
            .doOnSuccess(ok -> log.info("refresh tokens successfully"))
            .doOnError(ex -> log.error("refresh tokens failed - {}", ex.getMessage()))
            .onErrorResume(TokenRejectedException.class, ex -> Mono.just(ApiResponse.badRequest(ex.getMessage(), null)))
            .onErrorResume(TokenExpiredException.class, ex -> Mono.just(ApiResponse.expired()))
            .onErrorResume(ex -> Mono.just(ApiResponse.badRequest("invalid token", null)))
        ;
    }
}
