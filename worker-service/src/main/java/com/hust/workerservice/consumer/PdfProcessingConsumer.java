package com.hust.workerservice.consumer;

import com.hust.commonlibrary.constant.KafkaTopics;
import com.hust.commonlibrary.event.LessonMediaReadyEvent;
import com.hust.commonlibrary.event.MediaProcessingRequestEvent;
import com.hust.commonlibrary.entity.ContentType;
import com.hust.commonlibrary.event.RawTextIngestedEvent;
import com.hust.commonlibrary.annotation.TrackPerformance;
import com.hust.workerservice.strategy.StorageStrategy;
import com.hust.workerservice.dto.PythonParserResponse;
import com.hust.workerservice.dto.ParsedPageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdfProcessingConsumer {

    private final StorageStrategy storageStrategy;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${app.parser.service-url:http://127.0.0.1:8090/api/v1/parser/extract}")
    private String parserServiceUrl;

    @KafkaListener(topics = KafkaTopics.MEDIA_PROCESSING, groupId = "pdf-worker-group", concurrency = "1")
    @TrackPerformance(threshold = 30000, description = "GPU-Accelerated PDF OCR & Semantic Chunking Ingestion Pipeline")
    public void consume(MediaProcessingRequestEvent event) {
        if (!"PDF".equalsIgnoreCase(event.getMediaType())) {
            return;
        }
        
        log.info("📄 Nhận sự kiện xử lý PDF từ Kafka: {}", event);

        File tempFile = null;
        try {
            // 1. Tạo tệp tạm cục bộ và tải file PDF từ MinIO về
            tempFile = File.createTempFile("worker-pdf-raw-", ".pdf");
            storageStrategy.downloadFileToLocal(event.getFileUrl(), tempFile);

            // 2. Gửi tệp PDF sang Python Service cục bộ để OCR bằng GPU CUDA & Semantic Chunking
            log.info("🐍 Đang đẩy tệp PDF sang Python Parser Service để bóc tách thông minh...");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(tempFile));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<PythonParserResponse> response = restTemplate.postForEntity(
                    parserServiceUrl, requestEntity, PythonParserResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                PythonParserResponse parserResult = response.getBody();
                log.info("✅ Bóc tách PDF thành công! Tổng số trang: {}", parserResult.getTotalPages());

                // 3. Duyệt qua từng trang và gửi các Semantic Chunks đã cắt lên Kafka
                for (ParsedPageDto page : parserResult.getPages()) {
                    if (page.getChunks() != null) {
                        for (int chunkIdx = 0; chunkIdx < page.getChunks().size(); chunkIdx++) {
                            String chunkContent = page.getChunks().get(chunkIdx);
                            if (chunkContent != null && !chunkContent.trim().isEmpty()) {
                                RawTextIngestedEvent ingestionEvent = RawTextIngestedEvent.builder()
                                        .courseId(event.getCourseId())
                                        .lessonId(event.getLessonId())
                                        .mediaId(event.getMediaId())
                                        .content(chunkContent.trim())
                                        .contentType(ContentType.PDF)
                                        .sourceCitation("Trang " + page.getPage())
                                        .build();
                                
                                kafkaTemplate.send(KafkaTopics.RAW_TEXT_INGESTED, event.getLessonId(), ingestionEvent);
                            }
                        }
                    }
                }
            } else {
                throw new RuntimeException("Python Parser Service trả về mã lỗi: " + response.getStatusCode());
            }

            // 4. Phát sự kiện LessonMediaReadyEvent báo cáo hoàn thành
            long fileSize = tempFile.length();
            LessonMediaReadyEvent readyEvent = LessonMediaReadyEvent.builder()
                    .lessonId(event.getLessonId())
                    .mediaId(event.getMediaId())
                    .fileSize(fileSize)
                    .build();
            
            kafkaTemplate.send(KafkaTopics.LESSON_MEDIA_READY, event.getLessonId(), readyEvent);
            log.info("🎉 Hoàn tất toàn bộ chu trình xử lý PDF & Gửi Ingestion Event cho lesson {}", event.getLessonId());

        } catch (Exception e) {
            log.error("❌ Lỗi nghiêm trọng khi bóc tách PDF {}: {}", event.getMediaId(), e.getMessage(), e);
        } finally {
            // Xóa sạch tệp tạm cục bộ để bảo vệ ổ cứng
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
                log.info("🧹 Đã dọn dẹp file PDF tạm cục bộ.");
            }
        }
    }
}
