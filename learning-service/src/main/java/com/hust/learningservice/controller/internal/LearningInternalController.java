package com.hust.learningservice.controller.internal;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.learningservice.dto.response.CourseProgressResponse;
import com.hust.learningservice.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/internal/learning")
@RequiredArgsConstructor
public class LearningInternalController {

    private final LearningService learningService;

    @GetMapping("/progress")
    public ApiResponse<CourseProgressResponse> getCourseProgress(
            @RequestParam String userId, 
            @RequestParam String courseId) {
        return ApiResponse.<CourseProgressResponse>builder()
                .success(true)
                .payload(learningService.getCourseProgress(userId, courseId))
                .build();
    }

    @GetMapping("/progress/bulk")
    public ApiResponse<Map<String, CourseProgressResponse>> getCourseProgressBulk(
            @RequestParam String userId, 
            @RequestParam List<String> courseIds) {
        return ApiResponse.<Map<String, CourseProgressResponse>>builder()
                .success(true)
                .payload(learningService.getCourseProgressBulk(userId, courseIds))
                .build();
    }

}
