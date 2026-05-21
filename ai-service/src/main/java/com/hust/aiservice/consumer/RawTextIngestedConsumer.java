package com.hust.aiservice.consumer;

import com.hust.commonlibrary.entity.ContentType;
import com.hust.aiservice.repository.DocumentChunkRepository;
import com.hust.aiservice.service.GeminiService;
import com.hust.commonlibrary.event.RawTextIngestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawTextIngestedConsumer {

    private final GeminiService geminiService;
    private final DocumentChunkRepository documentChunkRepository;

    @KafkaListener(
            topics = "raw-text-ingested-topic",
            groupId = "ai-group"
    )
    public void consume(RawTextIngestedEvent event) {
        log.info("📩 Nhận được RawTextIngestedEvent: Course [{}], Lesson [{}], Type [{}], Citation [{}]", 
                 event.getCourseId(), event.getLessonId(), event.getContentType(), event.getSourceCitation());
        
        try {
            // 1. Kiểm tra tính hợp lệ của dữ liệu
            if (event.getContent() == null || event.getContent().trim().isEmpty()) {
                log.warn("⚠️ Nội dung văn bản rỗng, bỏ qua xử lý sinh embedding.");
                return;
            }

            ContentType contentType = event.getContentType();
            if (contentType == null) {
                log.error("❌ ContentType bị null, bỏ qua xử lý.");
                return;
            }

            // 2. Gọi Gemini API để sinh vector embedding 768 chiều
            List<Double> embedding = geminiService.getEmbedding(event.getContent());
            String embeddingString = geminiService.convertToVectorString(embedding);

            // 3. Tạo ID ngẫu nhiên cho chunk và lưu vào DB pgvector
            String chunkId = UUID.randomUUID().toString();
            documentChunkRepository.insertChunk(
                    chunkId,
                    event.getCourseId(),
                    event.getLessonId(),
                    event.getContent(),
                    contentType.name(),
                    event.getSourceCitation(),
                    embeddingString
            );

            log.info("✅ Đã lưu chunk thành công vào pgvector DB: Chunk ID [{}], Kích thước vector [{}]", 
                     chunkId, embedding.size());

        } catch (Exception e) {
            log.error("❌ Lỗi xảy ra khi xử lý RawTextIngestedEvent: {}", e.getMessage(), e);
            // Không rethrow ngoại lệ để tránh lặp vô hạn (Infinite Loop) của Kafka consumer retry
        }
    }
}
