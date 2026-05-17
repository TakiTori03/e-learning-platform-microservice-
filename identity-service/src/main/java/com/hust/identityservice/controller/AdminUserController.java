package com.hust.identityservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.identityservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<UserResponse>>> getAllUsers(
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<UserResponse>>builder()
                        .success(true)
                        .payload(userService.getAllUsers(pageable))
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .payload(userService.getUserById(id))
                        .build()
        );
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableRoles() {
        return ResponseEntity.ok(
                ApiResponse.<java.util.List<String>>builder()
                        .success(true)
                        .payload(userService.getAvailableRoles())
                        .build()
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable String id,
            @RequestParam String status) {
        userService.updateUserStatus(id, status);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("User status updated successfully")
                        .build()
        );
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @PathVariable String id,
            @RequestParam String roleName) {
        userService.assignRole(id, roleName);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Role assigned successfully")
                        .build()
        );
    }
}
