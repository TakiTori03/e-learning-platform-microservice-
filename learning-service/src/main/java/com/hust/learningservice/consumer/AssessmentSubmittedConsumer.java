package com.hust.learningservice.consumer;

import com.hust.commonlibrary.event.AssessmentSubmittedEvent;
import com.hust.learningservice.service.LearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.hust.commonlibrary.constant.KafkaTopics;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssessmentSubmittedConsumer {

    private final LearningService learningService;

    /**
     * Listens to assignment/quiz submissions from assessment-service.
     * Automagically unlocks lesson progress!
     */
    @KafkaListener(topics = KafkaTopics.ASSESSMENT_SUBMITTED, groupId = "learning-group")
    public void consumeAssessmentSubmitted(AssessmentSubmittedEvent event) {
        log.info("📬 Received AssessmentSubmittedEvent from Kafka! User: {}, Target: {} ({}), Type: {}", 
                event.getUserId(), event.getTargetId(), event.getTargetType(), event.getAssessmentType());

        try {
            // As per requirements: mark as done IMMEDIATELY upon submission if it's a LESSON
            if ("LESSON".equalsIgnoreCase(event.getTargetType())) {
                String userId = event.getUserId();
                String lessonId = event.getTargetId();

                log.info("🎓 Syncing submitted assessment outcome to academic progress. User [{}], Lesson [{}]", userId, lessonId);
                learningService.completeQuizLesson(userId, lessonId);
                
                log.info("✅ Successfully processed assessment completion event for Submission: {}", event.getSubmissionId());
            } else {
                log.info("⏩ Skipping progress update (Target was {})", event.getTargetType());
            }
        } catch (Exception e) {
            log.error("❌ ERROR syncing assessment submission progress to DB for Submission {}: {}", 
                    event.getSubmissionId(), e.getMessage(), e);
        }
    }
}
