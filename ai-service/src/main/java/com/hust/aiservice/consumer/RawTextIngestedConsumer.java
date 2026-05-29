package com.hust.aiservice.consumer;

import com.hust.commonlibrary.entity.ContentType;
import com.hust.aiservice.repository.DocumentChunkRepository;
import com.hust.aiservice.service.GeminiService;
import com.hust.commonlibrary.event.RawTextIngestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawTextIngestedConsumer {

    private final GeminiService geminiService;
    private final DocumentChunkRepository documentChunkRepository;

    @RetryableTopic(
            attempts = "${spring.kafka.consumer.attempts:4}",
            backoff = @Backoff(delayExpression = "${spring.kafka.consumer.backoff-delay:5000}", multiplierExpression = "${spring.kafka.consumer.backoff-multiplier:2.0}"),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {
                java.lang.RuntimeException.class,
                java.io.IOException.class
            }
    )
    @KafkaListener(
            topics = "${spring.kafka.topic.raw-text-ingested:raw-text-ingested-topic}",
            groupId = "${spring.kafka.consumer.group-id:ai-group}"
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
                    event.getMediaId(),
                    event.getChunkIndex(),
                    event.getContent(),
                    contentType.name(),
                    event.getSourceCitation(),
                    embeddingString
            );

            log.info("✅ Đã lưu chunk thành công vào pgvector DB: Chunk ID [{}], Kích thước vector [{}]", 
                     chunkId, embedding.size());

        } catch (Exception e) {
            log.error("❌ Lỗi xảy ra khi xử lý RawTextIngestedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Raw text ingestion process failed, triggering retry topic", e);
        }
    }

    @DltHandler
    public void handleDlt(RawTextIngestedEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("💀 Tin nhắn RawTextIngestedEvent đã thất bại vượt quá số lần thử và chuyển sang DLT (Topic: {}): {}", topic, event);
    }
}

