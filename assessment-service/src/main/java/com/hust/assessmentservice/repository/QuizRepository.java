package com.hust.assessmentservice.repository;

import com.hust.assessmentservice.entity.Quiz;
import com.hust.assessmentservice.entity.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, String> {
    Optional<Quiz> findByTargetIdAndTargetType(String targetId, TargetType targetType);
}
