package com.hust.courseservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Section extends BaseDocument {
    private String courseId;
    private String name;
    private String description;
    private String access; 
    private Integer position;
}
