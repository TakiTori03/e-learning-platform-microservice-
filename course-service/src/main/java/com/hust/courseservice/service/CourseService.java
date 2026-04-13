package com.hust.courseservice.service;

import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.CourseResponse;

import java.util.List;

public interface CourseService {
    CourseResponse createCourse(CourseRequest request);
    List<CourseResponse> getAllCourses();
    CourseResponse getCourseById(String id);
    List<CourseResponse> searchCourses(String query);
    List<CourseResponse> getCoursesByCategory(String categoryId);
}
