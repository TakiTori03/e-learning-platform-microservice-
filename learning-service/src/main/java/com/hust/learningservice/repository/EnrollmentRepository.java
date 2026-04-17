package com.hust.learningservice.repository;

import com.hust.learningservice.entity.StudentEnrollment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EnrollmentRepository extends MongoRepository<StudentEnrollment, String> {
    Optional<StudentEnrollment> findByUserIdAndCourseId(String userId, String courseId);
    List<StudentEnrollment> findByUserId(String userId);
    List<StudentEnrollment> findAllByUserIdAndCourseIdIn(String userId, List<String> courseIds);
}
