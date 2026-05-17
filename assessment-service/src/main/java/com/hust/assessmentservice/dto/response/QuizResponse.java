package com.hust.assessmentservice.dto.response;

import com.hust.assessmentservice.entity.enums.TargetType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResponse {
    private String id;
    private String title;
    private String courseId;
    private String description;
    private String targetId;
    private TargetType targetType;
    private Integer timeLimit;
    private List<QuestionResponse> questions;
}
