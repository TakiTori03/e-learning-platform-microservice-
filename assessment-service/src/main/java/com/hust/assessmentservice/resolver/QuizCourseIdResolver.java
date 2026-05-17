package com.hust.assessmentservice.resolver;

import com.hust.assessmentservice.entity.Quiz;
import com.hust.assessmentservice.repository.QuizRepository;
import com.hust.commonlibrary.resolver.CourseIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Chịu trách nhiệm tự động truy vấn DB cục bộ để lấy courseId THẬT từ ID Đề trắc nghiệm (Quiz).
 * Tương tác trực tiếp với Aspect động.
 */
@Component("quizResolver")
@RequiredArgsConstructor
public class QuizCourseIdResolver implements CourseIdResolver {

    private final QuizRepository quizRepository;

    @Override
    public String resolveCourseId(String domainId) {
        if (domainId == null || domainId.isBlank()) return null;

        return quizRepository.findById(domainId)
                .map(Quiz::getCourseId)
                .orElse(null);
    }
}
