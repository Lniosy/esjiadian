package com.lniosy.usedappliance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final int expirationDays;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.expiration-days:7}") int expirationDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationDays = expirationDays;
    }

    public String createToken(Long userId, String username, Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();
        Instant exp = now.plus(expirationDays, ChronoUnit.DAYS);
        List<String> roles = authorities.stream().map(GrantedAuthority::getAuthority).toList();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public AuthUser toAuthUser(String token) {
        Claims claims = parseClaims(token);
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles", List.class);
        List<SimpleGrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        return new AuthUser(userId, username, authorities);
    }
}
