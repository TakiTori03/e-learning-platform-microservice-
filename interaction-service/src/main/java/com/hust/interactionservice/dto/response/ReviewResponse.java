package com.hust.interactionservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Getter @Setter
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
