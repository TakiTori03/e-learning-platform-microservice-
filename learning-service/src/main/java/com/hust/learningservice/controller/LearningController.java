package com.hust.learningservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.learningservice.dto.request.LessonProgressRequest;
import com.hust.learningservice.dto.response.CourseProgressResponse;
import com.hust.learningservice.service.LearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;

    @PostMapping("/track")
    public ApiResponse<Void> trackProgress(@RequestBody @Valid LessonProgressRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        learningService.trackProgress(userId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @GetMapping("/progress/{courseId}")
    public ApiResponse<CourseProgressResponse> getMyProgress(@PathVariable String courseId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ApiResponse.<CourseProgressResponse>builder()
                .success(true)
                .payload(learningService.getCourseProgress(userId, courseId))
                .build();
    }
}
