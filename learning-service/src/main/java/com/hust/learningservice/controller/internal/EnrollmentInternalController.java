package com.hust.learningservice.controller.internal;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.learningservice.entity.StudentEnrollment;
import com.hust.learningservice.service.LearningService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/enrollments")
@RequiredArgsConstructor
public class EnrollmentInternalController {

    private final LearningService enrollmentService;

    @GetMapping("/check-access")
    public ApiResponse<Boolean> checkAccess(
            @RequestParam String userId,
            @RequestParam String courseId) {
        
        boolean hasAccess = enrollmentService.hasAccess(userId, courseId);
        
        return ApiResponse.<Boolean>builder()
                .success(true)
                .payload(hasAccess)
                .build();
    }

    @GetMapping("/check-lesson-access")
    public ApiResponse<Boolean> checkLessonAccess(
            @RequestParam String userId,
            @RequestParam String lessonId) {
        
        boolean hasAccess = enrollmentService.checkLessonAccess(userId, lessonId);
        
        return ApiResponse.<Boolean>builder()
                .success(true)
                .payload(hasAccess)
                .build();
    }

    @GetMapping
    public ApiResponse<List<StudentEnrollment>> getEnrolledCourses(@RequestParam String userId) {
        return ApiResponse.<java.util.List<com.hust.learningservice.entity.StudentEnrollment>>builder()
                .success(true)
                .payload(enrollmentService.getEnrolledCourses(userId))
                .build();
    }
}
