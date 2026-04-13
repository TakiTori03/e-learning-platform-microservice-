package com.hust.identityservice.service;

import com.hust.identityservice.dto.response.UserResponse;

public interface UserCacheService {
    UserResponse getCachedProfile(String userId);
    void evictProfile(String userId);
}
