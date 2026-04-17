package com.hust.learningservice.service.impl;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.learningservice.client.CourseClient;
import com.hust.learningservice.dto.request.LessonProgressRequest;
import com.hust.learningservice.dto.response.CourseProgressResponse;
import com.hust.learningservice.entity.LessonProgress;
import com.hust.learningservice.entity.StudentEnrollment;
import com.hust.learningservice.repository.EnrollmentRepository;
import com.hust.learningservice.repository.ProgressRepository;
import com.hust.learningservice.service.LearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningServiceImpl implements LearningService {

    private final EnrollmentRepository enrollmentRepository;
    private final ProgressRepository progressRepository;
    private final CourseClient courseClient;

    @Override
    @Transactional
    public void trackProgress(String userId, LessonProgressRequest request) {
        LessonProgress progress = progressRepository.findByUserIdAndCourseIdAndLessonId(
                userId, request.getCourseId(), request.getLessonId())
                .orElse(LessonProgress.builder()
                        .userId(userId)
                        .courseId(request.getCourseId())
                        .lessonId(request.getLessonId())
                        .build());

        progress.setIsDone(request.getIsDone());
        if (request.getLastWatchedTime() != null) {
            progress.setLastWatchedTime(request.getLastWatchedTime());
        }
        progressRepository.save(progress);

        recalculatePercentage(userId, request.getCourseId());
    }

    private void recalculatePercentage(String userId, String courseId) {
        StudentEnrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new RuntimeException("Student not enrolled in course"));

        long totalLessons = 0;
        try {
            ApiResponse<Long> countRes = courseClient.getLessonCount(courseId);
            if (countRes != null && countRes.isSuccess() && countRes.getPayload() != null) {
                totalLessons = countRes.getPayload();
            }
        } catch (Exception e) {
            log.error("Failed to fetch lesson count: {}", e.getMessage());
            return;
        }

        if (totalLessons == 0) return;

        List<LessonProgress> list = progressRepository.findByUserIdAndCourseId(userId, courseId);
        long finishedCount = list.stream()
                .filter(p -> p.getIsDone() != null && p.getIsDone())
                .count();

        double percentage = (double) finishedCount / totalLessons * 100;
        enrollment.setProgress(Math.min(100.0, percentage));
        
        if (percentage >= 100.0) {
            enrollment.setIsCompleted(true);
            enrollment.setCompletedAt(java.time.Instant.now());
        }

        enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(String userId, String courseId) {
        Optional<StudentEnrollment> enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
        
        if (enrollment.isEmpty()) {
            return CourseProgressResponse.builder()
                    .userId(userId)
                    .courseId(courseId)
                    .isEnrolled(false)
                    .build();
        }

        List<LessonProgress> progressList = progressRepository.findByUserIdAndCourseId(userId, courseId);
        List<String> finishedLessonIds = progressList.stream()
                .filter(p -> p.getIsDone() != null && p.getIsDone())
                .map(LessonProgress::getLessonId)
                .toList();

        return CourseProgressResponse.builder()
                .userId(userId)
                .courseId(courseId)
                .isEnrolled(true)
                .finishedLessonIds(finishedLessonIds)
                .progressPercentage(enrollment.get().getProgress())
                .build();
    }

    @Override
    @Transactional
    public void enrollStudentBulk(String userId, List<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) return;

        List<StudentEnrollment> existing = enrollmentRepository.findAllByUserIdAndCourseIdIn(userId, courseIds);
        Set<String> existingCourseIds = existing.stream()
                .map(StudentEnrollment::getCourseId)
                .collect(Collectors.toSet());

        List<StudentEnrollment> newEnrollments = courseIds.stream()
                .filter(id -> !existingCourseIds.contains(id))
                .distinct()
                .map(courseId -> StudentEnrollment.builder()
                        .userId(userId)
                        .courseId(courseId)
                        .progress(0.0)
                        .isCompleted(false)
                        .enrollmentDate(java.time.Instant.now())
                        .build())
                .collect(Collectors.toList());

        if (!newEnrollments.isEmpty()) {
            enrollmentRepository.saveAll(newEnrollments);
            log.info("Bulk enrolled user {} in {} new courses", userId, newEnrollments.size());
        }
    }
}
