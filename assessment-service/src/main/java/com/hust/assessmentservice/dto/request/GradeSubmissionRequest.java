package com.hust.assessmentservice.dto.request;

import lombok.*;

/**
 * DTO chứa Payload chấm điểm bài tập từ phía Giáo viên.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeSubmissionRequest {
    
    /**
     * Điểm số gán cho học viên.
     */
    private Double grade;

    /**
     * Nhận xét chi tiết từ giáo viên.
     */
    private String feedback;
}
