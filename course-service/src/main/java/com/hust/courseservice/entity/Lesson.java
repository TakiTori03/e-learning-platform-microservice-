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

    /**
     * 📝 The Dynamic Content Payload depending on the LessonType:
     * - VIDEO      ->  Lecture Notes, Markdown Transcripts, External GitHub Links. (Stream is in mediaId)
     * - DOCUMENT   ->  The main Rich Text / HTML reading material.
     * - QUIZ       -> Quiz guidelines, passing rules, or JSON configuration.
     * - CODING     ->  Problem statement, coding instructions, and skeleton boilerplate.
     * - ASSIGNMENT ->  Detailed project specification, grading rubric, and download templates.
     */
    private String content; 
    private Double videoLength;

    private CourseAccess access;
    private LessonType type; 
    private Integer position;
    private String mediaId;
}
