package com.hust.courseservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import com.hust.courseservice.entity.enums.CourseAccess;
import com.hust.courseservice.entity.enums.CourseLevel;
import com.hust.courseservice.entity.enums.CourseStatus;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.List;

@Document(collection = "courses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course extends BaseDocument {

    @Indexed(unique = true)
    private String code;

    @TextIndexed
    private String name;

    private String subTitle;

    private String thumbnail;

    private CourseAccess access; 

    private String coursePreview;

    @Builder.Default
    private Integer views = 0;

    private Double price;

    private Double finalPrice;

    @TextIndexed
    private String description;

    private CourseLevel level;

    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    @Indexed(unique = true)
    private String courseSlug;

    private String instructorId;

    @DocumentReference
    private Category category;

    private List<String> requirements;

    private List<String> willLearns;

    private List<String> tags;
}
