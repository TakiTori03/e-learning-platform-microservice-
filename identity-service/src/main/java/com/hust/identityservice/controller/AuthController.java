package com.hust.identityservice.controller;

import java.util.List;
import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.identityservice.dto.request.InstructorRegistrationRequest;
import com.hust.identityservice.dto.request.LoginRequest;
import com.hust.identityservice.dto.request.UserRegistrationRequest;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.identityservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .payload(userService.login(request, response))
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@RequestBody @Valid UserRegistrationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .payload(userService.register(request))
                .build();
    }

    @PostMapping("/register-instructor")
    public ApiResponse<UserResponse> registerInstructor(@RequestBody @Valid InstructorRegistrationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .payload(userService.registerInstructor(request))
                .build();
    }

    @PostMapping("/upgrade-instructor")
    public ApiResponse<UserResponse> upgradeToInstructor(@RequestBody @Valid InstructorRegistrationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .payload(userService.upgradeToInstructor(request))
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .payload(userService.getMyInfo())
                .build();
    }

    @GetMapping("/users/status/{status}")
    public ApiResponse<List<UserResponse>> getUsersByStatus(@PathVariable String status) {
        return ApiResponse.<List<UserResponse>>builder()
                .success(true)
                .payload(userService.getUsersByStatus(status))
                .build();
    }

    @PutMapping("/users/{userId}/status")
    public ApiResponse<Void> updateUserStatus(
            @PathVariable String userId,
            @RequestParam String status) {
        userService.updateUserStatus(userId, status);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            HttpServletRequest request, HttpServletResponse response) {
        userService.logout(request, response);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PostMapping("/refresh-token")
    public ApiResponse<Void> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        userService.refreshToken(request, response);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PostMapping("/assign-role")
    public ApiResponse<Void> assignRole(
            @RequestParam String userId,
            @RequestParam String roleName) {
        userService.assignRole(userId, roleName);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @GetMapping("/roles")
    public ApiResponse<List<String>> getAvailableRoles() {
        return ApiResponse.<List<String>>builder()
                .success(true)
                .payload(userService.getAvailableRoles())
                .build();
    }

}
