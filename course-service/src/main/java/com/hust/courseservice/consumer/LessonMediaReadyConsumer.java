package com.hust.courseservice.consumer;

import com.hust.commonlibrary.constant.KafkaTopics;
import com.hust.commonlibrary.event.LessonMediaReadyEvent;
import com.hust.courseservice.entity.Lesson;
import com.hust.courseservice.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LessonMediaReadyConsumer {

    private final LessonRepository lessonRepository;

    @KafkaListener(topics = KafkaTopics.LESSON_MEDIA_READY, groupId = "course-service-lesson-group")
    public void consume(LessonMediaReadyEvent event) {
        log.info("🎯 [course-service] Nhận sự kiện LESSON_MEDIA_READY cho lesson: {}", event.getLessonId());

        try {
            Optional<Lesson> lessonOpt = lessonRepository.findById(event.getLessonId());
            if (lessonOpt.isPresent()) {
                Lesson lesson = lessonOpt.get();
                
                boolean updated = false;

                if (event.getUrl() != null && !event.getUrl().isEmpty()) {
                    lesson.setContent(event.getUrl());
                    updated = true;
                }

                if (event.getDuration() != null) {
                    lesson.setVideoLength(event.getDuration());
                    updated = true;
                }

                if (event.getTranscriptUrl() != null && !event.getTranscriptUrl().isEmpty()) {
                    lesson.setTranscriptUrl(event.getTranscriptUrl());
                    updated = true;
                }

                if (updated) {
                    lessonRepository.save(lesson);
                    log.info("✅ Đã cập nhật content và videoLength cho lesson: {}", event.getLessonId());
                }
            } else {
                log.warn("⚠️ Không tìm thấy lesson với id: {}", event.getLessonId());
            }
        } catch (Exception e) {
            log.error("❌ Lỗi khi cập nhật lesson từ sự kiện LESSON_MEDIA_READY: {}", e.getMessage(), e);
        }
    }
}
