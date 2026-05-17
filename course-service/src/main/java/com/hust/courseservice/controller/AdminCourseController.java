package com.hust.courseservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<CourseResponse>>> getAllCourses(
            @RequestParam(required = false) String q,
            @org.springframework.data.web.PageableDefault org.springframework.data.domain.Pageable pageable) {
        // Reuse search or detail logic but for admin view (showing all statuses)
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.search(q, null, null, null, null, pageable))
                        .build()
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> adminCreate(
            @RequestBody CourseRequest request,
            @RequestParam(required = false) String instructorId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CourseResponse>builder()
                        .success(true)
                        .payload(courseService.adminCreate(request, instructorId))
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> adminUpdate(
            @PathVariable String id,
            @RequestBody CourseRequest request,
            @RequestParam(required = false) String instructorId) {
        return ResponseEntity.ok(
                ApiResponse.<CourseResponse>builder()
                        .success(true)
                        .payload(courseService.adminUpdate(id, request, instructorId))
                        .build()
        );
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> adminDelete(@RequestBody List<String> ids) {
        courseService.adminDelete(ids);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Courses deleted successfully")
                        .build()
        );
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable String id) {
        courseService.adminUpdateStatus(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Course approved successfully")
                        .build()
        );
    }

    @GetMapping("/{id}/histories")
    public ResponseEntity<ApiResponse<ListResponse<Object>>> getHistories(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<Object>>builder()
                        .success(true)
                        .payload(courseService.getHistories(id, page, limit))
                        .build()
        );
    }
}
