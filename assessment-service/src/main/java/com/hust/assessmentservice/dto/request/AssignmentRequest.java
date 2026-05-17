package com.hust.assessmentservice.dto.request;

import com.hust.assessmentservice.entity.enums.TargetType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentRequest {
    private String title;
    private String courseId;
    private String instructions;
    private String targetId;
    private TargetType targetType;
    private String attachmentFileKey;
    private Instant dueDate;
    private Double maxPoints;
}
