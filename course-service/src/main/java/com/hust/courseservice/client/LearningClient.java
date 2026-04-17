package com.hust.courseservice.client;

import com.hust.commonlibrary.dto.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "learning-service")
public interface LearningClient {

    @GetMapping("/internal/learning/progress")
    ApiResponse<CourseProgressInternalResponse> getCourseProgress(
            @RequestParam("userId") String userId, 
            @RequestParam("courseId") String courseId);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CourseProgressInternalResponse {
        private String userId;
        private String courseId;
        private List<String> finishedLessonIds;
        private Double progressPercentage;
        private Boolean isEnrolled;
    }
}
