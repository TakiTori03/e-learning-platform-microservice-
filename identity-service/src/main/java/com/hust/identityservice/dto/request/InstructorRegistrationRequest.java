package com.hust.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InstructorRegistrationRequest extends UserRegistrationRequest {
    
    @NotBlank(message = "FIELD_REQUIRED")
    private String headline;
    
    @NotBlank(message = "FIELD_REQUIRED")
    private String biography;
    
    private String experience; // Optional field for review
    
    private String portfolioUrl;
}
