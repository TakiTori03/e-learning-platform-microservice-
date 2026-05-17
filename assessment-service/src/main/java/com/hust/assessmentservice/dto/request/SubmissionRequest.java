package com.hust.assessmentservice.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionRequest {
    private String quizId;
    private List<AnswerRequest> answers;
}
