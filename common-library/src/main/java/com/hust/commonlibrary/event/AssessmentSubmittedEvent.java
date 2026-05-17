package com.hust.commonlibrary.event;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentSubmittedEvent {
    private String submissionId;
    private String assessmentId; // assignmentId or quizId
    private String assessmentType; // "ASSIGNMENT" or "QUIZ"
    private String userId;
    private String targetId; // lessonId
    private String targetType; // "LESSON"
    private Instant submittedAt;
}
