package com.hust.assessmentservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.assessmentservice.dto.request.AssignmentSubmissionRequest;
import com.hust.assessmentservice.dto.request.GradeSubmissionRequest;
import com.hust.assessmentservice.dto.response.AssignmentSubmissionResponse;
import com.hust.assessmentservice.service.AssignmentSubmissionService;
import com.hust.commonlibrary.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/assignment-submissions")
@RequiredArgsConstructor
public class AssignmentSubmissionController {

    private final AssignmentSubmissionService submissionService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentSubmissionResponse>> submitAssignment(
            @RequestBody AssignmentSubmissionRequest request) {
        
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        AssignmentSubmissionResponse response = submissionService.submitAssignment(userId, request);
        
        return ResponseEntity.ok(
                ApiResponse.<AssignmentSubmissionResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<AssignmentSubmissionResponse>>> getMySubmissions() {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<AssignmentSubmissionResponse> response = submissionService.getSubmissionsByUser(userId);
        return ResponseEntity.ok(
                ApiResponse.<List<AssignmentSubmissionResponse>>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<AssignmentSubmissionResponse>>> getSubmissionsByAssignment(
            @PathVariable String assignmentId) {
        List<AssignmentSubmissionResponse> response = submissionService.getSubmissionsByAssignment(assignmentId);
        return ResponseEntity.ok(
                ApiResponse.<List<AssignmentSubmissionResponse>>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }
    @PutMapping("/{submissionId}/grade")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AssignmentSubmissionResponse>> gradeSubmission(
            @PathVariable String submissionId,
            @RequestBody GradeSubmissionRequest request) {
        
        AssignmentSubmissionResponse response = submissionService.gradeSubmission(submissionId, request);
        return ResponseEntity.ok(
                ApiResponse.<AssignmentSubmissionResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }
}
