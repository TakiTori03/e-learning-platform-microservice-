package com.hust.assessmentservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.assessmentservice.dto.request.AssignmentRequest;
import com.hust.assessmentservice.dto.response.AssignmentResponse;
import com.hust.assessmentservice.entity.enums.TargetType;
import com.hust.assessmentservice.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(@RequestBody AssignmentRequest request) {
        AssignmentResponse response = assignmentService.createAssignment(request);
        return ResponseEntity.ok(
                ApiResponse.<AssignmentResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> updateAssignment(
            @PathVariable String id,
            @RequestBody AssignmentRequest request) {
        AssignmentResponse response = assignmentService.updateAssignment(id, request);
        return ResponseEntity.ok(
                ApiResponse.<AssignmentResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignmentById(@PathVariable String id) {
        AssignmentResponse response = assignmentService.getAssignmentById(id);
        return ResponseEntity.ok(
                ApiResponse.<AssignmentResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping("/target/{targetType}/{targetId}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignmentByTarget(
            @PathVariable TargetType targetType,
            @PathVariable String targetId) {
        AssignmentResponse response = assignmentService.getAssignmentByTarget(targetId, targetType);
        return ResponseEntity.ok(
                ApiResponse.<AssignmentResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }
}
