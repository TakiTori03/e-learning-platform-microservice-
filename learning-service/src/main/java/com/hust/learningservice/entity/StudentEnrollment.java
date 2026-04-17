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

    private Instant enrollmentDate;
    
    private Double progress; // 0.0 to 100.0
    
    private Boolean isCompleted;
    private Instant completedAt;
}
