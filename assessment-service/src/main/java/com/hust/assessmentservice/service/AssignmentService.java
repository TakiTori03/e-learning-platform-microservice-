package com.hust.assessmentservice.service;

import com.hust.assessmentservice.dto.request.AssignmentRequest;
import com.hust.assessmentservice.dto.response.AssignmentResponse;
import com.hust.assessmentservice.entity.enums.TargetType;

public interface AssignmentService {
    AssignmentResponse createAssignment(AssignmentRequest request);
    AssignmentResponse updateAssignment(String id, AssignmentRequest request);
    AssignmentResponse getAssignmentById(String id);
    AssignmentResponse getAssignmentByTarget(String targetId, TargetType targetType);
}
