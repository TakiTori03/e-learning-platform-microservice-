package com.hust.courseservice.service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.entity.enums.CourseStatus;
import com.hust.courseservice.entity.enums.CourseAccess;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CourseService {
    // Standard CRUD (matching monolith admin/client)
    java.util.Map<String, Long> countByCategories(List<String> categoryIds);

    CourseResponse create(CourseRequest request);
    CourseResponse update(String id, CourseRequest request);
    void delete(List<String> ids);
    CourseResponse detail(String id);
    ListResponse<CourseResponse> search(
            String q,
            List<String> authors,
            List<String> topics,
            List<String> levels,
            List<String> prices,
            Double rating,
            CourseStatus status,
            Pageable pageable
    );

    // Specific Monolith Functionalities
    List<CourseResponse> getPopularCourses(int limit);
    List<CourseResponse> getRelatedCourses(String courseId, int limit);
//    CourseResponse getFullDetail(String id); // getCourseDetail
    void increaseView(String id);
    void updateStatus(String id, CourseStatus status, CourseAccess access); // updateActiveStatusCourse
    List<CourseResponse> getAllActiveCourses();
    ListResponse<Object> getHistories(String id, int page, int limit);

    // Dedicated Admin Methods (Full privileges)
    void adminUpdateStatus(String id, CourseStatus status, CourseAccess access);
}
