package com.hust.courseservice.service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.CourseResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CourseService {
    // Standard CRUD (matching monolith admin/client)
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
            Pageable pageable
    );

    // Specific Monolith Functionalities
    List<CourseResponse> getPopularCourses(int limit);
    List<CourseResponse> getRelatedCourses(String courseId, int limit);
//    CourseResponse getFullDetail(String id); // getCourseDetail
    void increaseView(String id);
    void updateStatus(String id); // updateActiveStatusCourse
    List<CourseResponse> getAllActiveCourses();
    ListResponse<Object> getHistories(String id, int page, int limit);

    // Dedicated Admin Methods (Full privileges)
    CourseResponse adminCreate(CourseRequest request, String instructorId);
    CourseResponse adminUpdate(String id, CourseRequest request, String instructorId);
    void adminDelete(List<String> ids);
    void adminUpdateStatus(String id);
}
