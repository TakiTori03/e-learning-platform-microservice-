package com.hust.interactionservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse extends TimeResponse {
    private String id;
    private String code;
    private String courseId;
    private String userId;
    private String title;
    private String content;
    private Double ratingStar;
    private java.util.List<ReviewResponse> replies;
}
