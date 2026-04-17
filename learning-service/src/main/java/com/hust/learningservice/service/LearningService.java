package com.hust.learningservice.service;

import com.hust.learningservice.dto.request.LessonProgressRequest;
import com.hust.learningservice.dto.response.CourseProgressResponse;

import java.util.List;

public interface LearningService {
    void trackProgress(String userId, LessonProgressRequest request);
    CourseProgressResponse getCourseProgress(String userId, String courseId);
    void enrollStudentBulk(String userId, List<String> courseIds);
}
