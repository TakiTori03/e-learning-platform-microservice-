package com.hust.assessmentservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptionResponse {
    private String id;
    private String content;
    // NOTICE: We EXCLUDE 'isCorrect' to prevent students from cheating via browser inspect elements.
}
