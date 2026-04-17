package com.hust.courseservice.client;

import com.hust.commonlibrary.dto.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "interaction-service")
public interface InteractionClient {

    @PostMapping("/internal/reviews/course-ratings")
    ApiResponse<List<InternalCourseRatingResponse>> getCourseRatingsBulk(@RequestBody List<String> courseIds);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class InternalCourseRatingResponse {
        private String courseId;
        private Double avgRatingStars;
        private Long numOfReviews;
    }
}
