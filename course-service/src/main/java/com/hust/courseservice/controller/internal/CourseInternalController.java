package com.hust.courseservice.controller.internal;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.repository.LessonRepository;
import com.hust.courseservice.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/courses")
@RequiredArgsConstructor
public class CourseInternalController {

    private final LessonRepository lessonRepository;
    private final CourseService courseService;

    @GetMapping("/{courseId}/lesson-count")
    public ApiResponse<Long> getLessonCount(@PathVariable String courseId) {
        long count = lessonRepository.countByCourseId(courseId);
        return ApiResponse.<Long>builder()
                .success(true)
                .payload(count)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> getCourseDetail(@PathVariable String id) {
        return ApiResponse.<CourseResponse>builder()
                .success(true)
                .payload(courseService.detail(id))
                .build();
    }
}
