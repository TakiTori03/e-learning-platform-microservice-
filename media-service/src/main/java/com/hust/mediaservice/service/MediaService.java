package com.hust.mediaservice.service;

import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.mediaservice.dto.response.PresignedUrlResponse;
import com.hust.mediaservice.entity.Media;
import com.hust.mediaservice.repository.MediaRepository;
import com.hust.mediaservice.strategy.StorageStrategy;
import com.hust.mediaservice.entity.enums.MediaType;
import com.hust.mediaservice.entity.enums.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {
    private final MediaRepository mediaRepository;
    private final StorageStrategy storageStrategy;
    private final VideoProcessingService videoProcessingService;

    @Value("${app.storage.local-path:./uploads}")
    private String baseStoragePath;

    @Value("${app.minio.endpoint:}")
    private String minioEndpoint;

    @Value("${app.minio.bucket-name:}")
    private String bucketName;

    public Media upload(MultipartFile file, MediaType type, String referenceId) throws IOException {
        if (type == MediaType.VIDEO) {
            throw new IllegalArgumentException("Phát hiện nỗ lực tải video trực tiếp qua Gateway! Vui lòng sử dụng luồng tối ưu Presigned URL để tránh quá tải RAM.");
        }
        
        // 1. Initial save with READY status for normal files (images, PDFs)
        Media media = Media.builder()
                .fileName(file.getOriginalFilename())
                .fileType(type)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .provider(StorageProvider.S3)
                .ownerId(getUserId())
                .referenceId(referenceId)
                .status(Media.MediaStatus.READY)
                .build();
        
        media = mediaRepository.save(media);

        try {
            // 2. Direct lightweight file upload (Categorized by folder)
            String folderName = type.name().toLowerCase() + "s"; // "documents", "pdfs", "images"
            String url = storageStrategy.uploadFile(file, folderName);
            media.setUrl(url);
            return mediaRepository.save(media);
            
        } catch (Exception e) {
            log.error("Failed to process media: {}", media.getId(), e);
            media.setStatus(Media.MediaStatus.FAILED);
            mediaRepository.save(media);
            throw new IOException("Media processing failed", e);
        }
    }

    public Media getById(String id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", id));
    }

    public void deleteMedia(String id) throws IOException {
        Media media = getById(id);
        
        // 1. Xóa file chính trên MinIO
        if (media.getUrl() != null) {
            try {
                storageStrategy.deleteFile(media.getUrl());
            } catch (Exception e) {
                log.warn("Failed to delete main file from MinIO: {}", e.getMessage());
            }
        }
        
        // 2. Nếu là video, xóa raw file + thumbnail
        if (media.getRawFileKey() != null) {
            try {
                storageStrategy.deleteFile(media.getRawFileKey());
            } catch (Exception ignored) {}
        }
        if (media.getThumbnailUrl() != null) {
            try {
                storageStrategy.deleteFile(media.getThumbnailUrl());
            } catch (Exception ignored) {}
        }
        
        // 3. Xóa record trong MongoDB
        mediaRepository.delete(media);
        log.info("🗑️ Media {} deleted successfully.", id);
    }

    public Optional<Media> findByHlsFolderName(String hlsFolderName) {
        return mediaRepository.findByHlsFolderName(hlsFolderName);
    }

    public byte[] getVideoKey(String folderName) throws IOException {
        String keyPath = "hls/" + folderName + "/video.key"; 

        return storageStrategy.getFile(keyPath);
    }

    public PresignedUrlResponse generatePresignedUrlForVideo(String fileName, String contentType, long fileSize, String referenceId) {
        String rawFileKey = "raw-videos/" + UUID.randomUUID().toString() + "_" + fileName;
        
        log.info("Creating PENDING media asset. Requesting Presigned PUT URL for: {}", rawFileKey);
        String uploadUrl = storageStrategy.generatePresignedUploadUrl(rawFileKey, 15);
        
        Media media = Media.builder()
                .fileName(fileName)
                .fileType(MediaType.VIDEO)
                .contentType(contentType)
                .fileSize(fileSize)
                .provider(StorageProvider.S3)
                .ownerId(getUserId())
                .referenceId(referenceId)
                .status(Media.MediaStatus.PENDING)
                .rawFileKey(rawFileKey)
                .build();
        
        media = mediaRepository.save(media);
        
        return PresignedUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .mediaId(media.getId())
                .objectKey(rawFileKey)
                .build();
    }

    @Async("taskExecutor")
    public void processUploadedVideoAsync(String mediaId) {
        log.info("🚀 [ASYNC ENGINE] Starting HLS Background Job for Media: {}", mediaId);
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media record not found"));

        if (media.getStatus() != Media.MediaStatus.PENDING) {
            log.warn("Aborting async processing. Media {} is currently in {} state, expected PENDING.", mediaId, media.getStatus());
            return;
        }

        media.setStatus(Media.MediaStatus.PROCESSING);
        mediaRepository.save(media);
        
        File tempFile = null;
        String hlsFolderName = UUID.randomUUID().toString();
        Path localHlsPath = Paths.get(baseStoragePath, "hls", hlsFolderName);
        
        try {
            tempFile = File.createTempFile("transcode-raw-", "_" + media.getFileName());
            storageStrategy.downloadFileToLocal(media.getRawFileKey(), tempFile);
            
            // 3. Trigger FFMPEG HLS transcoding and thumbnail generation locally
            videoProcessingService.processToHls(tempFile, hlsFolderName);
            videoProcessingService.extractThumbnail(tempFile, hlsFolderName);
            
            // 4. Sync local FFMPEG HLS chunks folder to unified S3/MinIO Storage
            String remoteHlsPath = "hls/" + hlsFolderName;
            storageStrategy.uploadDirectory(localHlsPath.toString(), remoteHlsPath);
            
            // 5. Generate final Cloud-native URLs
            String url = String.format("%s/%s/%s/playlist.m3u8", minioEndpoint, bucketName, remoteHlsPath);
            String thumbnailUrl = String.format("%s/%s/%s/thumbnail.jpg", minioEndpoint, bucketName, remoteHlsPath);

            media.setUrl(url);
            media.setThumbnailUrl(thumbnailUrl);
            media.setHlsFolderName(hlsFolderName);
            media.setStatus(Media.MediaStatus.READY);
            mediaRepository.save(media);
            log.info("✅ [ASYNC ENGINE] HLS Transcoding COMPLETE for Media: {}", mediaId);
            
            try {
                storageStrategy.deleteFile(media.getRawFileKey());
                log.info("🧹 Reclaimed cloud storage by purging raw video: {}", media.getRawFileKey());
            } catch (Exception ex) {
                log.warn("Failed to delete raw temporary file from S3: {}", ex.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ [ASYNC ENGINE] Video Processing CRASHED for Media: {}", mediaId, e);
            media.setStatus(Media.MediaStatus.FAILED);
            mediaRepository.save(media);
        } finally {
            // Đảm bảo 100% dọn dẹp file tạm và thư mục tạm dù thành công hay thất bại!
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            try {
                if (java.nio.file.Files.exists(localHlsPath)) {
                    org.springframework.util.FileSystemUtils.deleteRecursively(localHlsPath);
                    log.info("🧹 Cleaned up local transcoding directory: {}", localHlsPath);
                }
            } catch (Exception ex) {
                log.error("Failed to clean up local transcoding directory: {}", localHlsPath, ex);
            }
        }
    }

    private String getUserId() {
        return SecurityUtils.getCurrentUserIdOrThrow();
    }
}
