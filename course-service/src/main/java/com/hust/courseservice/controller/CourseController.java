package com.hust.courseservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> create(@RequestBody CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CourseResponse>builder()
                        .success(true)
                        .payload(courseService.create(request))
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> update(@PathVariable String id, @RequestBody CourseRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<CourseResponse>builder()
                        .success(true)
                        .payload(courseService.update(id, request))
                        .build()
        );
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestBody List<String> ids) {
        courseService.delete(ids);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ListResponse<CourseResponse>>> search(
            @RequestParam(name = "q", required = false) String text,
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.search(text, pageable))
                        .build()
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> detail(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<CourseResponse>builder()
                        .success(true)
                        .payload(courseService.detail(id))
                        .build()
        );
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getPopularCourses(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getPopularCourses(limit))
                        .build()
        );
    }

    @GetMapping("/related/{courseId}")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getRelatedCourses(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getRelatedCourses(courseId, limit))
                        .build()
        );
    }

    @GetMapping("/suggested/{userId}")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getSuggestedCourses(
            @PathVariable String userId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getSuggestedCourses(userId, limit))
                        .build()
        );
    }

    @GetMapping("/ordered/{userId}")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCoursesOrderedByUser(@PathVariable String userId) {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getCoursesOrderedByUser(userId))
                        .build()
        );
    }

    @GetMapping("/id/wishlist/{userId}")
    public ResponseEntity<ApiResponse<List<String>>> getWishlistIds(@PathVariable String userId) {
        return ResponseEntity.ok(
                ApiResponse.<List<String>>builder()
                        .success(true)
                        .payload(courseService.getWishlistIds(userId))
                        .build()
        );
    }

    @GetMapping("/wishlist/{userId}")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getWishlistCourses(@PathVariable String userId) {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getWishlistCourses(userId))
                        .build()
        );
    }



    @GetMapping("/enrolled/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getEnrolledDetail(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<CourseResponse>builder()
                        .success(true)
                        .payload(courseService.getEnrolledDetail(id))
                        .build()
        );
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getFullDetail(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<CourseResponse>builder()
                        .success(true)
                        .payload(courseService.getFullDetail(id))
                        .build()
        );
    }

    @PostMapping("/increase-view/{id}")
    public ResponseEntity<ApiResponse<Void>> increaseView(@PathVariable String id) {
        courseService.increaseView(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/user/getUserByCourse/{id}")
    public ResponseEntity<ApiResponse<List<String>>> getUsersByCourseId(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<List<String>>builder()
                        .success(true)
                        .payload(courseService.getUsersByCourseId(id))
                        .build()
        );
    }

    @PatchMapping("/update-active-status/{id}")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable String id) {
        courseService.updateStatus(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/all-active")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAllActiveCourses() {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseResponse>>builder()
                        .success(true)
                        .payload(courseService.getAllActiveCourses())
                        .build()
        );
    }

    @GetMapping("/histories/{id}")
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
