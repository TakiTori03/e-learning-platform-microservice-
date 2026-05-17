package com.hust.assessmentservice.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionResponse {
    private String id;
    private String content;
    private Integer point;
    private Integer orderIndex;
    private List<OptionResponse> options;
}
