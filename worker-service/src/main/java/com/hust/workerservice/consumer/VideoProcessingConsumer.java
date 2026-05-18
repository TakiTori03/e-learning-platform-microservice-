package com.hust.workerservice.consumer;

import com.hust.commonlibrary.constant.KafkaTopics;
import com.hust.commonlibrary.event.LessonMediaReadyEvent;
import com.hust.commonlibrary.event.MediaProcessingRequestEvent;
import com.hust.commonlibrary.annotation.TrackPerformance;
import com.hust.workerservice.service.WhisperLocalSpeechToTextService;
import com.hust.workerservice.service.VideoProcessingService;
import com.hust.workerservice.strategy.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoProcessingConsumer {

    private final VideoProcessingService videoProcessingService;
    private final StorageStrategy storageStrategy;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WhisperLocalSpeechToTextService whisperLocalSpeechToTextService;

    @Value("${app.storage.local-path:./uploads}")
    private String baseStoragePath;

    @Value("${app.minio.endpoint:}")
    private String minioEndpoint;

    @Value("${app.minio.bucket-name:}")
    private String bucketName;

    @KafkaListener(topics = KafkaTopics.MEDIA_PROCESSING, groupId = "video-worker-group")
    @TrackPerformance(threshold = 300000, description = "Video HLS Transcoding & Subtitle Generation Pipeline")
    public void consume(MediaProcessingRequestEvent event) {
        if (!"VIDEO".equalsIgnoreCase(event.getMediaType())) {
            return; // Ignore non-video assets in this worker
        }
        
        log.info("🎬 Received VideoProcessingRequestEvent from Kafka: {}", event);
        
        File tempFile = null;
        String hlsFolderName = UUID.randomUUID().toString();
        Path localHlsPath = Paths.get(baseStoragePath, "hls", hlsFolderName);

        try {
            // 1. Create local temp file & Download raw video from MinIO
            tempFile = File.createTempFile("worker-transcode-raw-", ".mp4");
            storageStrategy.downloadFileToLocal(event.getFileUrl(), tempFile);

            // 2. Transcode to HLS & Extract Thumbnail
            videoProcessingService.processToHls(tempFile, hlsFolderName);
            videoProcessingService.extractThumbnail(tempFile, hlsFolderName);

            // 3. Extract Audio (MP3)
            String relativeAudioPath = videoProcessingService.extractAudio(tempFile, hlsFolderName);
            File audioFile = Paths.get(baseStoragePath, relativeAudioPath).toFile();

            // 4. Transcribe audio to WebVTT via Gemini API
            File vttFile = createVttFile(hlsFolderName, audioFile);

            // 5. Sync everything to MinIO
            String remoteHlsPath = "hls/" + hlsFolderName;
            storageStrategy.uploadDirectory(localHlsPath.toString(), remoteHlsPath);

            // 6. Generate Public Cloud URLs
            String url = String.format("%s/%s/%s/playlist.m3u8", minioEndpoint, bucketName, remoteHlsPath);
            String thumbnailUrl = String.format("%s/%s/%s/thumbnail.jpg", minioEndpoint, bucketName, remoteHlsPath);
            String transcriptUrl = String.format("%s/%s/%s/subtitles.vtt", minioEndpoint, bucketName, remoteHlsPath);

            // 7. Publish LessonMediaReadyEvent back to Kafka
            long fileSize = tempFile.length();
            Double duration = videoProcessingService.getVideoDuration(tempFile);
            
            LessonMediaReadyEvent readyEvent = LessonMediaReadyEvent.builder()
                    .lessonId(event.getLessonId())
                    .mediaId(event.getMediaId())
                    .hlsFolderName(hlsFolderName)
                    .transcriptUrl(transcriptUrl)
                    .url(url)
                    .thumbnailUrl(thumbnailUrl)
                    .fileSize(fileSize)
                    .duration(duration)
                    .build();

            kafkaTemplate.send(KafkaTopics.LESSON_MEDIA_READY, event.getLessonId(), readyEvent);
            log.info("✅ Successfully processed video (Size: {} bytes, Duration: {} sec) and sent LessonMediaReadyEvent for lesson {}", fileSize, duration, event.getLessonId());

            // 8. Clean up raw video from MinIO to save storage space
            try {
                storageStrategy.deleteFile(event.getFileUrl());
                log.info("🗑️ Cleaned up raw video from MinIO: {}", event.getFileUrl());
            } catch (Exception ex) {
                log.warn("⚠️ Failed to delete raw video {} from MinIO: {}", event.getFileUrl(), ex.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ Error processing video {}: {}", event.getMediaId(), e.getMessage(), e);
        } finally {
            // Clean up temporary resources
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            if (Files.exists(localHlsPath)) {
                FileSystemUtils.deleteRecursively(localHlsPath.toFile());
                log.info("🧹 Cleaned up local transcoding directory: {}", localHlsPath);
            }
        }
    }

    private File createVttFile(String hlsFolderName, File audioFile) throws IOException {
        Path vttPath = Paths.get(baseStoragePath, "hls", hlsFolderName, "subtitles.vtt");
        Files.createDirectories(vttPath.getParent());
        File vttFile = vttPath.toFile();

        // 1. Obtain real WebVTT content from local Whisper.cpp
        String vttContent = whisperLocalSpeechToTextService.transcribeAudioToVtt(audioFile);

        // 2. Save the WebVTT file locally
        Files.write(vttPath, vttContent.getBytes());
        log.info("💾 Saved WebVTT file locally: {}", vttFile.getAbsolutePath());

        return vttFile;
    }
}
