package com.hust.identityservice.service;

import com.hust.identityservice.dto.request.UserRegistrationRequest;
import com.hust.identityservice.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface UserService {

    UserResponse login(com.hust.identityservice.dto.request.LoginRequest request, HttpServletResponse response);
    UserResponse register(UserRegistrationRequest request);
    UserResponse registerInstructor(com.hust.identityservice.dto.request.InstructorRegistrationRequest request);
    UserResponse upgradeToInstructor(com.hust.identityservice.dto.request.InstructorRegistrationRequest request);
    UserResponse getMyInfo();
    void logout(HttpServletRequest request, HttpServletResponse response);
    void refreshToken(HttpServletRequest request, HttpServletResponse response);
    void assignRole(String userId, String roleName);
    List<String> getAvailableRoles();
    List<UserResponse> getUsersByStatus(String status);
    void updateUserStatus(String userId, String status);


}
