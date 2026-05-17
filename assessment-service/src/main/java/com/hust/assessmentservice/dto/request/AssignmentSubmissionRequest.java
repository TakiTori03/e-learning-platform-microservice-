package com.hust.assessmentservice.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmissionRequest {
    private String assignmentId;
    private String submittedFileUrl;
}
