package com.hust.interactionservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionResponse extends TimeResponse {
    private String id;
    private String code;
    private String courseId;
    private String lessonId;
    private String userId;
    private String content;
    private String parentId;
    private String rootId;
    private List<DiscussionResponse> replies; // Hỗ trợ hiển thị dạng cây
}
