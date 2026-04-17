package com.hust.learningservice.controller.internal;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.learningservice.dto.request.EnrollmentBulkRequest;
import com.hust.learningservice.dto.response.CourseProgressResponse;
import com.hust.learningservice.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/enroll-bulk")
    public ApiResponse<Void> enrollStudentBulk(@RequestBody EnrollmentBulkRequest request) {
        learningService.enrollStudentBulk(request.getUserId(), request.getCourseIds());
        return ApiResponse.<Void>builder().success(true).build();
    }
}
