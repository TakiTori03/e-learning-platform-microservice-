package com.hust.interactionservice.service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.interactionservice.dto.request.ReviewRequest;
import com.hust.interactionservice.dto.response.RatingResult;
import com.hust.interactionservice.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(ReviewRequest request);
    ListResponse<ReviewResponse> getReviewsByCourse(String courseId, String query, Integer rating, Pageable pageable);
    RatingResult getCourseRatingSummary(String courseId);
    List<com.hust.interactionservice.dto.response.InternalCourseRatingResponse> getCourseRatingsBulk(List<String> courseIds);

    // Review Reply
    ReviewResponse createReply(String reviewId, String content);
    List<ReviewResponse> getRepliesByReview(String reviewId);
}
