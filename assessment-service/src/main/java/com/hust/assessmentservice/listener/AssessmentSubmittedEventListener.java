package com.hust.assessmentservice.listener;

import com.hust.commonlibrary.constant.KafkaTopics;
import com.hust.commonlibrary.event.AssessmentSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssessmentSubmittedEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAssessmentSubmittedEvent(AssessmentSubmittedEvent event) {
        log.info("💡 Transaction COMMITTED successfully! Thread [{}] is picking up local event for assessment submission: {}", 
                Thread.currentThread().getName(), event.getSubmissionId());
        
        try {
            log.info("🚀 Broadcasting event to Kafka topic '{}' for student: {}",
                    KafkaTopics.ASSESSMENT_SUBMITTED, event.getUserId());
            
            kafkaTemplate.send(KafkaTopics.ASSESSMENT_SUBMITTED, event.getUserId(), event);
            
        } catch (Exception e) {
            log.error("❌ CRITICAL ERROR publishing to Kafka after commit! Database is saved but event delivery FAILED. Submission: {}. Error: {}", 
                    event.getSubmissionId(), e.getMessage(), e);
        }
    }
}
