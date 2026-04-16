package com.hust.courseservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lesson_progress")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "user_lesson_idx", def = "{'userId': 1, 'lessonId': 1}", unique = true)
public class LessonProgress extends BaseDocument {
    @Indexed
    private String userId;
    
    @Indexed
    private String lessonId;
    
    private boolean isDone;
}
