package com.hust.identityservice.controller.internal;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.identityservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserInternalController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable String id) {
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .payload(userService.getUserById(id))
                .build();
    }

    @PostMapping("/bulk")
    public ApiResponse<List<UserResponse>> getUsersByIds(@RequestBody List<String> ids) {
        return ApiResponse.<java.util.List<UserResponse>>builder()
                .success(true)
                .payload(userService.getUsersByIds(ids))
                .build();
    }
}
