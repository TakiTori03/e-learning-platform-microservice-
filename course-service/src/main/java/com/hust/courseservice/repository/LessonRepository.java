package com.hust.courseservice.repository;

import com.hust.courseservice.entity.Lesson;
import com.hust.courseservice.entity.enums.CourseAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends MongoRepository<Lesson, String> {
    List<Lesson> findAllBySectionIdOrderByPositionAsc(String sectionId);
    
    List<Lesson> findAllByCourseId(String courseId);
    List<Lesson> findAllByCourseIdOrderByPositionAsc(String courseId);
    
    List<Lesson> findAllByCourseIdAndAccessOrderByPositionAsc(String courseId, CourseAccess access);

    Page<Lesson> findAllBy(TextCriteria criteria, Pageable pageable);
}
