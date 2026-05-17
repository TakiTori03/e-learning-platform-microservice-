package com.hust.identityservice.service.impl;

import com.hust.commonlibrary.service.RedisService;
import com.hust.commonlibrary.constant.RedisPrefixConstants;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.hust.identityservice.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RedisService redisService;
    
    @Override
    public void blacklistToken(String token, Jwt jwt) {
        if (token == null) return;
        
        long expiration = jwt.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
        if (expiration > 0) {
            String key = RedisPrefixConstants.getTokenBlacklistKey(token);
            redisService.set(key, "true", expiration, TimeUnit.SECONDS);
            log.info("Token blacklisted for {} seconds", expiration);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        String key = RedisPrefixConstants.getTokenBlacklistKey(token);
        return redisService.hasKey(key);
    }
}
