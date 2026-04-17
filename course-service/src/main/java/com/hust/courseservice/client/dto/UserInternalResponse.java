package com.hust.courseservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInternalResponse {
    private String id;
    private String fullName;
    private String avatar;
    private String biography;
    private String headline;
}
