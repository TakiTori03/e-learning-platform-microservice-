package com.hust.courseservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionRequest {
    private String name;
    private String description;
    private String access; 
    private Integer position;
    private String courseId;
}
