package com.bronzera.mybus.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService{

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-ms}") long expirationMs
    ){
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String role, Long collegeId){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        var builder = Jwts.builder().subject(String.valueOf(userId)).claim("role", role).issuedAt(now).expiration(expiry);
        if(collegeId != null) {
            builder.claim("collegeId", collegeId);
        }
        return builder.signWith(key).compact();
    }

    public String generateLookupToken(Long studentId){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 10 * 60 * 1000L);
        return Jwts.builder()
        .subject(String.valueOf(studentId)).claim("role", "STUDENT_LOOKUP")
        .issuedAt(now).expiration(expiry).signWith(key).compact();
    }

    public  Claims parseToken(String token){
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}