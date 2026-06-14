package com.iflytek.smartprep.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final int MIN_KEY_BITS = 256;

    @Value("${smartprep.jwt.secret}")
    private String secret;

    @Value("${smartprep.jwt.expire-hours}")
    private int expireHours;

    private volatile SecretKey cachedKey;

    private SecretKey key() {
        if (cachedKey != null) return cachedKey;
        synchronized (this) {
            if (cachedKey != null) return cachedKey;
            cachedKey = buildKey();
            return cachedKey;
        }
    }

    private SecretKey buildKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length * 8 >= MIN_KEY_BITS) {
            return Keys.hmacShaKeyFor(keyBytes);
        }
        // 配置的密钥过短 (< 32 字符/256 bits)，通过 SHA-256 哈希派生确定性密钥（保证重启后 token 仍有效）
        try {
            byte[] hashed = java.security.MessageDigest.getInstance("SHA-256").digest(keyBytes);
            log.warn("JWT_SECRET 长度不足 ({} bits → 已通过 SHA-256 扩展到 256 bits)。请将 .env 中 JWT_SECRET 设为至少 32 字符的随机字符串以消除此警告。", keyBytes.length * 8);
            return Keys.hmacShaKeyFor(hashed);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public String generateToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expireHours * 3600L);
        return Jwts.builder()
                .claims(Map.of("uid", userId, "username", username, "role", role))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }
}
