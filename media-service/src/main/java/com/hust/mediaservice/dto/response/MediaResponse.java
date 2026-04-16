package com.hust.mediaservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse extends TimeResponse {
    private String id;
    private String fileName;
    private String fileType;
    private String contentType;
    private Long fileSize;
    private String url;
    private String provider;
}
