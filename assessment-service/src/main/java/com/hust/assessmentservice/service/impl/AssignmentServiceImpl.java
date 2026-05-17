package com.hust.assessmentservice.service.impl;

import com.hust.assessmentservice.dto.request.AssignmentRequest;
import com.hust.assessmentservice.dto.response.AssignmentResponse;
import com.hust.assessmentservice.entity.Assignment;
import com.hust.assessmentservice.entity.enums.TargetType;
import com.hust.assessmentservice.mapper.AssignmentMapper;
import com.hust.assessmentservice.repository.AssignmentRepository;
import com.hust.assessmentservice.service.AssignmentService;
import com.hust.commonlibrary.annotation.CheckCourseOwner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentMapper assignmentMapper;

    @Override
    @Transactional
    @CheckCourseOwner(courseId = "#request.courseId") 
    public AssignmentResponse createAssignment(AssignmentRequest request) {
        Assignment assignment = assignmentMapper.requestToEntity(request);
        return assignmentMapper.entityToResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    @CheckCourseOwner(domainId = "#id", resolver = "assignmentResolver")
    public AssignmentResponse updateAssignment(String id, AssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found with id " + id));
        
        assignmentMapper.partialUpdate(assignment, request);

        return assignmentMapper.entityToResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(String id) {
        return assignmentRepository.findById(id)
                .map(assignmentMapper::entityToResponse)
                .orElseThrow(() -> new RuntimeException("Assignment not found with id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentByTarget(String targetId, TargetType targetType) {
        return assignmentRepository.findByTargetIdAndTargetType(targetId, targetType)
                .map(assignmentMapper::entityToResponse)
                .orElseThrow(() -> new RuntimeException("Assignment not found for target " + targetId));
    }
}
