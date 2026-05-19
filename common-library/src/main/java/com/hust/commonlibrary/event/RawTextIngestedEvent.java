package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawTextIngestedEvent {
    private String courseId;
    private String lessonId;
    private String mediaId;
    private String content;
    private String contentType;      // "PDF" or "VIDEO"
    private String sourceCitation;    // "Trang 5" or "00:12" (timestamp)
}
