package com.hust.interactionservice.service.impl;

import com.hust.interactionservice.dto.request.ReviewRequest;
import com.hust.interactionservice.dto.response.RatingResult;
import com.hust.interactionservice.dto.response.ReviewResponse;
import com.hust.interactionservice.entity.Review;
import com.hust.interactionservice.entity.ReviewReply;
import com.hust.interactionservice.mapper.ReviewMapper;
import com.hust.interactionservice.mapper.ReviewReplyMapper;
import com.hust.interactionservice.repository.ReviewReplyRepository;
import com.hust.interactionservice.repository.ReviewRepository;
import com.hust.interactionservice.service.ReviewService;
import com.hust.interactionservice.utils.AppUtils;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final ReviewMapper reviewMapper;
    private final ReviewReplyMapper reviewReplyMapper;

    @Override
    public ReviewResponse createReview(ReviewRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        Review review = reviewMapper.requestToEntity(request);
        review.setUserId(userId);
        review.setCreatedBy(userId);
        review.setCode(AppUtils.generateCode("REV"));
        review.setIsHidden(false);

        Review savedReview = reviewRepository.save(review);
        
        // TODO: Call Feign Client to Order Service to set item.reviewed = true
        // orderClient.updateItemReviewed(request.getOrderId(), request.getCourseId(), true);

        return reviewMapper.entityToResponse(savedReview);
    }

    @Override
    public ListResponse<ReviewResponse> getReviewsByCourse(String courseId, String query, Integer rating, Pageable pageable) {
        List<Double> stars = null;
        if (rating != null) {
            stars = switch (rating) {
                case 1 -> List.of(0.5, 1.0, 1.5);
                case 2 -> List.of(2.0, 2.5);
                case 3 -> List.of(3.0, 3.5);
                case 4 -> List.of(4.0, 4.5);
                case 5 -> List.of(5.0);
                default -> null;
            };
        }

        Page<Review> reviewPageRaw;
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasStars = stars != null;

        if (hasQuery && hasStars) {
            reviewPageRaw = reviewRepository.findByCourseIdAndTitleContainingIgnoreCaseAndRatingStarIn(courseId, query, stars, pageable);
        } else if (hasQuery) {
            reviewPageRaw = reviewRepository.findByCourseIdAndTitleContainingIgnoreCase(courseId, query, pageable);
        } else if (hasStars) {
            reviewPageRaw = reviewRepository.findByCourseIdAndRatingStarIn(courseId, stars, pageable);
        } else {
            reviewPageRaw = reviewRepository.findByCourseId(courseId, pageable);
        }

        List<ReviewResponse> reviewResponses = reviewPageRaw.getContent().stream()
                .map(reviewMapper::entityToResponse)
                .toList();

        // Populate Replies
        List<String> reviewIds = reviewResponses.stream().map(ReviewResponse::getId).toList();
        List<ReviewReply> allReplies = reviewReplyRepository.findByReviewIdIn(reviewIds);
        
        Map<String, List<ReviewReply>> groupedReplies = allReplies.stream()
                .collect(Collectors.groupingBy(ReviewReply::getReviewId));

        reviewResponses.forEach(res -> {
            List<ReviewReply> replies = groupedReplies.getOrDefault(res.getId(), List.of());
            res.setReplies(replies.stream().map(reviewReplyMapper::entityToResponse).toList());
        });

        return ListResponse.of(
                reviewResponses,
                reviewPageRaw.getNumber() + 1,
                reviewPageRaw.getSize(),
                reviewPageRaw.getTotalElements(),
                reviewPageRaw.getTotalPages(),
                reviewPageRaw.isLast()
        );
    }

    @Override
    public ReviewResponse createReply(String reviewId, String content) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        ReviewReply reply = ReviewReply.builder()
                .reviewId(reviewId)
                .userId(userId)
                .content(content)
                .code(AppUtils.generateCode("REPLY"))
                .isHidden(false)
                .createdBy(userId)
                .build();
        
        return reviewReplyMapper.entityToResponse(reviewReplyRepository.save(reply));
    }

    @Override
    public List<ReviewResponse> getRepliesByReview(String reviewId) {
        return reviewReplyRepository.findByReviewId(reviewId).stream()
                .map(reviewReplyMapper::entityToResponse)
                .toList();
    }

    @Override
    public List<com.hust.interactionservice.dto.response.InternalCourseRatingResponse> getCourseRatingsBulk(List<String> courseIds) {
        List<Review> allReviews = reviewRepository.findByCourseIdIn(courseIds);

        Map<String, List<Review>> grouped = allReviews.stream()
                .collect(Collectors.groupingBy(Review::getCourseId));

        return courseIds.stream().map(cid -> {
            List<Review> courseReviews = grouped.getOrDefault(cid, List.of());
            long count = courseReviews.size();
            double avg = count > 0 
                    ? courseReviews.stream().mapToDouble(Review::getRatingStar).average().orElse(0.0) 
                    : 0.0;

            return com.hust.interactionservice.dto.response.InternalCourseRatingResponse.builder()
                    .courseId(cid)
                    .avgRatingStars(Math.round(avg * 10.0) / 10.0)
                    .numOfReviews(count)
                    .build();
        }).toList();
    }

    @Override
    public RatingResult getCourseRatingSummary(String courseId) {
        List<Review> reviews = reviewRepository.findByCourseId(courseId);
        long total = reviews.size();
        
        if (total == 0) {
            return RatingResult.builder()
                    .averageRating(0.0)
                    .totalReviews(0L)
                    .ratingPercentages(new HashMap<>())
                    .build();
        }

        double sum = reviews.stream().mapToDouble(Review::getRatingStar).sum();
        double average = sum / total;

        // Tính toán phần trăm theo nhóm (Copy logic từ Monolith)
        Map<String, Long> counts = new HashMap<>();
        for (Review r : reviews) {
            double star = r.getRatingStar();
            String group = "5";
            if (star < 2.0) group = "1";
            else if (star < 3.0) group = "2";
            else if (star < 4.0) group = "3";
            else if (star < 5.0) group = "4";
            
            counts.put(group, counts.getOrDefault(group, 0L) + 1);
        }

        Map<String, String> percentages = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            String key = String.valueOf(i);
            long count = counts.getOrDefault(key, 0L);
            percentages.put(key, String.format("%.1f%%", (count * 100.0) / total));
        }

        return RatingResult.builder()
                .averageRating(Math.round(average * 10.0) / 10.0) // Làm tròn 1 chữ số thập phân
                .totalReviews(total)
                .ratingPercentages(percentages)
                .build();
    }
}
