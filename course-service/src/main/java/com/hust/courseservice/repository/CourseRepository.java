package com.hust.courseservice.repository;

import com.hust.courseservice.entity.Course;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {

    Page<Course> findAllBy(TextCriteria criteria, Pageable pageable);

    List<Course> findAllByCategoryId(String categoryId);

    List<Course> findAllByInstructorId(String instructorId);
}
