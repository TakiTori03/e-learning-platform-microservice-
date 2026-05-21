package com.hust.aiservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Course ID không được để trống")
    private String courseId;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String message;
}
