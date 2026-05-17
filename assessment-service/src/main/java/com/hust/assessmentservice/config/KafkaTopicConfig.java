package com.hust.assessmentservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.hust.commonlibrary.constant.KafkaTopics;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic assessmentSubmittedTopic() {
        return TopicBuilder.name(KafkaTopics.ASSESSMENT_SUBMITTED)
                .partitions(3) 
                .replicas(1)   
                .build();
    }
}
