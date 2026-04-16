package com.hust.mediaservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.mediaservice.dto.response.MediaResponse;
import com.hust.mediaservice.entity.enums.MediaType;
import com.hust.mediaservice.mapper.MediaMapper;
import com.hust.mediaservice.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final MediaMapper mediaMapper;

    @PostMapping("/upload/image")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(
                ApiResponse.<MediaResponse>builder()
                        .success(true)
                        .payload(mediaMapper.entityToResponse(mediaService.upload(file, MediaType.IMAGE)))
                        .build()
        );
    }

    @PostMapping("/upload/video")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadVideo(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(
                ApiResponse.<MediaResponse>builder()
                        .success(true)
                        .payload(mediaMapper.entityToResponse(mediaService.upload(file, MediaType.VIDEO)))
                        .build()
        );
    }

    @PostMapping("/upload/pdf")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(
                ApiResponse.<MediaResponse>builder()
                        .success(true)
                        .payload(mediaMapper.entityToResponse(mediaService.upload(file, MediaType.PDF)))
                        .build()
        );
    }
}
