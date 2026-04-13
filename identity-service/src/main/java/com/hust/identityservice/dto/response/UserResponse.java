package com.hust.identityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hust.commonlibrary.dto.TimeResponse;
import com.hust.identityservice.entity.UserStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserResponse extends TimeResponse {
    private UUID id;

    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String avatar;
    private String headline;
    private String biography;
    private String language;

    // Map chứa các link socials (Facebook, LinkedIn...)
    private Map<String, String> socials;

    private boolean showProfile;
    private boolean showCourses;

    private Set<String> roles;
    private UserStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant lastLogin;

}