package com.hust.courseservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import com.hust.courseservice.entity.enums.ActionLogType;
import com.hust.courseservice.entity.enums.FunctionType;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "action_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionLog extends BaseDocument {
    private String userId;
    private String createdByName;
    private String courseId;
    private String sectionId;
    private String lessonId;
    private String categoryId;
    
    private String description;
    private ActionLogType type;
    private FunctionType functionType;
}
