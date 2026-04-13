package com.hust.courseservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lessons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lesson extends BaseDocument {
    private String sectionId;
    private String courseId;
    private String name;
    private String description;
    private String content; 
    private Double videoLength;
    private String access;
    private String type; 
    private Integer position;
}
