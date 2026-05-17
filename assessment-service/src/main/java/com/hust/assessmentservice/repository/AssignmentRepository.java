package com.hust.assessmentservice.repository;

import com.hust.assessmentservice.entity.Assignment;
import com.hust.assessmentservice.entity.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, String> {
    Optional<Assignment> findByTargetIdAndTargetType(String targetId, TargetType targetType);
}
