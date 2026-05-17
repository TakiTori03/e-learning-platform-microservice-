package com.hust.assessmentservice.service.impl;

import com.hust.assessmentservice.dto.request.AssignmentSubmissionRequest;
import com.hust.assessmentservice.dto.request.GradeSubmissionRequest;
import com.hust.assessmentservice.dto.response.AssignmentSubmissionResponse;
import com.hust.assessmentservice.entity.Assignment;
import com.hust.assessmentservice.entity.AssignmentSubmission;
import com.hust.commonlibrary.annotation.CheckCourseOwner;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.assessmentservice.entity.enums.SubmissionStatus;
import com.hust.commonlibrary.event.AssessmentSubmittedEvent;
import com.hust.assessmentservice.mapper.AssignmentSubmissionMapper;
import com.hust.assessmentservice.repository.AssignmentRepository;
import com.hust.assessmentservice.repository.AssignmentSubmissionRepository;
import com.hust.assessmentservice.service.AssignmentSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentSubmissionServiceImpl implements AssignmentSubmissionService {

    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionMapper submissionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AssignmentSubmissionResponse submitAssignment(String userId, AssignmentSubmissionRequest request) {
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new RuntimeException("Assignment not found with id " + request.getAssignmentId()));


        AssignmentSubmission submission = submissionRepository.findByUserIdAndAssignmentId(userId, assignment.getId())
                .orElse(new AssignmentSubmission());

        submission.setAssignmentId(assignment.getId());
        submission.setUserId(userId);
        submission.setSubmittedFileUrl(request.getSubmittedFileUrl());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(Instant.now());

        AssignmentSubmission savedSubmission = submissionRepository.save(submission);

        // 🚀 FIRE EVENT TO KAFKA VIA LISTENER (Identical to Quiz logic)
        AssessmentSubmittedEvent localEvent = AssessmentSubmittedEvent.builder()
                .submissionId(savedSubmission.getId())
                .assessmentId(assignment.getId())
                .assessmentType("ASSIGNMENT")
                .userId(userId)
                .targetId(assignment.getTargetId())
                .targetType(assignment.getTargetType() != null ? assignment.getTargetType().name() : null)
                .submittedAt(savedSubmission.getSubmittedAt())
                .build();

        log.info("Shouting local AssessmentSubmittedEvent into Memory Bus for Assignment decoupling...");
        eventPublisher.publishEvent(localEvent);

        return submissionMapper.entityToResponse(savedSubmission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> getSubmissionsByUser(String userId) {
        return submissionRepository.findByUserId(userId).stream()
                .map(submissionMapper::entityToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> getSubmissionsByAssignment(String assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId).stream()
                .map(submissionMapper::entityToResponse)
                .toList();
    }

    @Override
    @Transactional
    @CheckCourseOwner(domainId = "#submissionId", resolver = "assignmentSubmissionResolver")
    public AssignmentSubmissionResponse gradeSubmission(String submissionId, GradeSubmissionRequest request) {
        // 1. Tìm kiếm bài nộp của học viên
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Assignment Submission not found with id " + submissionId));

        // 2. Truy vấn ngược bài tập gốc để so sánh thang điểm
        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new RuntimeException("Assignment not found with id " + submission.getAssignmentId()));

        // 3. Xác thực điểm số (Phòng vệ Input Validation)
        if (request.getGrade() != null) {
            Double maxPoints = assignment.getMaxPoints() != null ? assignment.getMaxPoints() : 10.0;
            if (request.getGrade() < 0 || request.getGrade() > maxPoints) {
                throw new RuntimeException("Grade must be between 0 and " + maxPoints);
            }
        }

        // 4. Cập nhật điểm số & Trạng thái
        submission.setGrade(request.getGrade());
        submission.setFeedback(request.getFeedback());
        submission.setStatus(SubmissionStatus.GRADED);

        AssignmentSubmission updated = submissionRepository.save(submission);
        log.info("🎯 Graded assignment submission [{}] by user [{}]. Grade: {}", 
                submissionId, updated.getUserId(), updated.getGrade());

        return submissionMapper.entityToResponse(updated);
    }
}
