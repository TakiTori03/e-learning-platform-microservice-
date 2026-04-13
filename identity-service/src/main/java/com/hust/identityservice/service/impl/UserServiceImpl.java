package com.hust.identityservice.service.impl;

import com.hust.commonlibrary.constant.AppConstants;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.identityservice.dto.request.InstructorRegistrationRequest;
import com.hust.identityservice.dto.request.LoginRequest;
import com.hust.identityservice.dto.request.UserRegistrationRequest;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.identityservice.entity.User;
import com.hust.identityservice.entity.UserStatus;
import com.hust.identityservice.mapper.UserMapper;
import com.hust.identityservice.repository.UserRepository;
import com.hust.identityservice.repository.http.AuthRepository;
import com.hust.identityservice.service.TokenBlacklistService;
import com.hust.identityservice.service.UserCacheService;
import com.hust.identityservice.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserCacheService userCacheService;
    
    @Override
    public UserResponse login(LoginRequest request, HttpServletResponse response) {
        AccessTokenResponse tokenResponse = authRepository.login(request.getEmail(), request.getPassword());
        setTokenCookies(response, tokenResponse);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Lấy Role trực tiếp từ Keycloak
        Set<String> roles = authRepository.getRolesForUser(user.getId().toString());

        UserResponse userResponse = userMapper.toUserResponse(user);
        userResponse.setRoles(roles);

        log.info("BFF: User logged in and cookies set: {}", user.getEmail());
        return userResponse;
    }

    /**
     * Helper: Tạo User trên Keycloak
     */
    private String createKeycloakUser(UserRegistrationRequest request) {
        if (Boolean.TRUE.equals(userRepository.existsByEmail(request.getEmail()))) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(request.getEmail());
        userRep.setEmail(request.getEmail());
        userRep.setFirstName(request.getFirstName());
        userRep.setLastName(request.getLastName());
        userRep.setEnabled(true);
        userRep.setEmailVerified(true);

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setTemporary(false);
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(request.getPassword());
        userRep.setCredentials(List.of(cred));

        return authRepository.createUser(userRep);
    }

    @Override
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        String keycloakUserId = createKeycloakUser(request);

        // Gan role Student
        authRepository.assignRole(keycloakUserId, AppConstants.Role_Constants.ROLE_STUDENT);

        User user = userMapper.toUser(request);
        user.setId(UUID.fromString(keycloakUserId));
        user.setAvatar(AppConstants.DEFAULT_AVATAR);
        user.setStatus(UserStatus.ACTIVE);
        
        user = userRepository.save(user);
        log.info("BFF: Student registered and synced: {}", user.getEmail());

        UserResponse response = userMapper.toUserResponse(user);
        response.setRoles(Set.of(AppConstants.Role_Constants.ROLE_STUDENT));

        return response;
    }

    @Override
    @Transactional
    public UserResponse registerInstructor(InstructorRegistrationRequest request) {
        String keycloakUserId = createKeycloakUser(request);

        // Gán Role Instructor
        authRepository.assignRole(keycloakUserId, AppConstants.Role_Constants.ROLE_INSTRUCTOR);

        User user = userMapper.toUser(request);
        user.setId(UUID.fromString(keycloakUserId));
        user.setAvatar(AppConstants.DEFAULT_AVATAR);
        user.setStatus(UserStatus.PENDING); // Đăng ký mới làm GV cần chờ duyệt hồ sơ

        user = userRepository.save(user);
        log.info("BFF: New Instructor registered and pending review: {}", user.getEmail());

        UserResponse response = userMapper.toUserResponse(user);
        response.setRoles(Set.of(AppConstants.Role_Constants.ROLE_INSTRUCTOR));

        return response;
    }

    @Override
    @Transactional
    public UserResponse upgradeToInstructor(InstructorRegistrationRequest request) {
        var jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = jwt.getSubject();

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 1. Gán thêm Role Instructor trên Keycloak (Giữ nguyên Role Student cũ)
        authRepository.assignRole(userId, AppConstants.Role_Constants.ROLE_INSTRUCTOR);

        // 2. Cập nhật Profile hiện có
        user.setHeadline(request.getHeadline());
        user.setBiography(request.getBiography());
        user.setStatus(UserStatus.PENDING); // Chờ duyệt để được cấp quyền Instructor thực thụ (Active)

        user = userRepository.save(user);
        log.info("BFF: Student upgraded to Instructor (Pending): {}", user.getEmail());

        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse getMyInfo() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        
        String userId = jwt.getSubject();
        UserResponse response = userCacheService.getCachedProfile(userId);

        // Trích xuất Roles từ Authorities (đã được KeycloakRoleConverter xử lý)
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", "")) // Đưa về dạng ngắn gọn: ADMIN, STUDENT
                .collect(Collectors.toSet());
        
        response.setRoles(roles);
        return response;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, AppConstants.Token_Constants.REFRESH_TOKEN);
        
        if (refreshToken != null && !refreshToken.isEmpty()) {
            authRepository.logout(refreshToken);
        }

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String accessToken = extractCookie(request, AppConstants.Token_Constants.ACCESS_TOKEN);
            tokenBlacklistService.blacklistToken(accessToken, jwt);
        }

        clearCookie(response, AppConstants.Token_Constants.ACCESS_TOKEN);
        clearCookie(response, AppConstants.Token_Constants.REFRESH_TOKEN);
        
        log.info("User logged out successfully");
    }


    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, AppConstants.Token_Constants.REFRESH_TOKEN);
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        AccessTokenResponse tokenResponse = authRepository.refreshToken(refreshToken);
        setTokenCookies(response, tokenResponse);
    }

    private void setTokenCookies(HttpServletResponse response, AccessTokenResponse tokenResponse) {
        ResponseCookie accessCookie = ResponseCookie.from(AppConstants.Token_Constants.ACCESS_TOKEN, tokenResponse.getToken())
                .httpOnly(true)
                .secure(false) // Set to true if using HTTPS
                .path("/")
                .maxAge(tokenResponse.getExpiresIn())
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from(AppConstants.Token_Constants.REFRESH_TOKEN, tokenResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false) 
                .path("/")
                .maxAge(tokenResponse.getRefreshExpiresIn() > 0 ? tokenResponse.getRefreshExpiresIn() : 604800) // Default 7 days
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0) 
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public void assignRole(String userId, String roleName) {
        authRepository.assignRole(userId, roleName);
    }

    @Override
    public List<String> getAvailableRoles() {
        return authRepository.getAvailableRoles().stream()
                .map(RoleRepresentation::getName)
                .toList();
    }

    @Override
    public List<UserResponse> getUsersByStatus(String status) {
        UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
        List<User> users = userRepository.findByStatus(userStatus);
        
        return users.stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Override
    @Transactional
    public void updateUserStatus(String userId, String status) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        UserStatus newStatus = UserStatus.valueOf(status.toUpperCase());
        user.setStatus(newStatus);
        
        userRepository.save(user);
        log.info("Admin updated user status: {} -> {}", userId, newStatus);
    }
}
