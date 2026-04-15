package com.hust.courseservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse extends TimeResponse {
    private String id;
    private String name;
    private String description;
    
    // Monolith compatibility
    private String cateSlug;
    private String cateImage;
    private String cateParent;
    
    // Original fields (optional to keep)
    private String slug;
    private String icon;
}
