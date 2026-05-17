package com.hust.interactionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeedbackRequest {
    
    private String type;

    @NotBlank
    private String title;
    
    @NotBlank
    private String content;
}
