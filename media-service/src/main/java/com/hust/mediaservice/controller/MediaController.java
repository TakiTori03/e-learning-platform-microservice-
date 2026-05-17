package com.hust.mediaservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.mediaservice.client.LearningClient;
import com.hust.mediaservice.dto.response.MediaResponse;
import com.hust.mediaservice.dto.response.PresignedUrlResponse;

import com.hust.mediaservice.entity.Media;
import com.hust.mediaservice.entity.enums.MediaType;
import com.hust.mediaservice.mapper.MediaMapper;
import com.hust.mediaservice.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final MediaMapper mediaMapper;
    private final LearningClient learningClient;

    // ======================== QUERY ========================

    /**
     * Tra cứu trạng thái & thông tin chi tiết của một media asset.
     * Frontend dùng polling endpoint này để theo dõi quá trình transcode video
     * (PENDING → PROCESSING → READY / FAILED).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MediaResponse>> getMediaById(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<MediaResponse>builder()
                        .success(true)
                        .payload(mediaMapper.entityToResponse(mediaService.getById(id)))
                        .build()
        );
    }

    // ======================== UPLOAD ========================

    @PostMapping("/upload/image")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "referenceId", required = false) String referenceId) throws IOException {
        return ResponseEntity.ok(
                ApiResponse.<MediaResponse>builder()
                        .success(true)
                        .payload(mediaMapper.entityToResponse(mediaService.upload(file, MediaType.IMAGE, referenceId)))
                        .build()
        );
    }

    @PostMapping("/upload/pdf")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "referenceId", required = false) String referenceId) throws IOException {
        return ResponseEntity.ok(
                ApiResponse.<MediaResponse>builder()
                        .success(true)
                        .payload(mediaMapper.entityToResponse(mediaService.upload(file, MediaType.PDF, referenceId)))
                        .build()
        );
    }

    @PostMapping("/upload/document")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "referenceId", required = false) String referenceId) throws IOException {
        return ResponseEntity.ok(
                ApiResponse.<MediaResponse>builder()
                        .success(true)
                        .payload(mediaMapper.entityToResponse(mediaService.upload(file, MediaType.DOCUMENT, referenceId)))
                        .build()
        );
    }

    /**
     * High-Scale Endpoint: Request a Presigned temporary S3/MinIO URL to upload video
     * directly from browser, completely bypassing the API Gateway memory load.
     */
    @PostMapping("/upload/request-presigned-url")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> requestUploadUrl(
            @RequestParam("fileName") String fileName,
            @RequestParam("contentType") String contentType,
            @RequestParam("fileSize") Long fileSize,
            @RequestParam("referenceId") String lessonId) {
        
        return ResponseEntity.ok(
                ApiResponse.<PresignedUrlResponse>builder()
                        .success(true)
                        .payload(mediaService.generatePresignedUrlForVideo(fileName, contentType, fileSize, lessonId))
                        .build()
        );
    }

    /**
     * Trigger the background asynchronous transcoding processing engine for an uploaded video asset.
     * Front-end invokes this as soon as direct PUT to S3/Minio returns 200 OK.
     */
    @PostMapping("/process-video/{mediaId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> triggerAsyncProcessing(@PathVariable String mediaId) {
        mediaService.processUploadedVideoAsync(mediaId);
        
        return ResponseEntity.accepted().body(
                ApiResponse.<String>builder()
                        .success(true)
                        .payload("Nén HLS và mã hóa video đã được kích hoạt xử lý ngầm thành công.")
                        .build()
        );
    }

    // ======================== DELETE ========================

    /**
     * Xóa media asset: xóa file trên MinIO + xóa record trong MongoDB.
     * Chỉ INSTRUCTOR (chủ sở hữu) hoặc ADMIN mới được phép xóa.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteMedia(@PathVariable String id) throws IOException {
        mediaService.deleteMedia(id);
        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .payload("Media đã được xóa thành công.")
                        .build()
        );
    }

    // ======================== DRM / KEY ========================

    @GetMapping("/keys/{folderName}")
    public ResponseEntity<byte[]> getVideoKey(@PathVariable String folderName) throws IOException {
        Media media = mediaService.findByHlsFolderName(folderName)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "hlsFolderName", folderName));

        String lessonId = media.getReferenceId();
        if (lessonId == null) {
            throw new ResourceNotFoundException("Media không được liên kết với bài giảng nào.");
        }

        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        ApiResponse<Boolean> accessResponse = learningClient.checkLessonAccess(userId, lessonId);
        
        if (!accessResponse.isSuccess() || !accessResponse.getPayload()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(mediaService.getVideoKey(folderName));
    }
}
