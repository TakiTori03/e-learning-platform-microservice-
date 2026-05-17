package com.hust.assessmentservice.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptionRequest {
    private String content;
    private boolean isCorrect;
}
