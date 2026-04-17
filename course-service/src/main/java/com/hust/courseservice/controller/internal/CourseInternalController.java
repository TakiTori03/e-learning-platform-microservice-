package com.hust.courseservice.controller.internal;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/courses")
@RequiredArgsConstructor
public class CourseInternalController {

    private final LessonRepository lessonRepository;
    private final com.hust.courseservice.service.CourseService courseService;

    @GetMapping("/{courseId}/lesson-count")
    public ApiResponse<Long> getLessonCount(@PathVariable String courseId) {
        long count = lessonRepository.countByCourseId(courseId);
        return ApiResponse.<Long>builder()
                .success(true)
                .payload(count)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> getCourseById(@PathVariable String id) {
        return ApiResponse.<com.hust.courseservice.dto.response.CourseResponse>builder()
                .success(true)
                .payload(courseService.detail(id))
                .build();
    }
}
