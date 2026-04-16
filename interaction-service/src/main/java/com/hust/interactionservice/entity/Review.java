package com.hust.interactionservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "reviews")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Review extends BaseDocument {
    private String code; // Lấy từ coreHelper.getCodeDefault("REVIEW")
    private String courseId;
    private String orderId;
    private String userId;
    private String title;
    private String content;
    private Double ratingStar; // Hỗ trợ 0.5 sao như monolith
    private Boolean isHidden;
}
