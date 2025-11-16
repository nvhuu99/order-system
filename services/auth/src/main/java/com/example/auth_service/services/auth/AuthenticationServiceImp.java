package com.example.auth_service.services.auth;

import com.example.auth_service.repositories.users.entities.UserRoleAuthorities;
import com.example.auth_service.services.auth.dto.AuthTokens;
import com.example.auth_service.services.auth.dto.BasicAuthRequest;
import com.example.auth_service.services.auth.dto.RefreshAccessTokenRequest;
import com.example.auth_service.services.auth.dto.VerifyAccessTokenRequest;
import com.example.auth_service.services.auth.exceptions.AuthorityDeniedException;
import com.example.auth_service.services.auth.exceptions.UnauthorizedException;
import com.example.auth_service.services.users.UserService;
import com.example.auth_service.services.users.dto.UserDetails;
import com.example.auth_service.services.users.exceptions.BadCredentialsException;
import com.example.auth_service.utils.auth_jwt.AuthJwtUtils;
import com.example.auth_service.utils.auth_jwt.exceptions.TokenRejectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Service
public class AuthenticationServiceImp implements AuthenticationService {

    @Autowired
    private UserService userSvc;

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private AuthJwtUtils authJwtUtils;


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
                authJwtUtils.createAccessToken(uname, roles),
                authJwtUtils.createRefreshToken(uname)
            ))
            .flatMap(tokens -> userSvc
                .saveTokens(uname, tokens.accessToken(), tokens.refreshToken())
                .then(Mono.just(tokens))
            )
        ;
    }

    @Override
    public Mono<Void> verifyAccessToken(VerifyAccessTokenRequest request) {
        var accessToken = request.getAccessToken();
        var resourcePrefix = request.getResourcePrefix();
        var claims = authJwtUtils.parse(accessToken, true);
        return userSvc
            .findByAccessToken(accessToken)
            .switchIfEmpty(Mono.error(new TokenRejectedException()))
            .map(usr -> {
                if (!Objects.equals(usr.getUsername(), claims.getUsername())) {
                    throw new TokenRejectedException();
                }
                return usr;
            })
            .map(usr -> {
                for (var role: claims.getRoles()) {
                    if (! usr.getRoles().contains(role)) {
                        throw new UnauthorizedException(role);
                    }
                }
                return usr;
            })
            .map(usr -> {
                for (var role: claims.getRoles()) {
                    if (UserRoleAuthorities.hasAuthority(role, resourcePrefix)) {
                        return usr;
                    }
                }
                throw new AuthorityDeniedException(resourcePrefix);
            })
            .then()
        ;
    }

    @Override
    public Mono<AuthTokens> refreshAccessToken(RefreshAccessTokenRequest request) {
        var accessToken = request.getAccessToken();
        var refreshToken = request.getRefreshToken();
        return userSvc
            .findByAccessTokenAndRefreshToken(accessToken, refreshToken)
            .switchIfEmpty(Mono.error(new TokenRejectedException()))
            .map(usr -> {
                var atClaims = authJwtUtils.parse(accessToken, false);
                var rtClaims = authJwtUtils.parse(refreshToken, true);
                if (!Objects.equals(usr.getUsername(), atClaims.getUsername()) ||
                    !Objects.equals(usr.getUsername(), rtClaims.getUsername())
                ) {
                    throw new TokenRejectedException();
                }
                return usr;
            })
            .flatMap(usr -> {
                var tokens = new AuthTokens(
                    authJwtUtils.createAccessToken(usr.getUsername(), usr.getRoles()),
                    authJwtUtils.createRefreshToken(usr.getUsername())
                );
                return userSvc
                    .saveTokens(usr.getUsername(), tokens.accessToken(), tokens.refreshToken())
                    .then(Mono.just(tokens))
                ;
            })
        ;
    }
}
