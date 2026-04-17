package com.hust.learningservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressResponse {
    private String userId;
    private String courseId;
    private List<String> finishedLessonIds;
    private Double progressPercentage;
    private Boolean isEnrolled;
}
