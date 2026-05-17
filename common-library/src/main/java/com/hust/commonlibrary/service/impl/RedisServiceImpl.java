package com.hust.commonlibrary.service.impl;

import com.hust.commonlibrary.constant.AppConstants;
import com.hust.commonlibrary.service.RedisService;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final String keyPrefix;

    public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate, String applicationName) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = AppConstants.Redis_Constants.APP_PREFIX + applicationName + ":";
    }

    private String getFullKey(String key) {
        if (key == null) return null;
        if (key.startsWith(AppConstants.Redis_Constants.APP_PREFIX)) return key;
        return keyPrefix + key;
    }

    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(getFullKey(key), value);
    }

    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(getFullKey(key), value, timeout, unit);
    }

    @Override
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(getFullKey(key), value, timeout, unit));
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(getFullKey(key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(getFullKey(key));
        if (value == null) return null;
        return (T) value;
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(getFullKey(key));
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getFullKey(key)));
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(getFullKey(key), timeout, unit));
    }

    @Override
    public Long getExpire(String key) {
        return redisTemplate.getExpire(getFullKey(key));
    }

    @Override
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(getFullKey(key), delta);
    }
}
