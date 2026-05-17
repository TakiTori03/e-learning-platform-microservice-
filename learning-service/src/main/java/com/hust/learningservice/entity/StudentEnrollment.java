package com.hust.learningservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "student_enrollments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StudentEnrollment extends BaseDocument {

    @Indexed
    private String userId;

    @Indexed
    private String courseId;

    private String orderId;
    private Double progress;
    
    private Boolean isCompleted;
    private Instant completedAt;

    private String lastAccessedLessonId;
}
