package com.hust.identityservice.mapper;

import com.hust.identityservice.dto.request.InstructorRegistrationRequest;
import com.hust.identityservice.dto.request.UserRegistrationRequest;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.identityservice.entity.User;
import org.springframework.stereotype.Component;


@Component
public class UserMapper {

    // 1. Map từ Request Đăng ký -> Entity User (Student)
    public User toUser(UserRegistrationRequest request) {
        if (request == null) return null;

        return User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();
    }

    // 1.1 Map từ Request Đăng ký -> Entity User (Instructor)
    public User toUser(InstructorRegistrationRequest request) {
        if (request == null) return null;

        return User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .headline(request.getHeadline())
                .biography(request.getBiography())
                .build();
    }

    // 2. Map từ Entity User -> Response trả về Frontend
    public UserResponse toUserResponse(User user) {
        if (user == null) return null;

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .headline(user.getHeadline())
                .biography(user.getBiography())
                .language(user.getLanguage())
                .socials(user.getSocials())
                .showProfile(user.isShowProfile())
                .showCourses(user.isShowCourses())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
