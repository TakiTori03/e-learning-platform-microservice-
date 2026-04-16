package com.hust.interactionservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.interactionservice.dto.request.ReviewRequest;
import com.hust.interactionservice.dto.response.RatingResult;
import com.hust.interactionservice.dto.response.ReviewResponse;
import com.hust.interactionservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@RequestBody @Valid ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ReviewResponse>builder()
                .success(true)
                .payload(reviewService.createReview(request))
                .build());
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<ListResponse<ReviewResponse>>> getReviewsByCourse(
            @PathVariable String courseId,
            @RequestParam(required = false) String _q,
            @RequestParam(required = false) Integer _rating,
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<ListResponse<ReviewResponse>>builder()
                .success(true)
                .payload(reviewService.getReviewsByCourse(courseId, _q, _rating, pageable))
                .build());
    }

    @GetMapping("/course/{courseId}/summary")
    public ResponseEntity<ApiResponse<RatingResult>> getCourseRatingSummary(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.<RatingResult>builder()
                .success(true)
                .payload(reviewService.getCourseRatingSummary(courseId))
                .build());
    }

    @PostMapping("/{reviewId}/replies")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReply(@PathVariable String reviewId,
                                                                  @RequestBody String content) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ReviewResponse>builder()
                        .success(true)
                        .payload(reviewService.createReply(reviewId, content))
                        .build());
    }

    @GetMapping("/{reviewId}/replies")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReplies(@PathVariable String reviewId) {
        return ResponseEntity.ok(ApiResponse.<List<ReviewResponse>>builder()
                .success(true)
                .payload(reviewService.getRepliesByReview(reviewId))
                .build());
    }
}
