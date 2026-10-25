package com.example.jwtpartea.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, expirationMs, ACCESS_TOKEN);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, null, REFRESH_TOKEN);
    }

    private String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            Long tokenExpirationMs,
            String tokenType
    ) {
        Date now = new Date();
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put(TOKEN_TYPE_CLAIM, tokenType);

        var builder = Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .signWith(getSigningKey());

        if (tokenExpirationMs != null) {
            builder.expiration(new Date(now.getTime() + tokenExpirationMs));
        }

        return builder.compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        Date expiration = claims.getExpiration();
        return ACCESS_TOKEN.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                && userDetails.getUsername().equals(claims.getSubject())
                && expiration != null
                && !expiration.before(new Date());
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        return REFRESH_TOKEN.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                && userDetails.getUsername().equals(claims.getSubject())
                && claims.getExpiration() == null;
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
