package com.hust.interactionservice.repository;

import com.hust.interactionservice.entity.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    Page<Review> findByCourseId(String courseId, Pageable pageable);
    Page<Review> findByCourseIdAndTitleContainingIgnoreCase(String courseId, String title, Pageable pageable);
    Page<Review> findByCourseIdAndRatingStarIn(String courseId, List<Double> stars, Pageable pageable);
    Page<Review> findByCourseIdAndTitleContainingIgnoreCaseAndRatingStarIn(String courseId, String title, List<Double> stars, Pageable pageable);
    
    List<Review> findByCourseId(String courseId);
    Long countByCourseId(String courseId);
}
