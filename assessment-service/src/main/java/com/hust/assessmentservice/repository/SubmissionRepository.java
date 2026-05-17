package com.hust.assessmentservice.repository;

import com.hust.assessmentservice.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, String> {
    List<Submission> findByUserIdAndQuizId(String userId, String quizId);
    boolean existsByUserIdAndQuizId(String userId, String quizId);
    List<Submission> findByUserId(String userId);
}
