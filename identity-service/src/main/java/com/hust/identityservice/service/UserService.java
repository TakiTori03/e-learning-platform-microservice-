package com.hust.identityservice.service;


import com.hust.identityservice.dto.request.*;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.commonlibrary.dto.ListResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    
    void assignRole(String userId, String roleName);
    
    List<String> getAvailableRoles();
    
    List<UserResponse> getUsersByStatus(String status);
    
    void updateUserStatus(String userId, String status);
    
    UserResponse getUserById(String id);
    
    List<UserResponse> getUsersByIds(List<String> ids);
    
    void updateLastLogin(String userId);
    
    UserResponse updateMyProfile(UserUpdateRequest request);
    
    ListResponse<UserResponse> getAllUsers(Pageable pageable);
}
