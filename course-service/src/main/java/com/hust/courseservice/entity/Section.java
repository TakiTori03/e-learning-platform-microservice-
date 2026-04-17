package com.hust.courseservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import com.hust.courseservice.entity.enums.CourseAccess;
import lombok.*;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Section extends BaseDocument {
    private String courseId;

    @TextIndexed
    private String name;
    private String description;
    private CourseAccess access;
    private Integer position;
}
