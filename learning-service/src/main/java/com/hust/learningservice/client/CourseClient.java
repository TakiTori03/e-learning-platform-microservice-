package com.hust.learningservice.client;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.LessonInternalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "course-service")
public interface CourseClient {

    @GetMapping("/internal/courses/{courseId}/lesson-count")
    ApiResponse<Long> getLessonCount(@PathVariable String courseId);

    @GetMapping("/internal/lessons/{lessonId}")
    ApiResponse<LessonInternalResponse> getLessonDetail(@PathVariable String lessonId);
}
