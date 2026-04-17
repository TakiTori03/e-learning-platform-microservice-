package com.hust.interactionservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "discussions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Discussion extends BaseDocument {
    private String code;
    private String courseId; // Monolith có courseId
    private String sectionId;
    private String lessonId;
    private String userId;
    private String content; // Tương ứng với 'comments' bên Monolith
    private String parentId;
    private String rootId;
}
