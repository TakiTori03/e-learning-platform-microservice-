package com.hust.assessmentservice.dto.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmissionResponse {
    private String id;
    private String assignmentId;
    private String userId;
    private String submittedFileUrl;
    private Double grade;
    private String feedback;
    private String status;
    private Instant submittedAt;
}
