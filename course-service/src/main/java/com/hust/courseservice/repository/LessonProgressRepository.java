package com.hust.courseservice.repository;

import com.hust.courseservice.entity.LessonProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends MongoRepository<LessonProgress, String> {
    Optional<LessonProgress> findByUserIdAndLessonId(String userId, String lessonId);
    List<LessonProgress> findAllByUserIdAndLessonIdIn(String userId, List<String> lessonIds);
}
