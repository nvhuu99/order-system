package com.example.auth_service.utils;

import com.example.auth_service.utils.exceptions.InvalidTokenException;
import com.example.auth_service.utils.exceptions.TokenExpiredException;
import com.example.auth_service.utils.exceptions.TokenRejectedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtils {

    @Value("${token-service.jwt.secret}")
    private String secret;

    @Value("${token-service.access-token-expires-after-seconds}")
    private Integer accessTokenExpiresAfterSeconds;

    @Value("${token-service.refresh-token-expires-after-seconds}")
    private Integer refreshTokenExpiresAfterSeconds;


    public String createAccessToken(String username, List<String> roles) {
        return Jwts.builder()
            .setSubject(username)
            .claim("roles", roles)
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plusSeconds(accessTokenExpiresAfterSeconds)))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
            .compact()
            ;
    }

    public String createRefreshToken(String username) {
        return Jwts.builder()
            .setSubject(username)
            .claim("token_type", "refresh_token")
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plusSeconds(refreshTokenExpiresAfterSeconds)))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
            .compact()
        ;
    }

    public Claims parseJwt(String token, Boolean mustNotExpired) throws InvalidTokenException, TokenExpiredException, TokenRejectedException {
        try {
            return Jwts
                .parserBuilder()
                .setSigningKey(secret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
            ;
        } catch (ExpiredJwtException ex) {
            if (mustNotExpired) {
                throw new TokenExpiredException();
            }
            return ex.getClaims();
        } catch (SignatureException ex) {
            throw new TokenRejectedException();
        } catch (Exception ex) {
            throw new InvalidTokenException();
        }
    }
}
