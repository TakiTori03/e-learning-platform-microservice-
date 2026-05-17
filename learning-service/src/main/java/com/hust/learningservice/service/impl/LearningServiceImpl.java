package com.hust.learningservice.service.impl;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.LessonInternalResponse;
import com.hust.learningservice.client.CourseClient;
import com.hust.learningservice.dto.request.LessonProgressRequest;
import com.hust.learningservice.dto.response.CourseProgressResponse;
import com.hust.learningservice.dto.response.CourseNoteResponse;
import com.hust.learningservice.entity.LessonProgress;
import com.hust.learningservice.entity.StudentEnrollment;
import com.hust.learningservice.entity.CourseNote;
import com.hust.learningservice.mapper.LessonProgressMapper;
import com.hust.learningservice.mapper.NoteMapper;
import com.hust.learningservice.mapper.ProgressMapper;
import com.hust.learningservice.repository.EnrollmentRepository;
import com.hust.learningservice.repository.ProgressRepository;
import com.hust.learningservice.repository.CourseNoteRepository;
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
    private final CourseNoteRepository noteRepository;
    private final CourseClient courseClient;
    private final NoteMapper noteMapper;
    private final ProgressMapper progressMapper;
    private final LessonProgressMapper lessonProgressMapper;

    @Override
    @Transactional
    public void trackProgress(String userId, LessonProgressRequest request) {
        LessonProgress progress = progressRepository.findByUserIdAndCourseIdAndLessonId(
                userId, request.getCourseId(), request.getLessonId())
                .orElseGet(() -> {
                    LessonProgress newProgress = lessonProgressMapper.requestToEntity(request);
                    newProgress.setUserId(userId);
                    return newProgress;
                });

        // Sử dụng partialUpdate từ BaseMapper để cập nhật các trường từ request
        lessonProgressMapper.partialUpdate(progress, request);
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
        Optional<StudentEnrollment> enrollmentOpt = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
        
        if (enrollmentOpt.isEmpty()) {
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

        CourseProgressResponse response = progressMapper.entityToResponse(enrollmentOpt.get());
        response.setFinishedLessonIds(finishedLessonIds);
        
        return response;
    }

    @Override
    @Transactional
    public void updateLastAccessedLesson(String userId, String courseId, String lessonId) {
        enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .ifPresent(enrollment -> {
                    enrollment.setLastAccessedLessonId(lessonId);
                    enrollmentRepository.save(enrollment);
                    log.debug("Updated last accessed lesson to {} for user {} in course {}", lessonId, userId, courseId);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, CourseProgressResponse> getCourseProgressBulk(String userId, List<String> courseIds) {
        List<StudentEnrollment> enrollments = enrollmentRepository.findAllByUserIdAndCourseIdIn(userId, courseIds);
        List<LessonProgress> allProgress = progressRepository.findAllByUserIdAndCourseIdIn(userId, courseIds);

        java.util.Map<String, List<LessonProgress>> progressByCourse = allProgress.stream()
                .collect(Collectors.groupingBy(LessonProgress::getCourseId));

        java.util.Map<String, CourseProgressResponse> resultMap = new java.util.HashMap<>();
        
        for (StudentEnrollment enrollment : enrollments) {
            CourseProgressResponse response = progressMapper.entityToResponse(enrollment);
            List<LessonProgress> courseProgress = progressByCourse.getOrDefault(enrollment.getCourseId(), List.of());
            
            List<String> finishedLessonIds = courseProgress.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsDone()))
                    .map(LessonProgress::getLessonId)
                    .toList();
            
            response.setFinishedLessonIds(finishedLessonIds);
            resultMap.put(enrollment.getCourseId(), response);
        }

        // Đảm bảo những khóa học không có enrollment vẫn có response cơ bản (isEnrolled = false)
        courseIds.forEach(id -> resultMap.putIfAbsent(id, CourseProgressResponse.builder()
                .userId(userId)
                .courseId(id)
                .isEnrolled(false)
                .build()));

        return resultMap;
    }

    @Override
    @Transactional
    public void enrollStudentBulk(String userId, List<String> courseIds, String orderId) {
        if (courseIds == null || courseIds.isEmpty()) return;

        // Check idempotency: Nếu đã có enrollment cho Order này rồi thì bỏ qua việc save nhưng vẫn tiếp tục để Consumer bắn lại event
        if (orderId != null && enrollmentRepository.existsByUserIdAndOrderId(userId, orderId)) {
            log.info("Order {} already processed for user {}, enrollment records already exist", orderId, userId);
            return;
        }

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
                        .orderId(orderId)
                        .progress(0.0)
                        .isCompleted(false)
                        .build())
                .collect(Collectors.toList());

        if (!newEnrollments.isEmpty()) {
            enrollmentRepository.saveAll(newEnrollments);
            log.info("Bulk enrolled user {} in {} new courses for Order {}", userId, newEnrollments.size(), orderId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccess(String userId, String courseId) {
        return enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentEnrollment> getEnrolledCourses(String userId) {
        return enrollmentRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkLessonAccess(String userId, String lessonId) {
        ApiResponse<LessonInternalResponse> lessonRes = courseClient.getLessonDetail(lessonId);
        if (lessonRes == null || !lessonRes.isSuccess() || lessonRes.getPayload() == null) {
            return false;
        }

        LessonInternalResponse lesson = lessonRes.getPayload();
        
        // Nếu bài học là FREE, cho phép truy cập luôn
        if ("FREE".equalsIgnoreCase(lesson.getAccess())) {
            return true;
        }

        // Nếu là PAID, kiểm tra Enrollment
        return enrollmentRepository.existsByUserIdAndCourseId(userId, lesson.getCourseId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseNoteResponse> getMyNotes(String userId, String courseId) {
        List<CourseNote> notes = noteRepository.findAllByUserIdAndCourseId(userId, courseId);
        return noteMapper.entityToResponse(notes);
    }

    @Override
    @Transactional
    public void addNote(String userId, com.hust.learningservice.dto.request.CourseNoteRequest request) {
        CourseNote note = noteMapper.requestToEntity(request);
        note.setUserId(userId);
        noteRepository.save(note);
    }

    @Override
    @Transactional
    public void deleteNote(String noteId) {
        noteRepository.deleteById(noteId);
    }

    @Override
    @Transactional
    public void completeQuizLesson(String userId, String lessonId) {
        log.info("Initializing automated completion sequence for Lesson Quiz: {}", lessonId);

        // 1. Call Feign client to determine which Course owns this Quiz Lesson
        ApiResponse<LessonInternalResponse> lessonRes = courseClient.getLessonDetail(lessonId);
        if (lessonRes == null || !lessonRes.isSuccess() || lessonRes.getPayload() == null) {
            log.error("CRITICAL SYNC ERROR: Could not resolve Course ID for Lesson {} via Feign. Aborting progress sync.", lessonId);
            return;
        }
        
        String courseId = lessonRes.getPayload().getCourseId();
        log.info("Resolved Quiz Lesson {} to Parent Course {}. Mapping completion event...", lessonId, courseId);

        // 2. Map to core LessonProgressRequest to reuse calculation engine
        LessonProgressRequest syncRequest = LessonProgressRequest.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .isDone(true)
                .build();

        // 3. Invoke core track progress to save record and trigger overall course percent recalculation!
        trackProgress(userId, syncRequest);
    }
}
