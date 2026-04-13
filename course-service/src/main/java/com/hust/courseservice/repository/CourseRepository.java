package com.hust.courseservice.repository;

import com.hust.courseservice.entity.Course;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {
    
    // Tìm kiếm toàn văn (Full-text search) dựa trên các trường được đánh dấu @TextIndexed
    List<Course> findAllBy(TextCriteria criteria);

    List<Course> findAllByCategoryId(String categoryId);
}
