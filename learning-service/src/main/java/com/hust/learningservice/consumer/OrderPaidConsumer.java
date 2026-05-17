package com.hust.learningservice.consumer;

import com.hust.commonlibrary.event.OrderPaidEvent;
import com.hust.commonlibrary.event.EnrollmentSuccessEvent;
import com.hust.learningservice.service.LearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.hust.commonlibrary.constant.KafkaTopics;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidConsumer {

    private final LearningService learningService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaTopics.ORDER_PAID, groupId = "learning-group")
    public void consumeOrderPaidEvent(OrderPaidEvent event) {
        log.info("Received OrderPaidEvent from Kafka: {}", event);
        
        try {
            learningService.enrollStudentBulk(event.getUserId(), event.getCourseIds(), event.getOrderId());
            
            log.info("Successfully processed enrollment for Order: {}", event.getOrderId());
            
            // Bắn ngược lại event "EnrollmentSuccess" cho order-service để hoàn tất Saga
            EnrollmentSuccessEvent successEvent = EnrollmentSuccessEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .status("SUCCESS")
                    .build();
            
            kafkaTemplate.send(KafkaTopics.ENROLLMENT_SUCCESS, event.getOrderId(), successEvent);
            log.info("Sent EnrollmentSuccessEvent back to Kafka for Order: {}", event.getOrderId());
            
        } catch (Exception e) {
            log.error("Failed to process OrderPaidEvent for Order {}: {}", event.getOrderId(), e.getMessage());
            
            // Bắn tin nhắn báo THẤT BẠI về cho order-service để thực hiện Giao dịch bù (Undo)
            EnrollmentSuccessEvent failEvent = EnrollmentSuccessEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .status("FAILED")
                    .build();
            
            try {
                kafkaTemplate.send(KafkaTopics.ENROLLMENT_SUCCESS, event.getOrderId(), failEvent);
                log.info("Sent Enrollment FAILURE callback to order-service for Order: {}", event.getOrderId());
            } catch (Exception kafkaEx) {
                log.error("Critial: Could not even send Failure event to Kafka! {}", kafkaEx.getMessage());
            }

            // Chúng ta KHÔNG throw e nữa để Kafka không retry vô hạn (vì ta đã báo FAILED)
        }
    }
}
