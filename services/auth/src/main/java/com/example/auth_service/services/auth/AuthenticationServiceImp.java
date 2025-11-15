package com.example.auth_service.services.auth;

import com.example.auth_service.services.auth.dto.AuthTokens;
import com.example.auth_service.services.auth.dto.BasicAuthRequest;
import com.example.auth_service.services.auth.exceptions.UnauthorizedException;
import com.example.auth_service.services.users.UserService;
import com.example.auth_service.services.users.dto.UserDetails;
import com.example.auth_service.services.users.exceptions.BadCredentialsException;
import com.example.auth_service.utils.JwtUtils;
import com.example.auth_service.utils.exceptions.TokenRejectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
public class AuthenticationServiceImp implements AuthenticationService {

    @Autowired
    private UserService userSvc;

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JwtUtils jwtUtils;


    @Override
    public Mono<AuthTokens> basicAuth(BasicAuthRequest req) {
        var uname = req.getUsername();
        var pwd = req.getPassword();
        var roles = req.getRoles();
        return Mono
            .fromCallable(() -> {
                var credentials = new UsernamePasswordAuthenticationToken(uname, pwd);
                var result = authManager.authenticate(credentials);
                var userDetails = (UserDetails) result.getPrincipal();
                for (var role: roles) {
                    if (! userDetails.getRoles().contains(role)) {
                        throw new UnauthorizedException(role);
                    }
                }
                return userDetails;
            })
            .onErrorMap(ex -> new BadCredentialsException())
            .map(ignored -> new AuthTokens(
                jwtUtils.createAccessToken(uname, roles),
                jwtUtils.createRefreshToken(uname)
            ))
            .flatMap(tokens -> userSvc
                .saveTokens(uname, tokens.accessToken(), tokens.refreshToken())
                .then(Mono.just(tokens))
            )
        ;
    }

    @Override
    public Mono<Void> verifyAccessToken(String accessToken) {
        var claims = jwtUtils.parseJwt(accessToken, true);
        return userSvc
            .findByAccessToken(accessToken)
            .switchIfEmpty(Mono.error(new TokenRejectedException()))
            .doOnSuccess(usr -> {
                if (!Objects.equals(usr.getUsername(), claims.getSubject())) {
                    throw new TokenRejectedException();
                }
            })
            .then()
        ;
    }

    @Override
    public Mono<AuthTokens> refreshAccessToken(String accessToken, String refreshToken) {
        return userSvc
            .findByAccessTokenAndRefreshToken(accessToken, refreshToken)
            .switchIfEmpty(Mono.error(new TokenRejectedException()))
            .map(usr -> {
                var atClaims = jwtUtils.parseJwt(accessToken, false);
                var rtClaims = jwtUtils.parseJwt(refreshToken, true);
                if (!Objects.equals(usr.getUsername(), atClaims.getSubject()) ||
                    !Objects.equals(usr.getUsername(), rtClaims.getSubject())
                ) {
                    throw new TokenRejectedException();
                }
                return usr;
            })
            .flatMap(usr -> {
                var tokens = new AuthTokens(
                    jwtUtils.createAccessToken(usr.getUsername(), usr.getRoles()),
                    jwtUtils.createRefreshToken(usr.getUsername())
                );
                return userSvc
                    .saveTokens(usr.getUsername(), tokens.accessToken(), tokens.refreshToken())
                    .then(Mono.just(tokens))
                ;
            })
        ;
    }
}
