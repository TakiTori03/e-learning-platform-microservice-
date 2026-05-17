package com.hust.orderservice.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.commonlibrary.constant.KafkaTopics;
import com.hust.commonlibrary.event.OrderPaidEvent;
import com.hust.orderservice.constant.OutboxStatus;
import com.hust.orderservice.entity.OutboxEvent;
import com.hust.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void pollOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Outbox Poller: Found {} pending events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                if ("ORDER_PAID".equals(event.getEventType())) {
                    OrderPaidEvent orderPaidEvent = objectMapper.readValue(event.getPayload(), OrderPaidEvent.class);
                    
                    log.info("Outbox Poller: Publishing OrderPaidEvent to Kafka for Order ID: {}", event.getAggregateId());
                    
                    // Synchronously get to guarantee delivery
                    kafkaTemplate.send(KafkaTopics.ORDER_PAID, event.getAggregateId(), orderPaidEvent).get();

                    event.setStatus(OutboxStatus.PROCESSED);
                    event.setProcessedAt(Instant.now());
                    outboxEventRepository.save(event);
                    log.info("Outbox Poller: Successfully published and updated status to PROCESSED for Event ID: {}", event.getId());
                }
            } catch (Exception e) {
                log.error("Outbox Poller: Failed to process OutboxEvent ID: {}. Error: {}", event.getId(), e.getMessage());
                
                int retries = event.getRetryCount() + 1;
                event.setRetryCount(retries);
                if (retries >= 5) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox Poller: Event ID {} exceeded max retries. Marked as FAILED.", event.getId());
                }
                outboxEventRepository.save(event);
            }
        }
    }
}
