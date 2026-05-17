package com.hust.assessmentservice.resolver;

import com.hust.assessmentservice.entity.Assignment;
import com.hust.assessmentservice.entity.AssignmentSubmission;
import com.hust.assessmentservice.repository.AssignmentRepository;
import com.hust.assessmentservice.repository.AssignmentSubmissionRepository;
import com.hust.commonlibrary.resolver.CourseIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolver thông minh cho phép Aspect AOP tự động truy vết ngược từ 
 * ID bài nộp (SubmissionId) -> ID Bài tập (AssignmentId) -> ID Khóa học (CourseId).
 * Đảm bảo chỉ chủ sở hữu của Khóa học mới được quyền Chấm Điểm bài nộp!
 */
@Component("assignmentSubmissionResolver")
@RequiredArgsConstructor
public class AssignmentSubmissionCourseIdResolver implements CourseIdResolver {

    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;

    @Override
    public String resolveCourseId(String submissionId) {
        if (submissionId == null || submissionId.isBlank()) return null;

        // 1. Lấy thông tin bài nộp
        AssignmentSubmission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) return null;

        // 2. Truy vấn ngược ra Bài tập tương ứng để lấy ID khóa học gốc
        return assignmentRepository.findById(submission.getAssignmentId())
                .map(Assignment::getCourseId)
                .orElse(null);
    }
}
