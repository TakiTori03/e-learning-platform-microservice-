package com.hust.courseservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import com.hust.courseservice.entity.enums.CourseAccess;
import com.hust.courseservice.entity.enums.LessonType;
import lombok.*;
import org.springframework.data.mongodb.core.index.TextIndexed;
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

    @TextIndexed
    private String name;
    private String description;
    private String content; 
    private Double videoLength;
    private CourseAccess access;
    private LessonType type; 
    private Integer position;
}
