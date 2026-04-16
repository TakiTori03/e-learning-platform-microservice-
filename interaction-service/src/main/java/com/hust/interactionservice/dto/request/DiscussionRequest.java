package com.hust.interactionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionRequest {
    private String courseId;
    private String lessonId;

    @NotBlank
    private String content;

    private String parentId; // Có thể null nếu là thảo luận gốc
}
