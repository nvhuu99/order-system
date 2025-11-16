package com.example.auth_service.utils.auth_jwt;

import com.example.auth_service.utils.auth_jwt.exceptions.InvalidTokenException;
import com.example.auth_service.utils.auth_jwt.exceptions.TokenExpiredException;
import com.example.auth_service.utils.auth_jwt.exceptions.TokenRejectedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class AuthJwtUtils {

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
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plusSeconds(refreshTokenExpiresAfterSeconds)))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
            .compact()
        ;
    }

    public AuthClaims parse(String token, Boolean mustNotExpired) throws InvalidTokenException, TokenExpiredException, TokenRejectedException {
        try {
            var claims = Jwts
                .parserBuilder()
                .setSigningKey(secret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
            ;
            return extractAuthClaims(claims);
        }
        catch (ExpiredJwtException ex) {
            if (mustNotExpired) {
                throw new TokenExpiredException();
            }
            return extractAuthClaims(ex.getClaims());
        }
        catch (SignatureException ex) {
            throw new TokenRejectedException();
        }
        catch (Exception ex) {
            throw new InvalidTokenException();
        }
    }

    @SuppressWarnings("uncheck")
    private AuthClaims extractAuthClaims(Claims claims) {
        String username = claims.getSubject();
        List<String> roles = null;
        try {
            roles =  (List<String>)claims.get("roles");
        } catch (Exception ignore) {
            roles = new ArrayList<>();
        }
        return new AuthClaims(username, roles);
    }
}
