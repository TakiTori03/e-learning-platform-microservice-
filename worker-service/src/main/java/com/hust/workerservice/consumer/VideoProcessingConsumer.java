package com.hust.workerservice.consumer;

import com.hust.commonlibrary.constant.KafkaTopics;
import com.hust.commonlibrary.event.LessonMediaReadyEvent;
import com.hust.commonlibrary.event.MediaProcessingRequestEvent;
import com.hust.commonlibrary.entity.ContentType;
import com.hust.commonlibrary.event.RawTextIngestedEvent;
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

    @KafkaListener(topics = KafkaTopics.MEDIA_PROCESSING, groupId = "video-worker-group", concurrency = "1")
    @TrackPerformance(threshold = 300000, description = "Video HLS Transcoding & Whisper Subtitle Ingestion Pipeline")
    public void consume(MediaProcessingRequestEvent event) {
        if (!"VIDEO".equalsIgnoreCase(event.getMediaType())) {
            return;
        }
        
        log.info("🎬 Nhận sự kiện xử lý Video từ Kafka: {}", event);
        
        File tempFile = null;
        String hlsFolderName = UUID.randomUUID().toString();
        Path localHlsPath = Paths.get(baseStoragePath, "hls", hlsFolderName);

        try {
            // 1. Tạo tệp tạm và tải video gốc từ MinIO về máy cục bộ
            tempFile = File.createTempFile("worker-transcode-raw-", ".mp4");
            storageStrategy.downloadFileToLocal(event.getFileUrl(), tempFile);

            // 2. Chuyển đổi sang định dạng HLS (phân đoạn .ts) & Trích xuất ảnh thu nhỏ (Thumbnail)
            videoProcessingService.processToHls(tempFile, hlsFolderName);

            // 3. Tách luồng âm thanh siêu nhẹ dạng MP3 16kHz
            String relativeAudioPath = videoProcessingService.extractAudio(tempFile, hlsFolderName);
            File audioFile = Paths.get(baseStoragePath, relativeAudioPath).toFile();

            // 4. Bóc băng phụ đề WebVTT bằng Whisper.cpp cục bộ
            String vttContent = createVttFileAndGetContent(hlsFolderName, audioFile);

            // 4.5. Phân đoạn phụ đề WebVTT thành các chunk thông minh kèm mốc thời gian (Timestamps Alignment)
            parseAndEmitVttChunks(vttContent, event);

            // 5. Đồng bộ toàn bộ thư mục HLS lên MinIO
            String remoteHlsPath = "hls/" + hlsFolderName;
            storageStrategy.uploadDirectory(localHlsPath.toString(), remoteHlsPath);

            // 6. Tạo liên kết công khai
            String url = String.format("%s/%s/%s/playlist.m3u8", minioEndpoint, bucketName, remoteHlsPath);

            String transcriptUrl = String.format("%s/%s/%s/subtitles.vtt", minioEndpoint, bucketName, remoteHlsPath);

            // 7. Phát sự kiện LessonMediaReadyEvent báo cáo hoàn tất
            long fileSize = tempFile.length();
            Double duration = videoProcessingService.getVideoDuration(tempFile);
            
            LessonMediaReadyEvent readyEvent = LessonMediaReadyEvent.builder()
                    .lessonId(event.getLessonId())
                    .mediaId(event.getMediaId())
                    .hlsFolderName(hlsFolderName)
                    .transcriptUrl(transcriptUrl)
                    .url(url)
                    .fileSize(fileSize)
                    .duration(duration)
                    .build();

            kafkaTemplate.send(KafkaTopics.LESSON_MEDIA_READY, event.getLessonId(), readyEvent);
            log.info("✅ Đã phát LessonMediaReadyEvent thành công cho lesson {}", event.getLessonId());

            // 8. Dọn dẹp video thô trên MinIO để tiết kiệm không gian lưu trữ đám mây
            try {
                storageStrategy.deleteFile(event.getFileUrl());
                log.info("🗑️ Đã xóa video gốc thô trên MinIO: {}", event.getFileUrl());
            } catch (Exception ex) {
                log.warn("⚠️ Không thể xóa video thô {} trên MinIO: {}", event.getFileUrl(), ex.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ Lỗi nghiêm trọng khi transcode video {}: {}", event.getMediaId(), e.getMessage(), e);
        } finally {
            // Dọn dẹp tài nguyên tệp tạm cục bộ
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            if (Files.exists(localHlsPath)) {
                FileSystemUtils.deleteRecursively(localHlsPath.toFile());
                log.info("🧹 Đã dọn dẹp thư mục HLS tạm cục bộ: {}", localHlsPath);
            }
        }
    }

    private String createVttFileAndGetContent(String hlsFolderName, File audioFile) throws IOException {
        Path vttPath = Paths.get(baseStoragePath, "hls", hlsFolderName, "subtitles.vtt");
        Files.createDirectories(vttPath.getParent());
        File vttFile = vttPath.toFile();

        // Gọi Whisper.cpp bóc băng offline
        String vttContent = whisperLocalSpeechToTextService.transcribeAudioToVtt(audioFile);

        // Lưu tệp WebVTT cục bộ
        Files.write(vttPath, vttContent.getBytes());
        log.info("💾 Đã lưu tệp WebVTT phụ đề cục bộ: {}", vttFile.getAbsolutePath());

        return vttContent;
    }

    private void parseAndEmitVttChunks(String vttContent, MediaProcessingRequestEvent event) {
        log.info("🎞️ Bắt đầu phân mảnh thông minh phụ đề WebVTT...");
        if (vttContent == null || vttContent.trim().isEmpty()) {
            return;
        }

        String[] blocks = vttContent.split("\\r?\\n\\r?\\n");
        StringBuilder currentChunk = new StringBuilder();
        String currentStartTimestamp = null;
        int wordCount = 0;

        for (String block : blocks) {
            block = block.trim();
            if (block.isEmpty() || block.equals("WEBVTT")) {
                continue;
            }

            String[] lines = block.split("\\r?\\n");
            if (lines.length < 2) {
                continue;
            }

            String timeLine = lines[0];
            if (!timeLine.contains("-->")) {
                continue;
            }

            StringBuilder textLineBuilder = new StringBuilder();
            for (int i = 1; i < lines.length; i++) {
                textLineBuilder.append(lines[i]).append(" ");
            }
            String cueText = textLineBuilder.toString().trim();
            if (cueText.isEmpty()) {
                continue;
            }

            String startRaw = timeLine.split("-->")[0].trim();
            String formattedTime = formatTimestamp(startRaw);

            if (currentStartTimestamp == null) {
                currentStartTimestamp = formattedTime;
            }

            currentChunk.append(cueText).append(" ");
            wordCount += cueText.split("\\s+").length;

            // Gom khoảng 120 từ (~1 đến 1.5 phút thuyết giảng) để nhúng vector tốt nhất
            if (wordCount >= 120) {
                emitRawTextEvent(event, currentChunk.toString().trim(), currentStartTimestamp);
                currentChunk = new StringBuilder();
                currentStartTimestamp = null;
                wordCount = 0;
            }
        }

        // Emit nốt chunk cuối cùng còn sót lại
        if (!currentChunk.isEmpty() && currentStartTimestamp != null) {
            emitRawTextEvent(event, currentChunk.toString().trim(), currentStartTimestamp);
        }
    }

    private String formatTimestamp(String rawTime) {
        // "00:02:15.500" -> "02:15"
        String[] parts = rawTime.split(":");
        if (parts.length >= 3) {
            String minutes = parts[1];
            String seconds = parts[2];
            if (seconds.contains(".")) {
                seconds = seconds.substring(0, seconds.indexOf("."));
            }
            return minutes + ":" + seconds;
        }
        return "00:00";
    }

    private void emitRawTextEvent(MediaProcessingRequestEvent event, String text, String timestamp) {
        RawTextIngestedEvent ingestionEvent = RawTextIngestedEvent.builder()
                .courseId(event.getCourseId())
                .lessonId(event.getLessonId())
                .mediaId(event.getMediaId())
                .content(text)
                .contentType(ContentType.VIDEO)
                .sourceCitation(timestamp) // Trích dẫn trực tiếp giây:phút
                .build();
        
        kafkaTemplate.send(KafkaTopics.RAW_TEXT_INGESTED, event.getLessonId(), ingestionEvent);
        log.info("✅ Đã phát RawTextIngestedEvent (Video) tại mốc thời gian: [{}]", timestamp);
    }
}
