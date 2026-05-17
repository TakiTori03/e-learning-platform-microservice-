package com.hust.identityservice.service.impl;

import com.hust.commonlibrary.constant.AppConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.identityservice.dto.request.UserUpdateRequest;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.identityservice.entity.User;
import com.hust.identityservice.entity.UserStatus;
import com.hust.identityservice.mapper.UserMapper;
import com.hust.identityservice.repository.UserRepository;
import com.hust.identityservice.repository.http.AuthRepository;
import com.hust.identityservice.service.UserService;
import com.hust.commonlibrary.annotation.CustomCache;
import com.hust.commonlibrary.annotation.CustomCacheEvict;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;


    @Override
    @Transactional
    @CustomCacheEvict(key = "'user:profile:' + #userId") // 🧹 DỌN DẸP CACHE: Xóa cache profile cũ khi gán Role!
    public void assignRole(String userId, String roleName) {
        // 1. Ghi lên Keycloak Server
        authRepository.assignRole(userId, roleName);

        // 2. Đồng bộ Ghi kép (Dual-write) xuống Local DB PostgreSQL
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setRole(roleName);
        userRepository.save(user);

        log.info("BFF: Assigned role {} and synchronized DB for User {}", roleName, userId);
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
    @CustomCacheEvict(key = "'user:profile:' + #userId") // 🧹 DỌN DẸP CACHE: Xóa cache profile khi Admin phê duyệt hoặc đổi status!
    public void updateUserStatus(String userId, String status) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserStatus oldStatus = user.getStatus();
        UserStatus newStatus = UserStatus.valueOf(status.toUpperCase());
        user.setStatus(newStatus);

        userRepository.save(user);
        log.info("Admin updated user status: {} -> {}", userId, newStatus);

        // 🔥 LUỒNG DUYỆT GIẢNG VIÊN (Accept flow):
        if (oldStatus == UserStatus.PENDING && newStatus == UserStatus.ACTIVE) {
            log.info("Admin approved user {}. Granting INSTRUCTOR role in Keycloak and DB.", userId);

            // 1. Gán trên Keycloak
            authRepository.assignRole(userId, AppConstants.Role_Constants.ROLE_INSTRUCTOR);

            // 2. Đồng bộ Ghi kép xuống Local DB SQL
           user.setRole(AppConstants.Role_Constants.ROLE_INSTRUCTOR);
            userRepository.save(user);
        }
    }

    @Override
    @CustomCache(key = "'user:profile:' + #id", ttl = 24, unit = TimeUnit.HOURS) // 🚀 TRỌNG TÂM CACHING: Tự động Cache 24H cho Profile (Giảm tải 90% DB)!
    public UserResponse getUserById(String id) {
        User user = userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @Override
    public List<UserResponse> getUsersByIds(List<String> ids) {
        List<UUID> uuids = ids.stream()
                .map(UUID::fromString)
                .toList();

        List<User> users = userRepository.findAllById(uuids);

        return users.stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Override
    @Transactional
    public void updateLastLogin(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setLastLogin(java.time.Instant.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    @CustomCacheEvict(key = "'user:profile:' + #currentUserId") // 🧹 DỌN DẸP CACHE: Xóa cache profile cũ khi User tự cập nhật!
    public UserResponse updateMyProfile(UserUpdateRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName().trim());
        if (request.getLastName() != null) user.setLastName(request.getLastName().trim());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getHeadline() != null) user.setHeadline(request.getHeadline());
        if (request.getBiography() != null) user.setBiography(request.getBiography());
        if (request.getLanguage() != null) user.setLanguage(request.getLanguage());
        if (request.getShowProfile() != null) user.setShowProfile(request.getShowProfile());
        if (request.getShowCourses() != null) user.setShowCourses(request.getShowCourses());

        user = userRepository.save(user);
        
        // Trả về trực tiếp từ mapper (Không gọi Keycloak)
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public ListResponse<UserResponse> getAllUsers(Pageable pageable) {
        log.info("Fetching paginated user list from DB (Fast SQL Query): {}", pageable);

        Page<User> userPage = userRepository.findAll(pageable);

        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(userMapper::toUserResponse)
                .toList();

        return ListResponse.of(userResponses, userPage);
    }
}
