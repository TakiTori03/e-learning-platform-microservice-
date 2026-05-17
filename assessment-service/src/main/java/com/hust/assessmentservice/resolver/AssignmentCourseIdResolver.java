package com.hust.assessmentservice.resolver;

import com.hust.assessmentservice.entity.Assignment;
import com.hust.assessmentservice.repository.AssignmentRepository;
import com.hust.commonlibrary.resolver.CourseIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Chịu trách nhiệm tự động truy vấn DB cục bộ để lấy courseId THẬT từ ID Bài tập.
 * Được gọi ngầm bởi Aspect trong Common Library.
 */
@Component("assignmentResolver")
@RequiredArgsConstructor
public class AssignmentCourseIdResolver implements CourseIdResolver {

    private final AssignmentRepository assignmentRepository;

    @Override
    public String resolveCourseId(String domainId) {
        if (domainId == null || domainId.isBlank()) return null;
        
        return assignmentRepository.findById(domainId)
                .map(Assignment::getCourseId)
                .orElse(null);
    }
}
