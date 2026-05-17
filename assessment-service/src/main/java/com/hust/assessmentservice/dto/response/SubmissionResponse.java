package com.hust.assessmentservice.dto.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionResponse {
    private String id;
    private String quizId;
    private String userId;
    private Double score;
    private Double totalMaxScore;
    private String status; // "SUBMITTED" or "GRADED"
    private Instant submittedAt;
}
