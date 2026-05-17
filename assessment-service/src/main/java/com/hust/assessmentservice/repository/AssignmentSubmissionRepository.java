package com.hust.assessmentservice.repository;

import com.hust.assessmentservice.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, String> {
    List<AssignmentSubmission> findByUserId(String userId);
    List<AssignmentSubmission> findByAssignmentId(String assignmentId);
    Optional<AssignmentSubmission> findByUserIdAndAssignmentId(String userId, String assignmentId);
}
