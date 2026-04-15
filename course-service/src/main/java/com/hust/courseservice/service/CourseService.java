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
    ListResponse<CourseResponse> search(String text, Pageable pageable);

    // Specific Monolith Functionalities
    List<CourseResponse> getPopularCourses(int limit);
    List<CourseResponse> getRelatedCourses(String courseId, int limit);
    List<CourseResponse> getSuggestedCourses(String userId, int limit);
    List<CourseResponse> getCoursesOrderedByUser(String userId);
    List<String> getWishlistIds(String userId);
    List<CourseResponse> getWishlistCourses(String userId);
    CourseResponse getEnrolledDetail(String id); // getCourseEnrolledByUserId
    CourseResponse getFullDetail(String id); // getCourseDetail
    void increaseView(String id);
    List<String> getUsersByCourseId(String id);
    void updateStatus(String id); // updateActiveStatusCourse
    List<CourseResponse> getAllActiveCourses();
    ListResponse<Object> getHistories(String id, int page, int limit);
}
