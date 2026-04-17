package com.hust.interactionservice.controller.internal;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.interactionservice.dto.response.InternalCourseRatingResponse;
import com.hust.interactionservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/reviews")
@RequiredArgsConstructor
public class ReviewInternalController {

    private final ReviewService reviewService;

    @PostMapping("/course-ratings")
    public ApiResponse<List<InternalCourseRatingResponse>> getCourseRatingsBulk(@RequestBody List<String> courseIds) {
        return ApiResponse.<List<InternalCourseRatingResponse>>builder()
                .success(true)
                .payload(reviewService.getCourseRatingsBulk(courseIds))
                .build();
    }
}
