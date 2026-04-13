package com.hust.identityservice.service.impl;

import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.identityservice.entity.User;
import com.hust.identityservice.mapper.UserMapper;
import com.hust.identityservice.repository.UserRepository;
import com.hust.identityservice.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCacheServiceImpl implements UserCacheService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Cacheable(value = "profile", key = "#userId")
    public UserResponse getCachedProfile(String userId) {
        log.info("Cache miss! Đang truy vấn Database cho User ID: {}", userId);
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
                
        return userMapper.toUserResponse(user);
    }

    @Override
    @CacheEvict(value = "profile", key = "#userId")
    public void evictProfile(String userId) {
        log.info("Xóa Cache cho User ID: {}", userId);
    }
}
