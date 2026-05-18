package com.hust.workerservice.consumer;

import com.hust.commonlibrary.constant.KafkaTopics;
import com.hust.commonlibrary.event.LessonMediaReadyEvent;
import com.hust.commonlibrary.event.MediaProcessingRequestEvent;
import com.hust.commonlibrary.annotation.TrackPerformance;
import com.hust.workerservice.strategy.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdfProcessingConsumer {

    private final StorageStrategy storageStrategy;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaTopics.MEDIA_PROCESSING, groupId = "pdf-worker-group")
    @TrackPerformance(threshold = 10000, description = "PDF Text Extraction Pipeline")
    public void consume(MediaProcessingRequestEvent event) {
        if (!"PDF".equalsIgnoreCase(event.getMediaType())) {
            return; // Ignore non-PDF assets in this worker
        }
        
        log.info("📄 Received PdfProcessingRequestEvent from Kafka: {}", event);

        File tempFile = null;
        try {
            // 1. Create local temp file & Download PDF
            tempFile = File.createTempFile("worker-pdf-raw-", ".pdf");
            storageStrategy.downloadFileToLocal(event.getFileUrl(), tempFile);

            // 2. Extract text page by page using PDFBox
            try (PDDocument document = Loader.loadPDF(tempFile)) {
                PDFTextStripper stripper = new PDFTextStripper();
                int pageCount = document.getNumberOfPages();
                log.info("PDF has {} pages. Starting text extraction...", pageCount);

                for (int page = 1; page <= pageCount; page++) {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    String text = stripper.getText(document);
                    
                    log.info("Page {} Extracted Length: {} chars", page, text.trim().length());
                    // In a future phase, we will emit an AiIngestionEvent for RAG chunking here
                }
            }

            // 3. Trigger LessonMediaReadyEvent for PDF to set it as READY
            long fileSize = tempFile.length();
            LessonMediaReadyEvent readyEvent = LessonMediaReadyEvent.builder()
                    .lessonId(event.getLessonId())
                    .mediaId(event.getMediaId())
                    .fileSize(fileSize)
                    .build();
            
            kafkaTemplate.send(KafkaTopics.LESSON_MEDIA_READY, event.getLessonId(), readyEvent);
            log.info("✅ Successfully processed PDF (Size: {} bytes) and sent LessonMediaReadyEvent for lesson {}", fileSize, event.getLessonId());

        } catch (Exception e) {
            log.error("❌ Error processing PDF {}: {}", event.getMediaId(), e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
