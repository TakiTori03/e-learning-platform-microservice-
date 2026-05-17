package com.hust.assessmentservice.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerRequest {
    private String questionId;
    private String optionId;
}
