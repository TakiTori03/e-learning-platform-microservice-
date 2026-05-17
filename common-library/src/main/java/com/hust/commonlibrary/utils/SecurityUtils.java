package com.hust.commonlibrary.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

public class SecurityUtils {

    /**
     * Lấy UserId (Subject) từ SecurityContext của Spring Security.
     * @return Optional chứa UserId hoặc Empty nếu chưa đăng nhập.
     */
    public static Optional<String> getCurrentUserId() {
        return getCurrentJwt().map(Jwt::getSubject);
    }

    /**
     * Lấy UserId và ném lỗi nếu không tìm thấy.
     */
    public static String getCurrentUserIdOrThrow() {
        return getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
    }

    /**
     * Lấy đối tượng Jwt gốc từ SecurityContext.
     */
    public static Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }
}
