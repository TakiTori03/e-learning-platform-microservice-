package com.hust.courseservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping("/create")
    public ApiResponse<CourseResponse> createCourse(@RequestBody CourseRequest request) {
        return ApiResponse.<CourseResponse>builder()
                .success(true)
                .payload(courseService.createCourse(request))
                .build();
    }

    @GetMapping("/all")
    public ApiResponse<List<CourseResponse>> getAllCourses() {
        return ApiResponse.<List<CourseResponse>>builder()
                .success(true)
                .payload(courseService.getAllCourses())
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<List<CourseResponse>> search(@RequestParam String q) {
        return ApiResponse.<List<CourseResponse>>builder()
                .success(true)
                .payload(courseService.searchCourses(q))
                .build();
    }

    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<CourseResponse>> getByCategory(@PathVariable String categoryId) {
        return ApiResponse.<List<CourseResponse>>builder()
                .success(true)
                .payload(courseService.getCoursesByCategory(categoryId))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> getCourseById(@PathVariable String id) {
        return ApiResponse.<CourseResponse>builder()
                .success(true)
                .payload(courseService.getCourseById(id))
                .build();
    }
}
