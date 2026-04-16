package com.hust.commonlibrary.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

public class SecurityUtils {

    /**
     * Lấy UserId (Subject) từ SecurityContext của Spring Security.
     * @return Optional chứa UserId hoặc Empty nếi chưa đăng nhập.
     */
    public static Optional<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        
        if (principal instanceof Jwt) {
            return Optional.ofNullable(((Jwt) principal).getSubject());
        }
        
        // Trường hợp không dùng JWT (ví dụ Basic Auth hoặc Testing)
        return Optional.ofNullable(authentication.getName());
    }

    /**
     * Lấy UserId và ném lỗi nếu không tìm thấy (Dùng cho các nghiệp vụ bắt buộc đăng nhập).
     */
    public static String getCurrentUserIdOrThrow() {
        return getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
    }
}
