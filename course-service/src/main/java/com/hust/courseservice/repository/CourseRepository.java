package com.hust.courseservice.repository;

import com.hust.courseservice.entity.Course;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.hust.courseservice.dto.response.CourseInsightReportResponse;
import com.hust.courseservice.dto.response.AuthorCourseReportResponse;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {

    Page<Course> findAllBy(TextCriteria criteria, Pageable pageable);

    List<Course> findAllByCategoryId(String categoryId);

    List<Course> findAllByInstructorId(String instructorId);

    List<Course> findAllByStatus(com.hust.courseservice.entity.enums.CourseStatus status);

    @org.springframework.data.mongodb.repository.Aggregation(pipeline = {
        "{ '$group': { " +
        "    '_id': null, " +
        "    'totalCourses': { $sum: 1 }, " +
        "    'totalActiveCourses': { $sum: { $cond: [ { $eq: [ '$status', 'APPROVED' ] }, 1, 0 ] } }, " +
        "    'totalDraftCourses': { $sum: { $cond: [ { $eq: [ '$status', 'DRAFT' ] }, 1, 0 ] } }, " +
        "    'totalViews': { $sum: '$views' } " +
        "} }"
    })
    List<CourseInsightReportResponse> getCourseInsightsAggregation();

    @org.springframework.data.mongodb.repository.Aggregation(pipeline = {
        "{ '$match': { 'instructorId': ?0 } }",
        "{ '$group': { " +
        "    '_id': '$instructorId', " +
        "    'totalCourses': { $sum: 1 }, " +
        "    'totalViews': { $sum: '$views' } " +
        "} }",
        "{ '$project': { 'instructorId': '$_id', 'totalCourses': 1, 'totalViews': 1, '_id': 0 } }"
    })
    List<AuthorCourseReportResponse> getAuthorCourseReportAggregation(String instructorId);
}
