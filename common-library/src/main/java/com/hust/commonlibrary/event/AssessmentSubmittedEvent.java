package com.hust.commonlibrary.event;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentSubmittedEvent {
    private String submissionId;
    private String assessmentId;
    private String assessmentType;
    private String userId;
    private String targetId;
    private String targetType;
    private Instant submittedAt;
}
