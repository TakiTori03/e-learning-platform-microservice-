package com.hust.assessmentservice.service;

import com.hust.assessmentservice.dto.request.AssignmentSubmissionRequest;
import com.hust.assessmentservice.dto.request.GradeSubmissionRequest;
import com.hust.assessmentservice.dto.response.AssignmentSubmissionResponse;

import java.util.List;

public interface AssignmentSubmissionService {
    AssignmentSubmissionResponse submitAssignment(String userId, AssignmentSubmissionRequest request);
    List<AssignmentSubmissionResponse> getSubmissionsByUser(String userId);
    List<AssignmentSubmissionResponse> getSubmissionsByAssignment(String assignmentId);
    AssignmentSubmissionResponse gradeSubmission(String submissionId, GradeSubmissionRequest request);
}
