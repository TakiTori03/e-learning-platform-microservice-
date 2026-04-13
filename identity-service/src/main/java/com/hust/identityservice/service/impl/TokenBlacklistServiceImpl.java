package com.hust.identityservice.service.impl;

import com.hust.identityservice.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Override
    public void blacklistToken(String token, Jwt jwt) {
        if (token == null || jwt == null) return;
        
        long remainingTime = jwt.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
        if (remainingTime > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "revoked", Duration.ofSeconds(remainingTime));
            log.info("Token đã được đưa vào Blacklist. Sống thêm {} giây", remainingTime);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
