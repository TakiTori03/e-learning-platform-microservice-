package com.hust.assessmentservice.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionRequest {
    private String content;
    private Double point;
    private Integer orderIndex;
    private List<OptionRequest> options;
}
